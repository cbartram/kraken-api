package service

import (
	"errors"
	log "github.com/sirupsen/logrus"
	"maps"
)

type Period string

const (
	Month      Period = "monthly"
	ThreeMonth Period = "3-month"
	Year       Period = "yearly"
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
	Month      int `json:"month"`
	ThreeMonth int `json:"threeMonth"`
	Year       int `json:"year"`
}

type PluginMetadata struct {
	Name                 string         `json:"name"`
	Title                string         `json:"title"`
	Description          string         `json:"description"`
	ImageUrl             string         `json:"imageUrl"`
	VideoUrl             string         `json:"videoUrl"`
	TopPick              bool           `json:"topPick"`
	ConfigurationOptions []PluginConfig `json:"configurationOptions"`
	PriceDetails         PriceDetails   `json:"priceDetails"`
	Tier                 int            `json:"tier"`
}

type PluginConfig struct {
	Name        string   `json:"name"`
	Section     string   `json:"section"`
	Description string   `json:"description"`
	Type        string   `json:"type"`
	IsBool      bool     `json:"isBool"`
	Values      []string `json:"values"`
}

type PluginStore struct {
	plugins map[string]PluginMetadata
}

func NewPluginStore() *PluginStore {
	store := &PluginStore{
		plugins: make(map[string]PluginMetadata),
	}

	// Tier 3 Premium Plugins
	store.AddPlugin(
		"Cox-Helper",
		"Chambers Helper",
		"Tracks Olm rotations, specials, tick counters, and various boss helpers for CoX.",
		"https://kraken-plugins.duckdns.org/olm.png",
		"",
		true,
		[]PluginConfig{
			{},
		},
		PriceDetails{
			Month:      Tier3Month,
			ThreeMonth: Tier3ThreeMonth,
			Year:       Tier3Year,
		}, 3)

	store.AddPlugin(
		"Theatre-of-Blood",
		"Theatre of Blood",
		"All in one plugin for Theatre of Blood. Maiden attack ticks, Nylo menu entry swaps, aggressive nylo highlights, bloat timers, Sote prayer and maze overlay, tick eat timers, Verzik crab identifier and more!",
		"https://kraken-plugins.duckdns.org/tob.png",
		"",
		true,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier3Month,
			ThreeMonth: Tier3ThreeMonth,
			Year:       Tier3Year,
		}, 3)

	store.AddPlugin(
		"Gauntlet-Extended",
		"Gauntlet Extended",
		"Additional helpers for the gauntlet and corrupted gauntlet tracking hunleff prayer and attacks. Resource highlights, demi-boss highlights, resource tracking and more.",
		"https://kraken-plugins.duckdns.org/gauntlet.png",
		"",
		true,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier3Month,
			ThreeMonth: Tier3ThreeMonth,
			Year:       Tier3Year,
		}, 3)

	// Tier 2 Medium Plugins
	store.AddPlugin(
		"Zulrah",
		"Zulrah",
		"Tracks Zulrah rotations, snakelings, where to stand, where to move, and what to pray.",
		"https://kraken-plugins.duckdns.org/zulrah.png",
		"",
		false,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier2Month,
			ThreeMonth: Tier2ThreeMonth,
			Year:       Tier2Year,
		}, 2)

	store.AddPlugin(
		"Nightmare",
		"Nightmare of Ashihama",
		"All in one plugin for Nightmare and Phosani's Nightmare which tracks prayers, parasites, special attacks, pillar health and more.",
		"https://kraken-plugins.duckdns.org/nightmare.png",
		"",
		false,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier2Month,
			ThreeMonth: Tier2ThreeMonth,
			Year:       Tier2Year,
		}, 2)

	store.AddPlugin(
		"Alchemical-Hydra",
		"Alchemical Hydra",
		"Tracks your prayers, special attacks and when to switch for Hydra including the enrage phase. This plugin has markers for acid, fire and lightning.",
		"https://kraken-plugins.duckdns.org/hydra.png",
		"",
		true,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier2Month,
			ThreeMonth: Tier2ThreeMonth,
			Year:       Tier2Year,
		}, 2)

	// Tier 1 Basic Plugins
	store.AddPlugin(
		"Vorkath",
		"Vorkath",
		"Tracks acid, woox walk paths, and vorkaths special attack count.",
		"https://kraken-plugins.duckdns.org/vorkath.png",
		"",
		false,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier1Month,
			ThreeMonth: Tier1ThreeMonth,
			Year:       Tier1Year,
		}, 1)

	store.AddPlugin(
		"Cerberus",
		"Cerberus",
		"Tracks ghosts, Cerberus prayer rotations, and more.",
		"https://kraken-plugins.duckdns.org/cerberus.png",
		"",
		false,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier1Month,
			ThreeMonth: Tier1ThreeMonth,
			Year:       Tier1Year,
		}, 1)

	store.AddPlugin(
		"Effect-Timers",
		"Effect Timers",
		"Tracks freeze, teleblock, and other timers!",
		"https://kraken-plugins.duckdns.org/timers.png",
		"",
		false,
		[]PluginConfig{},
		PriceDetails{
			Month:      Tier1Month,
			ThreeMonth: Tier1ThreeMonth,
			Year:       Tier1Year,
		}, 1)

	return store
}

// AddPlugin adds a new plugin to the store
func (s *PluginStore) AddPlugin(name, title, description, imageUrl, videoUrl string, topPick bool, config []PluginConfig, prices PriceDetails, tier int) {
	if _, ok := s.plugins[name]; ok {
		log.Errorf("plugin with name: %s already exists. Skipping plugin add.", name)
		return
	}

	s.plugins[name] = PluginMetadata{
		Name:                 name,
		Title:                title,
		ConfigurationOptions: config,
		Description:          description,
		ImageUrl:             imageUrl,
		VideoUrl:             videoUrl,
		TopPick:              topPick,
		PriceDetails:         prices,
		Tier:                 tier,
	}
}

// GetPlugins Returns a list of all the plugin metadata
func (s *PluginStore) GetPlugins() []PluginMetadata {
	tmp := make([]PluginMetadata, 0)
	for v := range maps.Values(s.plugins) {
		tmp = append(tmp, v)
	}
	return tmp
}

// GetPlugin Returns a single plugin given the plugin name. If no plugin with the given name is found it returns an error.
func (s *PluginStore) GetPlugin(name string) (*PluginMetadata, error) {
	plugin, exists := s.plugins[name]
	if !exists {
		return nil, errors.New("plugin with name:" + name + " not found")
	}
	return &plugin, nil
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
		return 0, errors.New("invalid period, given: " + string(period))
	}
}
