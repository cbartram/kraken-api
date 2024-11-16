package util

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"math/big"
	"sync"
)

const (
	// Minimum password requirements based on AWS Cognito defaults
	minLength    = 8
	upperChars   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	lowerChars   = "abcdefghijklmnopqrstuvwxyz"
	numberChars  = "0123456789"
	specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
)

// PasswordConfig holds the configuration for password generation
type PasswordConfig struct {
	Length         int
	RequireUpper   bool
	RequireLower   bool
	RequireNumber  bool
	RequireSpecial bool
}

type Crypto struct {
	mu sync.RWMutex
}

// MakeCognitoSecretHash Creates a hash based on the user id, service id and secret which must be
// sent with every cognito auth request (along with a refresh token) to get a new access token.
func (c *Crypto) MakeCognitoSecretHash(userId, clientId, clientSecret string) string {
	usernameClientID := userId + clientId
	hash := hmac.New(sha256.New, []byte(clientSecret))
	hash.Write([]byte(usernameClientID))
	digest := hash.Sum(nil)

	return base64.StdEncoding.EncodeToString(digest)
}

// NewSecurePassword creates a new SecurePassword instance
func MakeCrypto() *Crypto {
	return &Crypto{}
}

// GeneratePassword generates a cryptographically secure password
func (c *Crypto) GeneratePassword(config PasswordConfig) (string, error) {
	if config.Length < minLength {
		return "", errors.New("password length must be at least 8 characters")
	}

	// Initialize the password builder with required characters
	var requiredChars []byte
	allChars := ""

	if config.RequireUpper {
		char, err := getRandomChar(upperChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
		allChars += upperChars
	}

	if config.RequireLower {
		char, err := getRandomChar(lowerChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
		allChars += lowerChars
	}

	if config.RequireNumber {
		char, err := getRandomChar(numberChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
		allChars += numberChars
	}

	if config.RequireSpecial {
		char, err := getRandomChar(specialChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
		allChars += specialChars
	}

	remainingLength := config.Length - len(requiredChars)
	if remainingLength < 0 {
		return "", errors.New("password length too short to satisfy requirements")
	}

	for i := 0; i < remainingLength; i++ {
		char, err := getRandomChar(allChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
	}

	password, err := shuffleBytes(requiredChars)
	if err != nil {
		return "", err
	}

	return string(password), nil
}

// Helper function to get a random character from a string
func getRandomChar(chars string) (byte, error) {
	if len(chars) == 0 {
		return 0, errors.New("character set is empty")
	}

	n, err := rand.Int(rand.Reader, big.NewInt(int64(len(chars))))
	if err != nil {
		return 0, err
	}

	return chars[n.Int64()], nil
}

// Helper function to securely shuffle a byte slice
func shuffleBytes(bytes []byte) ([]byte, error) {
	result := make([]byte, len(bytes))
	copy(result, bytes)

	for i := len(result) - 1; i > 0; i-- {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(i+1)))
		if err != nil {
			return nil, err
		}
		j := n.Int64()
		result[i], result[j] = result[j], result[i]
	}

	return result, nil
}
