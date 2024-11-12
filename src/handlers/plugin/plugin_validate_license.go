package plugin

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"kraken-api/src/util"
	"net/http"
	"slices"
)

type PluginValidateLicenseHandler struct{}

// HandleRequest Handles the /api/v1/plugin/validate-license route which validates a users license. Validating a license takes place in 3 parts:
// 1. Validate what they sent = what is on their account
// 2. Validate (once again) that the expiration timestamp for the plugin is ok
// 3. Validate that the hardware id matches what is on the users account
func (p *PluginValidateLicenseHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.LicenseKeyRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	// TODO Validate that the license key matches a regex.

	authManger := client.MakeCognitoService()
	log.Infof("fetching user attributes with access token")
	attr, err := authManger.GetUserAttributes(ctx, &reqBody.Credentials.AccessToken)

	licenseKeys := util.GetUserAttribute(attr, LICENSE_KEY)
	expirationTimestamps := util.GetUserAttribute(attr, EXPIRATION_TIMESTAMP_KEY)
	hardwareIds := util.GetUserAttribute(attr, HARDWARE_ID_KEY)

	if !slices.Contains(licenseKeys, reqBody.LicenseKey) {
		log.Infof("user passed invalid license key: %s key does not belong to user acct: %s", reqBody.LicenseKey, licenseKeys)
		c.JSON(http.StatusBadRequest, gin.H{
			"error": fmt.Sprintf("license key invalid, no license %s in user account", reqBody.LicenseKey),
		})
		return
	}

	for i, _ := range licenseKeys {
		expired, err := util.IsPluginExpired(expirationTimestamps[i])
		pluginHardwareId := hardwareIds[i]
		if err != nil {
			log.Errorf("error: failed to parse plugin expiration timestamp: %s to RFC3339 format. error: %s", expirationTimestamps[i], err.Error())
			continue
		}

		if expired {
			log.Infof("current time is after plugin expiration time, license key is expired.")
			c.JSON(http.StatusBadRequest, gin.H{
				"error": "license key invalid: expired",
			})
			return
		}

		if pluginHardwareId != reqBody.HardwareID {
			log.Infof("hardware id passed: %s does not match plugin HWID: %s", reqBody.HardwareID, pluginHardwareId)
			c.JSON(http.StatusBadRequest, gin.H{
				"error": fmt.Sprintf("license key invalid: hardware id %s is invalid", reqBody.HardwareID),
			})
			return
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"error": nil,
	})
}
