package plugin

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	v4 "github.com/aws/aws-sdk-go-v2/aws/signer/v4"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"strconv"
	"sync"
)

// SignedUrlDurationSeconds The amount of time the signed URL is valid for to read plugin data from S3
const SignedUrlDurationSeconds = 300

type PresignedUrlHandler struct{}

// HandleRequest Handles the /api/v1/plugin/presigned-url route which the service calls to generate pre-signed urls
// to download plugin JAR files from S3. Note: this method does NOT validate license keys for plugins only that
// plugins are not expired. All non-expired plugins will have presigned urls and be loadable by the service. License
// key validation happens in the /api/v1/plugin/validate-license endpoint before a loaded plugin is started.
func (p *PresignedUrlHandler) HandleRequest(c *gin.Context, ctx context.Context, w *service.Wrapper) {
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)
	devPlugins, err := strconv.ParseBool(c.Query("dev"))
	if err != nil {
		log.Errorf("Unable to parse boolean for dev plugins from val: %s", c.Query("dev"))
		devPlugins = false
	}

	log.Infof("loading dev plugins: %v", devPlugins)
	if len(user.Plugins) == 0 {
		log.Infof("user: %s has no purchased plugins, skipping presigned url generation", user.DiscordUsername)
		c.JSON(http.StatusOK, gin.H{
			"urls": []v4.PresignedHTTPRequest{},
		})
		return
	}

	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.CreatePresignedUrlRequestBatch
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Sprintf("invalid request body: %v", err)})
		return
	}

	if !util.IsValidHardwareID(reqBody.HardwareID, user.HardwareIDs) {
		log.Infof("user: %s has provided an invalid hardware id: %s", user.DiscordUsername, reqBody.HardwareID)
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid hardware id"})
		return
	}

	var preSignedUrls []v4.PresignedHTTPRequest

	results := make(chan PresignedURLResult, len(user.Plugins))
	var wg sync.WaitGroup

	for _, plugin := range user.Plugins {
		wg.Add(1)
		go GeneratePreSignedURL(
			ctx,
			w.S3Service,
			reqBody.Plugins,
			plugin,
			devPlugins,
			&wg,
			results,
		)
	}

	go func() {
		wg.Wait()
		close(results)
	}()

	// Note: This will return partial results if some S3 calls fails for any reason.
	for result := range results {
		if result.Error != nil {
			log.Errorf("error generating presigned url: %s", err)
			continue
		}
		if result.URL != nil {
			preSignedUrls = append(preSignedUrls, *result.URL)
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"urls": preSignedUrls,
	})
}

type PresignedURLResult struct {
	URL   *v4.PresignedHTTPRequest
	Error error
}

func GeneratePreSignedURL(
	ctx context.Context,
	s3 *service.S3Service,
	licenseKeys map[string]string,
	plugin model.Plugin,
	devPlugins bool,
	wg *sync.WaitGroup,
	results chan<- PresignedURLResult,
) {
	defer wg.Done()
	if util.IsPluginExpired(plugin.ExpirationTimestamp) {
		log.Infof("plugin: %s is expired", plugin.Name)
		results <- PresignedURLResult{nil, errors.New(fmt.Sprintf("plugin: %s is expired", plugin.Name))}
		return
	}

	license, exists := licenseKeys[plugin.Name]

	if !exists {
		log.Infof("plugin: %s is not present in given license key map", plugin.Name)
		results <- PresignedURLResult{nil, errors.New(fmt.Sprintf("plugin: %s is not present in given license key map", plugin.Name))}
		return
	}

	if plugin.LicenseKey != license {
		log.Infof("plugin: %s license key: %s does not match provided (request) license key: %s", plugin.Name, plugin.LicenseKey, license)
		results <- PresignedURLResult{nil, errors.New(fmt.Sprintf("plugin: %s license key: %s does not match provided (request) license key: %s", plugin.Name, plugin.LicenseKey, license))}
		return
	}

	var prefix string
	if devPlugins {
		prefix = fmt.Sprintf("dev/%s", plugin.Name)
	} else {
		prefix = fmt.Sprintf("plugins/%s", plugin.Name)
	}

	exists, name, err := s3.GetLatestVersion(prefix)
	if err != nil || !exists {
		log.Errorf("error: plugin with prefix: %s does not exist or error: %s", plugin.Name, err)
		results <- PresignedURLResult{nil, err}
		return
	}

	url, err := s3.CreatePresignedUrl(ctx, fmt.Sprintf("%s.jar", name), SignedUrlDurationSeconds)
	if err != nil {
		log.Errorf("error creating presigned url for plugin: %s.jar with prefix: %s", name, prefix)
		results <- PresignedURLResult{nil, err}
		return
	}

	log.Infof("generated pre-signed url for: plugin: %s, %d seconds, url: %s",
		name, SignedUrlDurationSeconds, url.URL)
	results <- PresignedURLResult{url, nil}
}
