package util

import (
	"encoding/base64"
	"strings"
	"testing"
)

func TestMakeCognitoSecretHash(t *testing.T) {
	c := MakeCrypto()

	userId := "testuser"
	clientId := "client123"
	clientSecret := "supersecret"

	hash := c.MakeCognitoSecretHash(userId, clientId, clientSecret)

	if hash == "" {
		t.Error("Expected non-empty hash")
	}

	// Check that it is valid base64
	_, err := base64.StdEncoding.DecodeString(hash)
	if err != nil {
		t.Errorf("Hash is not valid base64: %v", err)
	}
}

func TestGeneratePassword_Success(t *testing.T) {
	c := MakeCrypto()

	tests := []struct {
		name           string
		config         PasswordConfig
		expectErr      bool
		expectContains []string
	}{
		{
			name: "Minimum length, no special requirements",
			config: PasswordConfig{
				Length: 8,
			},
			expectErr: false,
		},
		{
			name: "Require uppercase, lowercase, number, special",
			config: PasswordConfig{
				Length:         12,
				RequireUpper:   true,
				RequireLower:   true,
				RequireNumber:  true,
				RequireSpecial: true,
			},
			expectErr: false,
			expectContains: []string{
				upperChars,
				lowerChars,
				numberChars,
				specialChars,
			},
		},
		{
			name: "Require only uppercase",
			config: PasswordConfig{
				Length:       8,
				RequireUpper: true,
			},
			expectErr: false,
			expectContains: []string{
				upperChars,
			},
		},
		{
			name: "Require only special",
			config: PasswordConfig{
				Length:         10,
				RequireSpecial: true,
			},
			expectErr: false,
			expectContains: []string{
				specialChars,
			},
		},
		{
			name: "Too short length",
			config: PasswordConfig{
				Length: 7,
			},
			expectErr: true,
		},
		{
			name: "Length too short for requirements",
			config: PasswordConfig{
				Length:        3,
				RequireUpper:  true,
				RequireLower:  true,
				RequireNumber: true,
			},
			expectErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pw, err := c.GeneratePassword(tt.config)
			if tt.expectErr {
				if err == nil {
					t.Errorf("expected error, got none, password=%q", pw)
				}
				return
			}

			if err != nil {
				t.Errorf("unexpected error: %v", err)
			}

			if len(pw) != tt.config.Length {
				t.Errorf("expected password length %d, got %d", tt.config.Length, len(pw))
			}

			// Check required character types are present
			for _, charSet := range tt.expectContains {
				found := false
				for _, c := range pw {
					if strings.ContainsRune(charSet, c) {
						found = true
						break
					}
				}
				if !found {
					t.Errorf("password %q does not contain required characters from set %q", pw, charSet)
				}
			}
		})
	}
}

func Test_getRandomChar_EmptySet(t *testing.T) {
	_, err := getRandomChar("")
	if err == nil {
		t.Error("expected error for empty character set")
	}
}

func Test_shuffleBytes(t *testing.T) {
	input := []byte("ABCDEFG")
	shuffled, err := shuffleBytes(input)
	if err != nil {
		t.Errorf("shuffleBytes returned error: %v", err)
	}

	if len(shuffled) != len(input) {
		t.Errorf("shuffled length mismatch: got %d, want %d", len(shuffled), len(input))
	}

	// Ensure same characters exist after shuffle
	origMap := make(map[byte]int)
	for _, b := range input {
		origMap[b]++
	}

	for _, b := range shuffled {
		origMap[b]--
	}

	for k, v := range origMap {
		if v != 0 {
			t.Errorf("byte %q count mismatch after shuffle", k)
		}
	}
}
