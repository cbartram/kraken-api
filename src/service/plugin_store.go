package service

import (
	"errors"
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
