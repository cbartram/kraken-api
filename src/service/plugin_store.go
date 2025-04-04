package service

import (
	"errors"
	"fmt"
	log "github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"kraken-api/src/model"
)

type Period string

const (
	Month      Period = "monthly"
	ThreeMonth Period = "3-month"
	Year       Period = "yearly"
)

type PluginStore struct {
	db *gorm.DB
}

func NewPluginStore(db *gorm.DB) *PluginStore {
	return &PluginStore{
		db: db,
	}
}

// GetPlugins Returns a list of all the plugin metadata
func (s *PluginStore) GetPlugins() []model.PluginMetadata {
	tmp := make([]model.PluginMetadata, 0)
	s.db.Preload("ConfigurationOptions").
		Preload("PriceDetails").
		Find(&tmp)

	return tmp
}

// GetPluginsInPack Returns all plugins that are part of a specific plugin pack
func (s *PluginStore) GetPluginsInPack(packName string) ([]model.PluginMetadata, error) {
	// First, find the plugin pack by name
	var pack model.PluginPack
	if err := s.db.Where("name = ?", packName).First(&pack).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("plugin pack not found: %s", packName)
		}
		return nil, fmt.Errorf("error finding plugin pack: %w", err)
	}

	// Get all plugin metadata items associated with this pack through the join table
	var packItems []model.PluginPackItem
	if err := s.db.Where("pack_id = ?", pack.ID).
		Preload("PluginMetadata.ConfigurationOptions").
		Preload("PluginMetadata.PriceDetails").
		Find(&packItems).Error; err != nil {
		return nil, fmt.Errorf("error loading pack items: %w", err)
	}

	// Extract just the plugin metadata from the items
	plugins := make([]model.PluginMetadata, len(packItems))
	for i, item := range packItems {
		plugins[i] = item.PluginMetadata
	}

	return plugins, nil
}

// GetPlugin Returns a single plugin given the plugin name. If no plugin with the given name is found it returns an error.
func (s *PluginStore) GetPlugin(name string) (*model.PluginMetadata, error) {
	plugin := &model.PluginMetadata{}

	tx := s.db.Where("name = ?", name).
		Preload("ConfigurationOptions").
		Preload("PriceDetails").
		First(plugin)

	if tx.Error != nil {
		log.Errorf("failed to find plugin with name: %s, err: %v", name, tx.Error)
		return nil, errors.New("failed to find plugin with name: " + name)
	}
	return plugin, nil
}

// GetPrice returns the price for a specific plugin and period
func (s *PluginStore) GetPrice(pluginName string, period Period) (int, error) {
	plugin, err := s.GetPlugin(pluginName)
	if err != nil {
		return 0, err
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

// GetPluginPacks returns all available plugin packs
func (s *PluginStore) GetPluginPacks() []model.PluginPack {
	var packs []model.PluginPack
	s.db.Preload("Items.PluginMetadata").
		Preload("PriceDetails").
		Where("active = ?", true).
		Find(&packs)

	return packs
}

// GetPluginPack returns a specific plugin pack by name
func (s *PluginStore) GetPluginPack(name string) (*model.PluginPack, error) {
	pack := &model.PluginPack{}

	tx := s.db.Where("name = ?", name).
		Preload("Items.PluginMetadata").
		Preload("PriceDetails").
		First(pack)

	if tx.Error != nil {
		log.Errorf("failed to find plugin pack with name: %s, err: %v", name, tx.Error)
		return nil, fmt.Errorf("failed to find plugin pack with name: %s", name)
	}

	return pack, nil
}

// GetPackPrice returns the price for a specific plugin pack and period
func (s *PluginStore) GetPackPrice(packName string, period Period) (int, error) {
	pack, err := s.GetPluginPack(packName)
	if err != nil {
		return 0, err
	}

	var basePrice int
	switch period {
	case Month:
		basePrice = pack.PriceDetails.Month
	case ThreeMonth:
		basePrice = pack.PriceDetails.ThreeMonth
	case Year:
		basePrice = pack.PriceDetails.Year
	default:
		return 0, errors.New("invalid period, given: " + string(period))
	}

	// Apply the pack discount if any
	if pack.Discount > 0 {
		discountAmount := float32(basePrice) * (pack.Discount / 100.0)
		return basePrice - int(discountAmount), nil
	}

	return basePrice, nil
}

// CalculatePackSavings calculates how much a user saves by buying the pack vs individual plugins
func (s *PluginStore) CalculatePackSavings(packName string, period Period) (int, error) {
	packPrice, err := s.GetPackPrice(packName, period)
	if err != nil {
		return 0, err
	}

	plugins, err := s.GetPluginsInPack(packName)
	if err != nil {
		return 0, err
	}

	// Calculate the total price if bought individually
	totalIndividualPrice := 0
	for _, plugin := range plugins {
		var pluginPrice int
		switch period {
		case Month:
			pluginPrice = plugin.PriceDetails.Month
		case ThreeMonth:
			pluginPrice = plugin.PriceDetails.ThreeMonth
		case Year:
			pluginPrice = plugin.PriceDetails.Year
		}
		totalIndividualPrice += pluginPrice
	}

	// Calculate savings
	savings := totalIndividualPrice - packPrice
	return savings, nil
}
