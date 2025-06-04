package util

import (
	"crypto/rand"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"kraken-api/src/model"
	"os"
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

func PurchaseDurationToDays(purchaseDuration string) int {
	switch purchaseDuration {
	case "monthly":
		return 32
	case "3-month":
		return 96
	case "yearly":
		return 366
	}
	return 0
}

func GetHostname() string {
	host := os.Getenv("HOSTNAME")

	if host == "" {
		return "http://localhost:5173"
	}

	return "https://kraken-plugins.duckdns.org"
}

// ParseVersion extracts version from object name
func ParseVersion(objectName string) (*Version, error) {
	if !strings.HasSuffix(objectName, ".jar") {
		return nil, nil
	}

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

func (v Version) ToString() string {
	return fmt.Sprintf("%d.%d.%d", v.Major, v.Minor, v.Patch)
}

func GetUserAttributeString(attributes []types.AttributeType, attributeName string) string {
	for _, attribute := range attributes {
		if aws.ToString(attribute.Name) == attributeName {
			return aws.ToString(attribute.Value)
		}
	}

	return ""
}

// CalculateDiscountedPrice Calculates the discounted price given a percentage discount and the original price of the plugin.
func CalculateDiscountedPrice(originalPrice int, discountPercent float32) int {
	if discountPercent <= 0 {
		return originalPrice
	}
	discountAmount := float32(originalPrice) * (discountPercent / 100.0)
	return int(float32(originalPrice) - discountAmount)
}

// GenerateLicenseKey generates a random license key in the format "XXXXX-XXXXX-XXXXX-XXXXX"
func GenerateLicenseKey() (string, error) {
	totalChars := sectionSize * numSections
	bytes := make([]byte, totalChars)

	if _, err := rand.Read(bytes); err != nil {
		return "", fmt.Errorf("failed to generate random bytes: %v", err)
	}

	result := make([]string, numSections)

	for i := 0; i < totalChars; i++ {
		// Convert to character in range A-Z (0-25 + 65 for ASCII 'A')
		bytes[i] = bytes[i]%26 + 65
		result[i/sectionSize] += string(bytes[i])
	}

	return strings.Join(result, "-"), nil
}
