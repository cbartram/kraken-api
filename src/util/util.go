package util

import (
	log "github.com/sirupsen/logrus"
	"time"
)

// IsPluginExpired Returns true when the plugin expiration date is past the current date and false otherwise.
func IsPluginExpired(expirationTimestamp string) (bool, error) {
	expiresAt, err := time.Parse(time.RFC3339, expirationTimestamp)
	now := time.Now()
	if err != nil {
		return true, err
	}

	if now.After(expiresAt) {
		return true, nil
	}

	log.Infof("plugin is still valid for: %v days", expiresAt.Sub(now).Hours()/24)
	return false, nil
}
