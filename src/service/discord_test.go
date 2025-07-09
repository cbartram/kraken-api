package service

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"go.uber.org/zap/zaptest"
)

// Mock model.DiscordTokenResponse for testing
type DiscordTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    int    `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
	Scope        string `json:"scope"`
}

// Test helper to create a test logger
func createTestLogger(t *testing.T) *zap.SugaredLogger {
	return zaptest.NewLogger(t).Sugar()
}

// Test helper to set environment variables and clean up
func setEnvVars(t *testing.T, clientID, clientSecret string) func() {
	originalClientID := os.Getenv("DISCORD_CLIENT_ID")
	originalClientSecret := os.Getenv("DISCORD_CLIENT_SECRET")

	os.Setenv("DISCORD_CLIENT_ID", clientID)
	os.Setenv("DISCORD_CLIENT_SECRET", clientSecret)

	return func() {
		os.Setenv("DISCORD_CLIENT_ID", originalClientID)
		os.Setenv("DISCORD_CLIENT_SECRET", originalClientSecret)
	}
}

func TestMakeDiscordService(t *testing.T) {
	tests := []struct {
		name         string
		clientID     string
		clientSecret string
		expectError  bool
		errorMsg     string
	}{
		{
			name:         "successful creation with valid env vars",
			clientID:     "test_client_id",
			clientSecret: "test_client_secret",
			expectError:  false,
		},
		{
			name:         "missing client ID",
			clientID:     "",
			clientSecret: "test_client_secret",
			expectError:  true,
			errorMsg:     "missing required environment variables",
		},
		{
			name:         "missing client secret",
			clientID:     "test_client_id",
			clientSecret: "",
			expectError:  true,
			errorMsg:     "missing required environment variables",
		},
		{
			name:         "missing both credentials",
			clientID:     "",
			clientSecret: "",
			expectError:  true,
			errorMsg:     "missing required environment variables",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cleanup := setEnvVars(t, tt.clientID, tt.clientSecret)
			defer cleanup()

			logger := createTestLogger(t)
			service, err := MakeDiscordService(logger)

			if tt.expectError {
				assert.Error(t, err)
				assert.Nil(t, service)
				assert.Contains(t, err.Error(), tt.errorMsg)
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, service)
				assert.Equal(t, tt.clientID, service.clientID)
				assert.Equal(t, tt.clientSecret, service.clientSecret)
				assert.NotNil(t, service.httpClient)
				assert.NotNil(t, service.log)
			}
		})
	}
}

func TestDiscordService_ExchangeCodeForToken(t *testing.T) {
	tests := []struct {
		name           string
		code           string
		redirectUri    string
		mockStatusCode int
		mockResponse   interface{}
		expectError    bool
		errorContains  string
	}{
		{
			name:           "successful token exchange",
			code:           "test_code",
			redirectUri:    "http://localhost:8080/callback",
			mockStatusCode: http.StatusOK,
			mockResponse: DiscordTokenResponse{
				AccessToken:  "test_access_token",
				TokenType:    "Bearer",
				ExpiresIn:    3600,
				RefreshToken: "test_refresh_token",
				Scope:        "identify email",
			},
			expectError: false,
		},
		{
			name:           "discord API returns 400 bad request",
			code:           "invalid_code",
			redirectUri:    "http://localhost:8080/callback",
			mockStatusCode: http.StatusBadRequest,
			mockResponse:   map[string]string{"error": "invalid_grant"},
			expectError:    true,
			errorContains:  "discord API returned status: 400",
		},
		{
			name:           "discord API returns 401 unauthorized",
			code:           "test_code",
			redirectUri:    "http://localhost:8080/callback",
			mockStatusCode: http.StatusUnauthorized,
			mockResponse:   map[string]string{"error": "invalid_client"},
			expectError:    true,
			errorContains:  "discord API returned status: 401",
		},
		{
			name:           "discord API returns 500 internal server error",
			code:           "test_code",
			redirectUri:    "http://localhost:8080/callback",
			mockStatusCode: http.StatusInternalServerError,
			mockResponse:   "Internal Server Error",
			expectError:    true,
			errorContains:  "discord API returned status: 500",
		},
		{
			name:           "invalid JSON response",
			code:           "test_code",
			redirectUri:    "http://localhost:8080/callback",
			mockStatusCode: http.StatusOK,
			mockResponse:   "invalid json response",
			expectError:    true,
			errorContains:  "failed to parse response",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Create mock server
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				// Verify the request method and endpoint
				assert.Equal(t, "POST", r.Method)
				assert.Equal(t, "/oauth2/token", r.URL.Path)

				// Verify headers
				assert.Equal(t, "application/x-www-form-urlencoded", r.Header.Get("Content-Type"))

				// Verify basic auth
				username, password, ok := r.BasicAuth()
				assert.True(t, ok)
				assert.Equal(t, "test_client_id", username)
				assert.Equal(t, "test_client_secret", password)

				// Verify form data
				err := r.ParseForm()
				assert.NoError(t, err)
				assert.Equal(t, "authorization_code", r.FormValue("grant_type"))
				assert.Equal(t, tt.code, r.FormValue("code"))
				assert.Equal(t, tt.redirectUri, r.FormValue("redirect_uri"))

				w.WriteHeader(tt.mockStatusCode)

				if tt.mockStatusCode == http.StatusOK {
					if tokenResp, ok := tt.mockResponse.(DiscordTokenResponse); ok {
						json.NewEncoder(w).Encode(tokenResp)
						return
					}
				}

				if respMap, ok := tt.mockResponse.(map[string]string); ok {
					json.NewEncoder(w).Encode(respMap)
				} else if respStr, ok := tt.mockResponse.(string); ok {
					w.Write([]byte(respStr))
				}
			}))
			defer server.Close()

			// Create service with mock server URL
			logger := createTestLogger(t)
			service := &DiscordService{
				log:          logger,
				clientID:     "test_client_id",
				clientSecret: "test_client_secret",
				httpClient:   &http.Client{},
			}

			// Replace the Discord API endpoint with our mock server
			originalEndpoint := discordTokenEndpoint
			discordTokenEndpoint = server.URL + "/oauth2/token"
			defer func() {
				discordTokenEndpoint = originalEndpoint
			}()

			// Execute the method
			result, err := service.ExchangeCodeForToken(tt.code, tt.redirectUri)

			if tt.expectError {
				assert.Error(t, err)
				assert.Nil(t, result)
				assert.Contains(t, err.Error(), tt.errorContains)
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, result)

				expectedResp := tt.mockResponse.(DiscordTokenResponse)
				assert.Equal(t, expectedResp.AccessToken, result.AccessToken)
				assert.Equal(t, expectedResp.TokenType, result.TokenType)
				assert.Equal(t, expectedResp.ExpiresIn, result.ExpiresIn)
				assert.Equal(t, expectedResp.RefreshToken, result.RefreshToken)
				assert.Equal(t, expectedResp.Scope, result.Scope)
			}
		})
	}
}

func TestDiscordService_ExchangeCodeForToken_EmptyResponse(t *testing.T) {
	// Test empty response body
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		// Send empty response
	}))
	defer server.Close()

	logger := createTestLogger(t)
	service := &DiscordService{
		log:          logger,
		clientID:     "test_client_id",
		clientSecret: "test_client_secret",
		httpClient:   &http.Client{},
	}

	originalEndpoint := discordTokenEndpoint
	discordTokenEndpoint = server.URL + "/oauth2/token"
	defer func() {
		discordTokenEndpoint = originalEndpoint
	}()

	result, err := service.ExchangeCodeForToken("test_code", "http://localhost:8080/callback")

	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "failed to parse response")
}

// Benchmark tests
func BenchmarkMakeDiscordService(b *testing.B) {
	cleanup := setEnvVars(&testing.T{}, "test_client_id", "test_client_secret")
	defer cleanup()

	logger := createTestLogger(&testing.T{})

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		service, err := MakeDiscordService(logger)
		if err != nil {
			b.Fatal(err)
		}
		_ = service
	}
}

func BenchmarkExchangeCodeForToken(b *testing.B) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		response := DiscordTokenResponse{
			AccessToken:  "test_access_token",
			TokenType:    "Bearer",
			ExpiresIn:    3600,
			RefreshToken: "test_refresh_token",
			Scope:        "identify email",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	logger := createTestLogger(&testing.T{})
	service := &DiscordService{
		log:          logger,
		clientID:     "test_client_id",
		clientSecret: "test_client_secret",
		httpClient:   &http.Client{},
	}

	originalEndpoint := discordTokenEndpoint
	discordTokenEndpoint = server.URL + "/oauth2/token"
	defer func() {
		discordTokenEndpoint = originalEndpoint
	}()

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		result, err := service.ExchangeCodeForToken("test_code", "http://localhost:8080/callback")
		if err != nil {
			b.Fatal(err)
		}
		_ = result
	}
}
