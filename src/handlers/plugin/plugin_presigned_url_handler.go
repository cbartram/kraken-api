package plugin

import (
	"context"
	"encoding/json"
	"fmt"
	v4 "github.com/aws/aws-sdk-go-v2/aws/signer/v4"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"kraken-api/src/util"
	"net/http"
)

// The amount of time the signed URL is valid for to read plugin data from S3
const SIGNED_URL_DURATION_SECONDS = 30

type PluginPresignedUrlHandler struct{}

// HandleRequest Handles the /api/v1/plugin/presigned-url route which the client calls to generate pre signed urls
// to download plugin JAR files from S3. Note: this method does NOT validate license keys for plugins only that
// plugins are not expired. All non-expired plugins will have presigned urls and be loadable by the client. License
// key validation happens in the /api/v1/plugin/validate-license endpoint before a loaded plugin is started.
func (p *PluginPresignedUrlHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	// Note: Only the access token is provided in the request body. All other values will be nil
	var reqBody model.CognitoCredentials
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	authManger := client.MakeCognitoService()
	log.Infof("fetching user attributes with access token")
	attr, err := authManger.GetUserAttributes(ctx, &reqBody.AccessToken)
	if err != nil {
		log.Errorf("error: failed to get user attributes with access token, error: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "failed to get user attributes with access token: " + err.Error(),
		})
		return
	}

	// Purchased Plugins attribute type will be "nil" (string) if no plugins have been purchased, else it will be
	// a CSV list of purchased plugin id's
	// TODO This should be parallelized in the future with go routines.
	pluginKeys := util.GetUserAttribute(attr, "custom:purchased_plugins")
	expirationTimestamps := util.GetUserAttribute(attr, "custom:expiration_timestamp")
	// Only return pre-signed url's for plugins where the plugin is not expired
	log.Infof("custom:purchased_plugins=%s, custom:expiration_timestamps=%s", pluginKeys, expirationTimestamps)
	if pluginKeys[0] == "nil" {
		c.JSON(http.StatusOK, gin.H{
			"urls": []v4.PresignedHTTPRequest{},
		})
		return
	}
	var preSignedUrls = make([]v4.PresignedHTTPRequest, 0)
	s3, err := client.MakeS3Service("kraken-plugins")
	if err != nil {
		log.Errorf("error: failed to create s3 client: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "error: failed to create S3 client.",
		})
		return
	}

	for i, plugin := range pluginKeys {
		expired, err := util.IsPluginExpired(expirationTimestamps[i])
		if err != nil {
			log.Errorf("error: failed to parse plugin expiration timestamp: %s to RFC3339 format. error: %s", expirationTimestamps[i], err.Error())
			continue
		}

		if expired {
			log.Infof("current time is after plugin expiration time, skipping pre-signed URL")
			continue
		}

		exists, name, err := s3.DoesObjectExist(fmt.Sprintf("plugins/%s", plugin))
		if err != nil || !exists {
			log.Errorf("error: plugin with prefix: %s does not exist or error: %s", plugin, err)
			continue
		}

		url, err := s3.GetObject(ctx, fmt.Sprintf("%s.jar", name), SIGNED_URL_DURATION_SECONDS)
		if err != nil {
			log.Errorf("error creating presigned url for plugin: %s", name)
			continue
		}
		log.Infof("generated pre-signed url good for: plugin=%s, %d seconds, url: %s", name, SIGNED_URL_DURATION_SECONDS, url.URL)
		preSignedUrls = append(preSignedUrls, *url)
	}

	c.JSON(http.StatusOK, gin.H{
		"urls": preSignedUrls,
	})
}
