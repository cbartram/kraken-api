package plugin

import (
	"context"
	"errors"
	"fmt"
	v4 "github.com/aws/aws-sdk-go-v2/aws/signer/v4"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"net/url"
	"strconv"
	"sync"
	"time"
)

// SignedUrlDurationSeconds The amount of time the signed URL is valid for to read plugin data from S3
const SignedUrlDurationSeconds = 300

type PresignedUrlHandler struct{}

type PresignedUrlResponse struct {
	URL         string `json:"URL"`
	TrialPlugin bool   `json:"isTrialPlugin"`
}

type PresignedURLResult struct {
	URL    *url.URL
	Plugin *model.Plugin
	Error  error
}

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
	hwid := c.Query("hardwareId")
	if len(hwid) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "must provide hardwareId query parameter"})
		return
	}

	devPlugins, err := strconv.ParseBool(c.Query("dev"))
	if err != nil {
		log.Errorf("Unable to parse boolean for dev plugins from val: %s", c.Query("dev"))
		devPlugins = false
	}

	log.Infof("user provided hardware id: %s, loading dev plugins: %v", hwid, devPlugins)
	if len(user.Plugins) == 0 {
		log.Infof("user: %s has no purchased plugins, skipping presigned url generation", user.DiscordUsername)
		c.JSON(http.StatusOK, []v4.PresignedHTTPRequest{})
		return
	}

	if !util.IsValidHardwareID(hwid, user.HardwareIDs) {
		log.Infof("user: %s has provided an invalid hardware id: %s", user.DiscordUsername, hwid)
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid hardware id"})
		return
	}

	var preSignedUrls []PresignedUrlResponse

	results := make(chan PresignedURLResult, len(user.Plugins))
	var wg sync.WaitGroup
	var expiredPluginIDs []uint
	var activePlugins []*model.Plugin

	for _, plugin := range user.Plugins {
		if plugin.TrialPlugin && util.IsPluginExpired(plugin.ExpirationTimestamp) {
			log.Infof("user: %s has expired trial plugin: %s", user.DiscordUsername, plugin.Name)
			expiredPluginIDs = append(expiredPluginIDs, plugin.ID)
		} else {
			activePlugins = append(activePlugins, &plugin)
		}
	}

	if len(expiredPluginIDs) > 0 {
		if err := w.Database.Unscoped().Where("id IN ?", expiredPluginIDs).Delete(&model.Plugin{}).Error; err != nil {
			log.Errorf("failed to delete expired trial plugins: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("failed to delete expired trial plugins: %v", err)})
			return
		} else {
			log.Infof("deleted %d expired trial plugins for user: %s", len(expiredPluginIDs), user.DiscordUsername)
		}
	}

	for _, plugin := range activePlugins {
		wg.Add(1)
		go GeneratePreSignedURL(
			ctx,
			w.S3Service,
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
			log.Errorf("error generating presigned url for plugin: %s err: %s ", result.Plugin.Name, err)
			continue
		}
		if result.URL != nil {
			preSignedUrls = append(preSignedUrls, PresignedUrlResponse{
				URL:         result.URL.String(),
				TrialPlugin: result.Plugin.TrialPlugin,
			})
		}
	}

	c.JSON(http.StatusOK, preSignedUrls)
}

func GeneratePreSignedURL(
	ctx context.Context,
	s3 *service.MinIOService,
	plugin *model.Plugin,
	devPlugins bool,
	wg *sync.WaitGroup,
	results chan<- PresignedURLResult,
) {
	defer wg.Done()

	log.Infof("generating presigned url for plugin: %s, expires at: %s, license key: %s for user id: %d", plugin.Name, plugin.ExpirationTimestamp, plugin.LicenseKey, plugin.UserID)
	if util.IsPluginExpired(plugin.ExpirationTimestamp) {
		log.Infof("plugin: %s is expired with timestamp: %s when current time is: %s", plugin.Name, plugin.ExpirationTimestamp, time.Now().Format(time.RFC3339))
		results <- PresignedURLResult{nil, plugin, errors.New(fmt.Sprintf("plugin: %s is expired", plugin.Name))}
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
		results <- PresignedURLResult{nil, plugin, err}
		return
	}

	url, err := s3.CreatePresignedUrl(ctx, fmt.Sprintf("%s.jar", name), SignedUrlDurationSeconds)
	if err != nil {
		log.Errorf("error creating presigned url for plugin: %s.jar with prefix: %s", name, prefix)
		results <- PresignedURLResult{nil, plugin, err}
		return
	}

	log.Infof("generated pre-signed url for: plugin: %s, %d seconds, url: %s",
		name, SignedUrlDurationSeconds, url)
	results <- PresignedURLResult{url, plugin, nil}
}
