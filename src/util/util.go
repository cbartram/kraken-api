package util

import (
	"crypto/rand"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"kraken-api/src/model"
	"regexp"
	"strconv"
	"strings"
	"time"
)

const (
	// Define the format: 5 chars + hyphen + 5 chars + hyphen + 5 chars + hyphen + 5 chars
	keyLength    = 23 // Total length including hyphens
	sectionSize  = 5  // Number of characters in each section
	numSections  = 4  // Number of sections
	validPattern = "^[A-Z]{5}-[A-Z]{5}-[A-Z]{5}-[A-Z]{5}$"
)

// Version represents a semantic version
type Version struct {
	Major int
	Minor int
	Patch int
}

// ParseVersion extracts version from object name
func ParseVersion(objectName string) (*Version, error) {
	// Extract version using regex
	re := regexp.MustCompile(`-(\d+)\.(\d+)\.(\d+)\.jar$`)
	matches := re.FindStringSubmatch(objectName)

	if len(matches) != 4 {
		return nil, fmt.Errorf("invalid version format in object name: %s", objectName)
	}

	major, _ := strconv.Atoi(matches[1])
	minor, _ := strconv.Atoi(matches[2])
	patch, _ := strconv.Atoi(matches[3])

	return &Version{
		Major: major,
		Minor: minor,
		Patch: patch,
	}, nil
}

// IsPluginExpired Returns true when the provided expiration time is past the current time and false otherwise.
func IsPluginExpired(expirationTimestamp time.Time) bool {
	return time.Now().After(expirationTimestamp)
}

// IsValidHardwareID Returns true if the given hardware id is valid and false otherwise.
func IsValidHardwareID(hardwareID string, hardwareIDs []model.HardwareID) bool {
	for _, hwid := range hardwareIDs {
		if hardwareID == hwid.Value {
			return true
		}
	}
	return false
}

// IsGreaterThan returns true if v is greater than other
func (v Version) IsGreaterThan(other Version) bool {
	if v.Major != other.Major {
		return v.Major > other.Major
	}
	if v.Minor != other.Minor {
		return v.Minor > other.Minor
	}
	return v.Patch > other.Patch
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

func GetUserAttributeString(attributes []types.AttributeType, attributeName string) string {
	for _, attribute := range attributes {
		if aws.ToString(attribute.Name) == attributeName {
			return aws.ToString(attribute.Value)
		}
	}

	return ""
}

func MakeAttribute(key, value string) types.AttributeType {
	attr := types.AttributeType{
		Name:  &key,
		Value: &value,
	}
	return attr
}

// GenerateLicenseKey generates a random license key in the format "XXXXX-XXXXX-XXXXX-XXXXX"
func GenerateLicenseKey() (string, error) {
	totalChars := sectionSize * numSections
	bytes := make([]byte, totalChars)

	if _, err := rand.Read(bytes); err != nil {
		return "", fmt.Errorf("failed to generate random bytes: %v", err)
	}

	// Convert random bytes to uppercase letters
	result := make([]string, numSections)
	for i := 0; i < totalChars; i++ {
		// Convert to character in range A-Z (0-25 + 65 for ASCII 'A')
		bytes[i] = byte(bytes[i]%26 + 65)
		result[i/sectionSize] += string(bytes[i])
	}

	return strings.Join(result, "-"), nil
}

func ValidateLicenseKey(key string) bool {
	if len(key) != keyLength {
		return false
	}

	// Check if the key matches the pattern using regex
	match, err := regexp.MatchString(validPattern, key)
	if err != nil {
		return false
	}

	return match
}
