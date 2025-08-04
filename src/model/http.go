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
	DiscordID       string `json:"discordId"`
	DiscordUsername string `json:"discordUsername"`
	AvatarID        string `json:"avatarId"`
	DiscordEmail    string `json:"discordEmail"`
	HardwareID      string `json:"hardwareId"`
}

type CreatePresignedUrlRequestBatch struct {
	Plugins    map[string]string `json:"plugins"`
	HardwareID string            `json:"hardwareId"`
}

type PurchasePluginRequest struct {
	PluginName       string `json:"pluginName"`
	PurchaseDuration string `json:"purchaseDuration"`
	IsPack           bool   `json:"-"`
}
