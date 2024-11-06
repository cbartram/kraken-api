package main

import (
	"context"
	"encoding/json"
	"fmt"
	"kraken-api/src/model"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	log "github.com/sirupsen/logrus"
)

// Configuration constants
const (
	discordAPIEndpoint = "https://discord.com/api/oauth2/token"
	timeout            = 10 * time.Second
)

// handleRequest is the main Lambda handler
func handleRequest(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	logLevel, err := log.ParseLevel(os.Getenv("LOG_LEVEL"))
	if err != nil {
		logLevel = log.InfoLevel
	}

	log.SetOutput(os.Stdout)
	log.SetLevel(logLevel)

	// Parse request body
	var reqBody model.Request
	if err := json.Unmarshal([]byte(request.Body), &reqBody); err != nil {
		return createResponse(http.StatusBadRequest, model.Response{
			Message: "Invalid request body",
			Status:  "error",
		})
	}

	if reqBody.Code == "" {
		return createResponse(http.StatusBadRequest, model.Response{
			Message: "Access code: 'code' is required",
			Status:  "error",
		})
	}

	token, err := exchangeCode(ctx, reqBody.Code)
	if err != nil {
		return createResponse(http.StatusInternalServerError, model.Response{
			Message: fmt.Sprintf("Failed to exchange code: %v", err),
			Status:  "error",
		})
	}

	return createResponse(http.StatusOK, model.Response{
		AccessToken:  token.AccessToken,
		TokenType:    token.TokenType,
		ExpiresIn:    token.ExpiresIn,
		RefreshToken: token.RefreshToken,
		Scope:        token.Scope,
	})

}

// exchangeCode exchanges the OAuth code for an access token
func exchangeCode(ctx context.Context, code string) (*model.DiscordTokenResponse, error) {
	clientID := os.Getenv("DISCORD_CLIENT_ID")
	clientSecret := os.Getenv("DISCORD_CLIENT_SECRET")
	redirectURI := os.Getenv("DISCORD_REDIRECT_URI")

	if clientID == "" || clientSecret == "" || redirectURI == "" {
		return nil, fmt.Errorf("missing required environment variables: CLIENT_ID, CLIENT_SECRET or REDIRECT_URI")
	}

	// Prepare the request body
	data := url.Values{}
	data.Set("grant_type", "authorization_code")
	data.Set("code", code)
	data.Set("redirect_uri", redirectURI)

	// Create context with timeout
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	log.Infof("Making POST request to: %s", discordAPIEndpoint)

	// Create request
	req, err := http.NewRequestWithContext(ctx, "POST", discordAPIEndpoint, strings.NewReader(data.Encode()))
	if err != nil {
		log.Errorf("error creating post to discord api %s: %s", discordAPIEndpoint, err)
		return nil, fmt.Errorf("failed to create POST request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.SetBasicAuth(clientID, clientSecret)

	// Send request
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Errorf("error making post to discord api %s: %s", discordAPIEndpoint, err)
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	// Check response status
	if resp.StatusCode != http.StatusOK {
		log.Errorf("unexpected status code from discord API: %d", resp.StatusCode)
		return nil, fmt.Errorf("discord API returned status: %d", resp.StatusCode)
	}

	log.Infof("http status code: %d", resp.StatusCode)

	// Parse response
	var tokenResp model.DiscordTokenResponse
	if err := json.NewDecoder(resp.Body).Decode(&tokenResp); err != nil {
		log.Errorf("failed to deserialize discord response body into model.DiscordTokenResponse object: %s", err)
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	log.Infof("Discord token response: %s", tokenResp.AccessToken)

	return &tokenResp, nil
}

// createResponse creates an API Gateway response
func createResponse(statusCode int, body model.Response) (events.APIGatewayProxyResponse, error) {
	jsonBody, err := json.Marshal(body)
	if err != nil {
		return events.APIGatewayProxyResponse{}, err
	}

	return events.APIGatewayProxyResponse{
		StatusCode: statusCode,
		Headers: map[string]string{
			"Content-Type": "application/json",
		},
		Body: string(jsonBody),
	}, nil
}

func main() {
	lambda.Start(handleRequest)
}
