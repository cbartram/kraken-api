package util

import (
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"kraken-api/src/model"
	"os"
	"regexp"
	"testing"
	"time"
)

func TestPurchaseDurationToDays(t *testing.T) {
	tests := []struct {
		input    string
		expected int
	}{
		{"monthly", 32},
		{"3-month", 96},
		{"yearly", 366},
		{"weekly", 0},
		{"", 0},
	}

	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			got := PurchaseDurationToDays(tt.input)
			if got != tt.expected {
				t.Errorf("PurchaseDurationToDays(%q) = %d, want %d", tt.input, got, tt.expected)
			}
		})
	}
}

func TestGetHostname(t *testing.T) {
	// Save current env
	orig := os.Getenv("HOSTNAME")
	defer os.Setenv("HOSTNAME", orig)

	t.Run("env unset", func(t *testing.T) {
		os.Unsetenv("HOSTNAME")
		got := GetHostname()
		want := "http://localhost:5173"
		if got != want {
			t.Errorf("GetHostname() = %q, want %q", got, want)
		}
	})

	t.Run("env set", func(t *testing.T) {
		os.Setenv("HOSTNAME", "something")
		got := GetHostname()
		want := "https://kraken-plugins.duckdns.org"
		if got != want {
			t.Errorf("GetHostname() = %q, want %q", got, want)
		}
	})
}

func TestParseVersion(t *testing.T) {
	tests := []struct {
		name      string
		input     string
		want      *Version
		expectErr bool
	}{
		{
			name:      "not a jar file",
			input:     "plugin.txt",
			want:      nil,
			expectErr: false,
		},
		{
			name:      "invalid jar without version",
			input:     "plugin.jar",
			want:      nil,
			expectErr: true,
		},
		{
			name:  "valid jar version",
			input: "plugin-1.2.3.jar",
			want: &Version{
				Major: 1,
				Minor: 2,
				Patch: 3,
			},
			expectErr: false,
		},
		{
			name:      "invalid jar format",
			input:     "plugin-foo.bar.jar",
			want:      nil,
			expectErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := ParseVersion(tt.input)

			if tt.expectErr {
				if err == nil {
					t.Errorf("expected error for input %q, got nil", tt.input)
				}
				return
			}

			if err != nil {
				t.Errorf("unexpected error: %v", err)
				return
			}

			if tt.want == nil && got != nil {
				t.Errorf("expected nil, got %+v", got)
				return
			}

			if tt.want != nil && (got == nil || *got != *tt.want) {
				t.Errorf("ParseVersion(%q) = %+v, want %+v", tt.input, got, tt.want)
			}
		})
	}
}

func TestIsPluginExpired(t *testing.T) {
	future := time.Now().Add(10 * time.Hour)
	past := time.Now().Add(-10 * time.Hour)

	if IsPluginExpired(future) {
		t.Error("Expected plugin not expired for future time")
	}

	if !IsPluginExpired(past) {
		t.Error("Expected plugin expired for past time")
	}
}

func TestIsValidHardwareID(t *testing.T) {
	hwids := []model.HardwareID{
		{Value: "ABC123"},
		{Value: "XYZ789"},
	}

	if !IsValidHardwareID("ABC123", hwids) {
		t.Error("Expected hardware ID ABC123 to be valid")
	}

	if IsValidHardwareID("DEF456", hwids) {
		t.Error("Expected hardware ID DEF456 to be invalid")
	}
}

func TestVersion_IsGreaterThan(t *testing.T) {
	v1 := Version{1, 2, 3}
	v2 := Version{1, 2, 2}
	v3 := Version{1, 3, 0}
	v4 := Version{2, 0, 0}
	v5 := Version{1, 2, 3}

	if !v1.IsGreaterThan(v2) {
		t.Error("Expected v1 > v2")
	}
	if v2.IsGreaterThan(v1) {
		t.Error("Expected v2 < v1")
	}
	if !v3.IsGreaterThan(v1) {
		t.Error("Expected v3 > v1")
	}
	if !v4.IsGreaterThan(v3) {
		t.Error("Expected v4 > v3")
	}
	if v1.IsGreaterThan(v5) {
		t.Error("Expected v1 == v5")
	}
}

func TestVersion_ToString(t *testing.T) {
	v := Version{1, 2, 3}
	got := v.ToString()
	want := "1.2.3"
	if got != want {
		t.Errorf("ToString() = %q, want %q", got, want)
	}
}

func TestGetUserAttributeString(t *testing.T) {
	attributes := []types.AttributeType{
		{Name: aws.String("email"), Value: aws.String("test@example.com")},
		{Name: aws.String("sub"), Value: aws.String("abc123")},
	}

	got := GetUserAttributeString(attributes, "email")
	want := "test@example.com"
	if got != want {
		t.Errorf("GetUserAttributeString() = %q, want %q", got, want)
	}

	got = GetUserAttributeString(attributes, "nonexistent")
	if got != "" {
		t.Errorf("Expected empty string for nonexistent attribute, got %q", got)
	}
}

func TestCalculateDiscountedPrice(t *testing.T) {
	tests := []struct {
		name           string
		originalPrice  int
		discount       float32
		expectedResult int
	}{
		{"no discount", 100, 0, 100},
		{"negative discount", 100, -10, 100},
		{"50 percent", 200, 50, 100},
		{"25 percent", 200, 25, 150},
		{"100 percent", 200, 100, 0},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := CalculateDiscountedPrice(tt.originalPrice, tt.discount)
			if got != tt.expectedResult {
				t.Errorf("CalculateDiscountedPrice(%d, %.2f) = %d, want %d",
					tt.originalPrice, tt.discount, got, tt.expectedResult)
			}
		})
	}
}

func TestGenerateLicenseKey(t *testing.T) {
	key, err := GenerateLicenseKey()
	if err != nil {
		t.Errorf("unexpected error generating license key: %v", err)
	}

	if len(key) != keyLength {
		t.Errorf("expected length %d, got %d", keyLength, len(key))
	}

	// Validate format
	re := regexp.MustCompile(validPattern)
	if !re.MatchString(key) {
		t.Errorf("License key format invalid: %q", key)
	}
}
