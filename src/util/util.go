package util

import (
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	log "github.com/sirupsen/logrus"
	"strings"
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

// GetUserAttribute Retrieves and parses a user attribute from Cognito into an array of strings. Most
// attributes are CSV strings. Examples include: purchased plugins, plugin expiration dates, plugin purchase dates etc...
func GetUserAttribute(attributes []types.AttributeType, attributeName string) []string {
	for _, attribute := range attributes {
		if aws.ToString(attribute.Name) == attributeName {
			return strings.Split(aws.ToString(attribute.Value), ",")
		}
	}

	return make([]string, 0)
}

func MakeAttribute(key, value string) types.AttributeType {
	attr := types.AttributeType{
		Name:  &key,
		Value: &value,
	}
	return attr
}
