package model

import (
	"context"
	"github.com/gin-gonic/gin"
)

type RequestHandler interface {
	HandleRequest(c *gin.Context, ctx context.Context)
}

// DiscordTokenResponse represents Discord's token response
type DiscordTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    int    `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
	Scope        string `json:"scope"`
}

type CognitoCreateUserRequest struct {
	DiscordID       string `json:"discord_id"`
	DiscordUsername string `json:"discord_username"`
	DiscordEmail    string `json:"discord_email"`
	HardwareID      string `json:"hardware_id"`
}

type CognitoUserStatusRequest struct {
	AccountEnabled bool   `json:"accountEnabled"`
	DiscordID      string `json:"discordId"`
}

type CognitoAuthRequest struct {
	RefreshToken string `json:"refreshToken"`
	DiscordID    string `json:"discordId"`
}

type LicenseKeyRequestBatch struct {
	Credentials CognitoCredentials `json:"credentials"`
	Plugins     map[string]string  `json:"plugins"`
	HardwareID  string             `json:"hardwareId"`
}

type LicenseKeyRequest struct {
	Credentials CognitoCredentials `json:"credentials"`
	LicenseKey  string             `json:"licenseKey"`
	PluginName  string             `json:"pluginName"`
	HardwareID  string             `json:"hardwareId"`
}

type PurchasePluginRequest struct {
	PluginName           string             `json:"pluginName"`
	PurchaseDurationDays int                `json:"purchaseDurationDays,omitempty"`
	Credentials          CognitoCredentials `json:"credentials"`
	// TODO CC information likely
}

type CognitoCredentials struct {
	RefreshToken    string `json:"refresh_token,omitempty"`
	TokenExpiration int32  `json:"token_expiration_seconds,omitempty"`
	AccessToken     string `json:"access_token,omitempty"`
	IdToken         string `json:"id_token,omitempty"`
}

type CognitoUser struct {
	CognitoID       string             `json:"cognitoId,omitempty"`
	DiscordUsername string             `json:"discordUsername,omitempty"`
	Email           string             `json:"email,omitempty"`
	DiscordID       string             `json:"discordId,omitempty"`
	AccountEnabled  bool               `json:"accountEnabled,omitempty"`
	Credentials     CognitoCredentials `json:"credentials,omitempty"`
}
