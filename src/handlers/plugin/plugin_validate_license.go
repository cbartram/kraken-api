package plugin

import (
	"encoding/json"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/util"
	"net/http"
	"slices"
)

type ValidateLicenseHandler struct{}

func (p *ValidateLicenseHandler) HandleSingleValidationRequest(c *gin.Context) {
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

	log.Infof("fetching user attributes with access token")
	// TODO DB
	attr := []types.AttributeType{}

	licenseKeys := util.GetUserAttribute(attr, LicenseKey)
	expirationTimestamps := util.GetUserAttribute(attr, ExpirationTimestampKey)
	hardwareIds := util.GetUserAttribute(attr, HardwareIdKey)
	pluginNames := util.GetUserAttribute(attr, PurchasedPluginsKey)
	foundPlugin := false

	for i, name := range pluginNames {
		if name == reqBody.PluginName {
			foundPlugin = true
			validLicense := licenseKeys[i]
			providedLicense := reqBody.LicenseKey

			if hardwareIds[0] != reqBody.HardwareID {
				log.Infof("hardware id passed: %s does not match plugin HWID: %s", reqBody.HardwareID, hardwareIds[0])
				c.JSON(http.StatusOK, gin.H{
					name: "",
				})
				return
			}

			expired, err := util.IsPluginExpired(expirationTimestamps[i])
			if err != nil {
				log.Errorf("error: failed to parse plugin expiration timestamp: %s to RFC3339 format. error: %s", expirationTimestamps[i], err.Error())
				c.JSON(http.StatusOK, gin.H{
					name: "",
				})
				return
			}

			if expired {
				log.Infof("current time is after plugin expiration time, license key is expired.")
				c.JSON(http.StatusOK, gin.H{
					name: "",
				})
				return
			}
			if providedLicense != validLicense {
				log.Infof("provided license key: %s does not match valid license key: %s for plugin: %s", providedLicense, validLicense, name)
				c.JSON(http.StatusOK, gin.H{
					name: "",
				})
				return
			}

			c.JSON(http.StatusOK, gin.H{
				name: expirationTimestamps[i],
			})
		}
	}

	if !foundPlugin {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "account has not purchased plugin: " + reqBody.PluginName + " but is requesting validation.",
		})
	}
}

// HandleRequest Handles the /api/v1/plugin/validate-license route which validates a users license. Validating a license takes place in 3 parts:
// 1. Validate what they sent = what is on their account
// 2. Validate (once again) that the expiration timestamp for the plugin is ok
// 3. Validate that the hardware id matches what is on the users account
func (p *ValidateLicenseHandler) HandleRequest(c *gin.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.LicenseKeyRequestBatch
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	// TODO Validate that the license key matches a regex.
	log.Infof("fetching user attributes with access token")
	// TODO DB
	attr := []types.AttributeType{}

	licenseKeys := util.GetUserAttribute(attr, LicenseKey)
	expirationTimestamps := util.GetUserAttribute(attr, ExpirationTimestampKey)
	hardwareIds := util.GetUserAttribute(attr, HardwareIdKey)
	pluginNames := util.GetUserAttribute(attr, PurchasedPluginsKey)

	plugins := map[string]string{}

	// Validate expiration and hwid for all plugins we have in the DB
	for i, name := range pluginNames {
		if !slices.Contains(hardwareIds, reqBody.HardwareID) {
			log.Infof("hardware id(s): %s does not match plugin HWID: %s", reqBody.HardwareID, hardwareIds[0])
			plugins[name] = ""
			continue
		}

		expired, err := util.IsPluginExpired(expirationTimestamps[i])
		if err != nil {
			log.Errorf("error: failed to parse plugin expiration timestamp: %s to RFC3339 format. error: %s", expirationTimestamps[i], err.Error())
			plugins[name] = ""
			continue
		}

		if expired {
			log.Infof("current time is after plugin expiration time, license key is expired.")
			plugins[name] = ""
			continue
		}

		providedLicenseKey := reqBody.Plugins[name]
		validLicenseKey := licenseKeys[i]

		if providedLicenseKey != validLicenseKey {
			log.Infof("provided license key: %s does not match valid license key: %s for plugin: %s", providedLicenseKey, validLicenseKey, name)
			plugins[name] = ""
		}
		plugins[name] = expirationTimestamps[i]
	}

	c.JSON(http.StatusOK, plugins)
}
