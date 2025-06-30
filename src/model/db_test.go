package model

import (
	"testing"
	"time"
)

func TestUser_InFreeTrialPeriod(t *testing.T) {
	now := time.Now()

	tests := []struct {
		name     string
		user     User
		expected bool
	}{
		{
			name: "User in active free trial period",
			user: User{
				UsedFreeTrial:      true,
				FreeTrialStartTime: now.Add(-24 * time.Hour), // Started yesterday
				FreeTrialEndTime:   now.Add(24 * time.Hour),  // Ends tomorrow
			},
			expected: true,
		},
		{
			name: "User has not used free trial",
			user: User{
				UsedFreeTrial:      false,
				FreeTrialStartTime: now.Add(-24 * time.Hour),
				FreeTrialEndTime:   now.Add(24 * time.Hour),
			},
			expected: false,
		},
		{
			name: "User's free trial has expired",
			user: User{
				UsedFreeTrial:      true,
				FreeTrialStartTime: now.Add(-48 * time.Hour), // Started 2 days ago
				FreeTrialEndTime:   now.Add(-24 * time.Hour), // Ended yesterday
			},
			expected: false,
		},
		{
			name: "User's free trial hasn't started yet",
			user: User{
				UsedFreeTrial:      true,
				FreeTrialStartTime: now.Add(24 * time.Hour), // Starts tomorrow
				FreeTrialEndTime:   now.Add(48 * time.Hour), // Ends day after tomorrow
			},
			expected: false,
		},
		{
			name: "Edge case: free trial just started",
			user: User{
				UsedFreeTrial:      true,
				FreeTrialStartTime: now.Add(-1 * time.Minute), // Started 1 minute ago
				FreeTrialEndTime:   now.Add(24 * time.Hour),   // Ends tomorrow
			},
			expected: true,
		},
		{
			name: "Edge case: free trial just ended",
			user: User{
				UsedFreeTrial:      true,
				FreeTrialStartTime: now.Add(-24 * time.Hour),  // Started yesterday
				FreeTrialEndTime:   now.Add(-1 * time.Minute), // Ended 1 minute ago
			},
			expected: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := tt.user.InFreeTrialPeriod()
			if result != tt.expected {
				t.Errorf("InFreeTrialPeriod() = %v, want %v", result, tt.expected)
			}
		})
	}
}

func TestPluginSale_IsCurrentlyActive(t *testing.T) {
	now := time.Now()

	tests := []struct {
		name     string
		sale     PluginSale
		expected bool
	}{
		{
			name: "Active sale within time window",
			sale: PluginSale{
				Active:    true,
				StartTime: now.Add(-24 * time.Hour), // Started yesterday
				EndTime:   now.Add(24 * time.Hour),  // Ends tomorrow
			},
			expected: true,
		},
		{
			name: "Inactive sale within time window",
			sale: PluginSale{
				Active:    false,
				StartTime: now.Add(-24 * time.Hour),
				EndTime:   now.Add(24 * time.Hour),
			},
			expected: false,
		},
		{
			name: "Active sale but not started yet",
			sale: PluginSale{
				Active:    true,
				StartTime: now.Add(1 * time.Hour),  // Starts in 1 hour
				EndTime:   now.Add(24 * time.Hour), // Ends tomorrow
			},
			expected: false,
		},
		{
			name: "Active sale but already ended",
			sale: PluginSale{
				Active:    true,
				StartTime: now.Add(-48 * time.Hour), // Started 2 days ago
				EndTime:   now.Add(-1 * time.Hour),  // Ended 1 hour ago
			},
			expected: false,
		},
		{
			name: "Edge case: sale just started",
			sale: PluginSale{
				Active:    true,
				StartTime: now.Add(-1 * time.Minute), // Started 1 minute ago
				EndTime:   now.Add(24 * time.Hour),   // Ends tomorrow
			},
			expected: true,
		},
		{
			name: "Edge case: sale just ended",
			sale: PluginSale{
				Active:    true,
				StartTime: now.Add(-24 * time.Hour),  // Started yesterday
				EndTime:   now.Add(-1 * time.Minute), // Ended 1 minute ago
			},
			expected: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := tt.sale.IsCurrentlyActive()
			if result != tt.expected {
				t.Errorf("IsCurrentlyActive() = %v, want %v", result, tt.expected)
			}
		})
	}
}

func TestTableNameMethods(t *testing.T) {
	tests := []struct {
		name     string
		model    interface{ TableName() string }
		expected string
	}{
		{
			name:     "User table name",
			model:    User{},
			expected: "users",
		},
		{
			name:     "HardwareID table name",
			model:    HardwareID{},
			expected: "hardware_ids",
		},
		{
			name:     "Plugin table name",
			model:    Plugin{},
			expected: "plugins",
		},
		{
			name:     "PluginMetadataPriceDetails table name",
			model:    PluginMetadataPriceDetails{},
			expected: "plugin_metadata_price_details",
		},
		{
			name:     "PluginPackPriceDetails table name",
			model:    PluginPackPriceDetails{},
			expected: "plugin_pack_price_details",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := tt.model.TableName()
			if result != tt.expected {
				t.Errorf("TableName() = %v, want %v", result, tt.expected)
			}
		})
	}
}

// Benchmark tests for performance-sensitive functions
func BenchmarkUser_InFreeTrialPeriod(b *testing.B) {
	now := time.Now()
	user := User{
		UsedFreeTrial:      true,
		FreeTrialStartTime: now.Add(-24 * time.Hour),
		FreeTrialEndTime:   now.Add(24 * time.Hour),
	}

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		user.InFreeTrialPeriod()
	}
}

func BenchmarkPluginSale_IsCurrentlyActive(b *testing.B) {
	now := time.Now()
	sale := PluginSale{
		Active:    true,
		StartTime: now.Add(-24 * time.Hour),
		EndTime:   now.Add(24 * time.Hour),
	}

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		sale.IsCurrentlyActive()
	}
}
