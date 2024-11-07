package client

import (
	"crypto/rand"
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

// SecurePassword generates and stores passwords securely
type SecurePassword struct {
	mu sync.RWMutex
}

// NewSecurePassword creates a new SecurePassword instance
func MakeSecurePassword() *SecurePassword {
	return &SecurePassword{}
}

// GeneratePassword generates a cryptographically secure password
func (sp *SecurePassword) GeneratePassword(config PasswordConfig) (string, error) {
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

	// Generate remaining characters
	remainingLength := config.Length - len(requiredChars)
	if remainingLength < 0 {
		return "", errors.New("password length too short to satisfy requirements")
	}

	// Generate random characters for the remaining length
	for i := 0; i < remainingLength; i++ {
		char, err := getRandomChar(allChars)
		if err != nil {
			return "", err
		}
		requiredChars = append(requiredChars, char)
	}

	// Shuffle the password
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
