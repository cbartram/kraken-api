package service

import (
	"encoding/json"
	"fmt"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"net/http"
	"net/url"
	"os"
	"strings"
)

const (
	discordAPIEndpoint   = "https://discord.com/api"
	discordTokenEndpoint = discordAPIEndpoint + "/oauth2/token"
	discordUserEndpoint  = discordAPIEndpoint + "/users/@me"
)

// DiscordService handles OAuth2 authentication and API calls
type DiscordService struct {
	clientID     string
	clientSecret string
	httpClient   *http.Client
}

// UserResponse represents the Discord user information
type UserResponse struct {
	ID            string `json:"id"`
	Username      string `json:"username"`
	Discriminator string `json:"discriminator"`
	Avatar        string `json:"avatar"`
	Email         string `json:"email"`
	Verified      bool   `json:"verified"`
}

// MakeDiscordService creates a new Discord service
func MakeDiscordService() (*DiscordService, error) {
	clientID := os.Getenv("DISCORD_CLIENT_ID")
	clientSecret := os.Getenv("DISCORD_CLIENT_SECRET")

	if clientID == "" || clientSecret == "" {
		return nil, fmt.Errorf("missing required environment variables: CLIENT_ID, CLIENT_SECRET")
	}

	return &DiscordService{
		clientID:     clientID,
		clientSecret: clientSecret,
		httpClient:   &http.Client{},
	}, nil
}

// ExchangeCodeForToken exchanges an authorization code for an access token
func (c *DiscordService) ExchangeCodeForToken(code, redirectUri string) (*model.DiscordTokenResponse, error) {
	data := url.Values{}
	data.Set("grant_type", "authorization_code")
	data.Set("code", code)
	data.Set("redirect_uri", redirectUri)

	log.Infof("Making POST request to: %s with redirect uri: %s and code: %s", discordTokenEndpoint, redirectUri, code)

	// Create request
	req, err := http.NewRequest("POST", discordTokenEndpoint, strings.NewReader(data.Encode()))
	if err != nil {
		log.Errorf("error creating post to discord api %s: %s", discordTokenEndpoint, err)
		return nil, fmt.Errorf("failed to create POST request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.SetBasicAuth(c.clientID, c.clientSecret)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Errorf("error making post to discord api %s: %s", discordTokenEndpoint, err)
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		bodyBytes, _ := io.ReadAll(resp.Body)
		bodyString := string(bodyBytes)
		log.Errorf("unexpected status code from discord API: %d message: %s", resp.StatusCode, bodyString)
		return nil, fmt.Errorf("discord API returned status: %d message: %s", resp.StatusCode, bodyString)
	}

	log.Infof("discord access token http status code: %d", resp.StatusCode)

	var tokenResp model.DiscordTokenResponse
	if err := json.NewDecoder(resp.Body).Decode(&tokenResp); err != nil {
		log.Errorf("failed to deserialize discord response body into model.DiscordTokenResponse object: %s", err)
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	return &tokenResp, nil
}
