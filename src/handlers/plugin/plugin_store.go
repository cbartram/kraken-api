package plugin

import (
	"errors"
)

type Period string

const (
	Month      Period = "month"
	ThreeMonth Period = "3-month"
	Year       Period = "year"
)

// Tier prices in tokens
const (
	// Tier 1 - Basic plugins
	Tier1Month      = 100  // $2 equivalent
	Tier1ThreeMonth = 250  // ~17% discount from monthly
	Tier1Year       = 1000 // ~17% discount from quarterly

	// Tier 2 - Medium plugins
	Tier2Month      = 200  // $4 equivalent
	Tier2ThreeMonth = 500  // ~17% discount from monthly
	Tier2Year       = 1000 // ~58% discount from quarterly

	// Tier 3 - Premium plugins
	Tier3Month      = 500  // $6 equivalent
	Tier3ThreeMonth = 1000 // ~33% discount from monthly
	Tier3Year       = 5000 // ~17% discount from quarterly
)

type PriceDetails struct {
	Month      int
	ThreeMonth int
	Year       int
}

type Plugin struct {
	Name         string
	PriceDetails PriceDetails
	Tier         int
}

type PluginStore struct {
	plugins map[string]Plugin
}

func NewPluginStore() *PluginStore {
	store := &PluginStore{
		plugins: make(map[string]Plugin),
	}

	// Tier 3 Premium Plugins
	store.AddPlugin("Cox-Helper", PriceDetails{
		Month:      Tier3Month,
		ThreeMonth: Tier3ThreeMonth,
		Year:       Tier3Year,
	}, 3)

	store.AddPlugin("Theatre-of-Blood", PriceDetails{
		Month:      Tier3Month,
		ThreeMonth: Tier3ThreeMonth,
		Year:       Tier3Year,
	}, 3)

	store.AddPlugin("Gauntlet-Extended", PriceDetails{
		Month:      Tier3Month,
		ThreeMonth: Tier3ThreeMonth,
		Year:       Tier3Year,
	}, 3)

	// Tier 2 Medium Plugins
	store.AddPlugin("Zulrah", PriceDetails{
		Month:      Tier2Month,
		ThreeMonth: Tier2ThreeMonth,
		Year:       Tier2Year,
	}, 2)

	store.AddPlugin("Nightmare", PriceDetails{
		Month:      Tier2Month,
		ThreeMonth: Tier2ThreeMonth,
		Year:       Tier2Year,
	}, 2)

	store.AddPlugin("Alchemical-Hydra", PriceDetails{
		Month:      Tier2Month,
		ThreeMonth: Tier2ThreeMonth,
		Year:       Tier2Year,
	}, 2)

	// Tier 1 Basic Plugins
	store.AddPlugin("Vorkath", PriceDetails{
		Month:      Tier1Month,
		ThreeMonth: Tier1ThreeMonth,
		Year:       Tier1Year,
	}, 1)

	store.AddPlugin("Cerberus", PriceDetails{
		Month:      Tier1Month,
		ThreeMonth: Tier1ThreeMonth,
		Year:       Tier1Year,
	}, 1)

	store.AddPlugin("Effect-Timers", PriceDetails{
		Month:      Tier1Month,
		ThreeMonth: Tier1ThreeMonth,
		Year:       Tier1Year,
	}, 1)

	return store
}

// AddPlugin adds a new plugin to the store
func (s *PluginStore) AddPlugin(name string, prices PriceDetails, tier int) {
	s.plugins[name] = Plugin{
		Name:         name,
		PriceDetails: prices,
		Tier:         tier,
	}
}

// GetPrice returns the price for a specific plugin and period
func (s *PluginStore) GetPrice(pluginName string, period Period) (int, error) {
	plugin, exists := s.plugins[pluginName]
	if !exists {
		return 0, errors.New("plugin not found")
	}

	switch period {
	case Month:
		return plugin.PriceDetails.Month, nil
	case ThreeMonth:
		return plugin.PriceDetails.ThreeMonth, nil
	case Year:
		return plugin.PriceDetails.Year, nil
	default:
		return 0, errors.New("invalid period")
	}
}
