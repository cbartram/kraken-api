package handlers

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/service"
	"net/http"
)

// The amount of time the signed URL is valid for to read plugin data from S3
const SIGNED_URL_DURATION_SECONDS = 90

type ClientBootstrapHandler struct{}

// HandleRequest Handles the /api/v1/plugin/presigned-url route which the service calls to generate pre signed urls
// to download plugin JAR files from S3. Note: this method does NOT validate license keys for plugins only that
// plugins are not expired. All non-expired plugins will have presigned urls and be loadable by the service. License
// key validation happens in the /api/v1/plugin/validate-license endpoint before a loaded plugin is started.
func (b *ClientBootstrapHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	s3, err := service.MakeS3Service("kraken-plugins")
	if err != nil {
		log.Errorf("error: failed to create s3 service: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "error: failed to create S3 service.",
		})
		return
	}

	exists, name, err := s3.GetLatestVersion("client/")
	if err != nil || !exists {
		log.Errorf("error: kraken client with prefix: kraken-client- does not exist or error: %s", err)
	}

	url, err := s3.GetObject(ctx, fmt.Sprintf("%s.jar", name), SIGNED_URL_DURATION_SECONDS)
	if err != nil {
		log.Errorf("error creating presigned url for kraken-client: %s", name)
	}

	c.JSON(http.StatusOK, url)
}
