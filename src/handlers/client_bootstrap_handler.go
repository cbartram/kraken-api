package handlers

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/service"
	"net/http"
)

// SignedUrlDurationSeconds The amount of time the signed URL is valid for to read plugin data from S3
const SignedUrlDurationSeconds = 90

type ClientBootstrapHandler struct{}

// HandleRequest Handles the /api/v1/plugin/pre-signed-url route which the service calls to generate pre-signed urls
// to download the kraken-client JAR file from S3.
func (b *ClientBootstrapHandler) HandleRequest(c *gin.Context, ctx context.Context, w *service.Wrapper) {
	exists, name, err := w.S3Service.GetLatestVersion("client/")
	if err != nil || !exists {
		log.Errorf("error: kraken client with prefix: kraken-client- does not exist or error: %s", err)
	}

	url, err := w.S3Service.CreatePresignedUrl(ctx, fmt.Sprintf("%s.jar", name), SignedUrlDurationSeconds)
	if err != nil {
		log.Errorf("error creating presigned url for kraken-client: %s", name)
	}

	c.JSON(http.StatusOK, url)
}
