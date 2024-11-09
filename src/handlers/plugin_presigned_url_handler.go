package handlers

import (
	"context"
	"github.com/gin-gonic/gin"
)

type PluginPresignedUrlHandler struct{}

// HandleRequest Handles the /api/v1/discord-oauth route which the client calls to trade a code for an OAuth
// access token.
func (p *PluginPresignedUrlHandler) HandleRequest(c *gin.Context, ctx context.Context) {

}
