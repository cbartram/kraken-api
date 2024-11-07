package model

import (
	"context"
	"github.com/gin-gonic/gin"
)

type RequestHandler interface {
	HandleRequest(c *gin.Context, ctx context.Context)
}

// DiscordOAuthRequest represents the expected request body
type DiscordOAuthRequest struct {
	Code string `json:"code"`
}

// DiscordTokenResponse represents Discord's token response
type DiscordTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    int    `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
	Scope        string `json:"scope"`
}

// ErrorResponse A response when an error occurs.
type ErrorResponse struct {
	Message string `json:"message,omitempty"`
	Status  string `json:"status,omitempty"`
}

type CognitoCreateUserRequest struct {
	DiscordID       string `json:"discord_id"`
	DiscordUsername string `json:"discord_username"`
	DiscordEmail    string `json:"discord_email"`
}

type CognitoAuthRequest struct {
	RefreshToken string `json:"refresh_token"`

	// This isn't actually used in the auth process as it can be expired but in case it is sent by the client
	// it is kept here.
	AccessToken string `json:"access_token,omitempty"`
}

type CognitoAuthResponse struct {
	RefreshToken string `json:"refresh_token,omitempty"`
	AccessToken  string `json:"access_token"`
}
