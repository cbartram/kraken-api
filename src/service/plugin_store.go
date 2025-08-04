package service

import (
	"errors"
	"fmt"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"kraken-api/src/cache"
	"kraken-api/src/model"
	"sync"
	"time"
)

type Period string

const (
	Month      Period = "monthly"
	ThreeMonth Period = "3-month"
	Year       Period = "yearly"
	Lifetime   Period = "lifetime"
)

type PluginStore struct {
	db            *gorm.DB
	cacheMu       sync.Mutex
	cache         map[string]string
	redisCache    *cache.RedisCache
	cacheTime     time.Time
	cacheDuration time.Duration
	log           *zap.SugaredLogger
}

func NewPluginStore(db *gorm.DB, cache *cache.RedisCache, log *zap.SugaredLogger) *PluginStore {
	return &PluginStore{
		db:            db,
		redisCache:    cache,
		cache:         make(map[string]string),
		cacheDuration: 20 * time.Minute,
		log:           log,
	}
}

// GetPlugins Returns a list of all the plugin metadata
func (s *PluginStore) GetPlugins() []model.PluginMetadata {
	key := "pluginstore:metadata:all"

	plugins := make([]model.PluginMetadata, 0)
	if err := s.redisCache.Get(key, &plugins); err == nil {
		s.log.Infof("cache hit for key: %v", key)
		return plugins
	}

	s.db.Preload("ConfigurationOptions").
		Preload("PriceDetails").
		Preload("Versions", func(db *gorm.DB) *gorm.DB {
			return db.Order("plugin_versions.id DESC") // Newer versions first
		}).
		Find(&plugins)

	for i := range plugins {
		for j := range plugins[i].Versions {
			if plugins[i].Versions[j].Latest {
				plugins[i].LatestVersion = plugins[i].Versions[j].Version
			}
		}
	}

	if err := s.redisCache.Set(key, &plugins, s.cacheDuration); err != nil {
		s.log.Errorf("err caching plugins: %v", err)
	}

	return plugins
}

func (s *PluginStore) GetPluginsInPack(packName string) ([]model.PluginMetadata, error) {
	key := fmt.Sprintf("pluginstore:packplugins:%s", packName)

	var plugins []model.PluginMetadata
	if err := s.redisCache.Get(key, &plugins); err == nil {
		s.log.Infof("cache hit for plugins in pack: %v", key)
		return plugins, nil
	}

	var pack model.PluginPack
	if err := s.db.Where("name = ?", packName).First(&pack).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("plugin pack not found: %s", packName)
		}
		return nil, fmt.Errorf("error finding plugin pack: %w", err)
	}

	var packItems []model.PluginPackItem
	if err := s.db.Where("pack_id = ?", pack.ID).
		Preload("PluginMetadata.ConfigurationOptions").
		Preload("PluginMetadata.PriceDetails").
		Preload("PluginMetadata.Versions", func(db *gorm.DB) *gorm.DB {
			return db.Order("plugin_versions.id DESC") // Newer versions first
		}).
		Find(&packItems).Error; err != nil {
		return nil, fmt.Errorf("error loading pack items: %w", err)
	}

	plugins = make([]model.PluginMetadata, len(packItems))
	for i, item := range packItems {
		plugins[i] = item.PluginMetadata
	}

	if err := s.redisCache.Set(key, &plugins, s.cacheDuration); err != nil {
		s.log.Errorf("error caching plugins in pack: %v", err)
	}

	return plugins, nil
}

// GetPlugin Returns a single plugin given the plugin name. If no plugin with the given name is found it returns an error.
func (s *PluginStore) GetPlugin(name string) (*model.PluginMetadata, error) {
	plugin := &model.PluginMetadata{}

	// We do have pluginstore:metadata:all in the cache, but we may not have a specific plugin in the cache
	// This saves double cache lookups for both metadata and version
	key := fmt.Sprintf("pluginstore:metadata:%s", name)
	if err := s.redisCache.Get(key, &plugin); err == nil {
		s.log.Infof("cache hit for single plugin with key: %v", key)
		return plugin, nil
	}

	tx := s.db.Where("name = ?", name).
		Preload("ConfigurationOptions").
		Preload("PriceDetails").
		Preload("Versions", func(db *gorm.DB) *gorm.DB {
			return db.Order("plugin_versions.id DESC") // Newer versions first
		}).
		First(plugin)

	for i := range plugin.Versions {
		if plugin.Versions[i].Latest {
			plugin.LatestVersion = plugin.Versions[i].Version
		}
	}

	if tx.Error != nil {
		return nil, errors.New("failed to find plugin with name: " + name)
	}

	err := s.redisCache.Set(key, plugin, s.cacheDuration)
	if err != nil {
		s.log.Errorf("error caching single plugin with key: %s err: %v", key, err)
	}

	return plugin, nil
}

// GetPrice returns the price for a specific plugin and period
func (s *PluginStore) GetPrice(pluginName string, period Period, isPack bool) (int, error) {
	plugin, err := s.GetPlugin(pluginName)
	if err != nil {
		return 0, err
	}

	if isPack && period == Lifetime {
		return 0, errors.New("plugin pack cannot be purchased with: 'lifetime' duration")
	}

	switch period {
	case Month:
		return plugin.PriceDetails.Month, nil
	case ThreeMonth:
		return plugin.PriceDetails.ThreeMonth, nil
	case Year:
		return plugin.PriceDetails.Year, nil
	case Lifetime:
		return plugin.PriceDetails.Lifetime, nil
	default:
		return 0, errors.New("invalid period, given: " + string(period))
	}
}

// GetPluginPacks returns all available plugin packs
func (s *PluginStore) GetPluginPacks() []model.PluginPack {
	key := "pluginstore:packs:all"

	var packs []model.PluginPack
	if err := s.redisCache.Get(key, &packs); err == nil {
		s.log.Infof("cache hit for all plugin packs: %v", key)
		return packs
	}

	// Cache miss – fetch from DB
	s.db.Preload("Items.PluginMetadata").
		Preload("PriceDetails").
		Where("active = ?", true).
		Find(&packs)

	if err := s.redisCache.Set(key, &packs, s.cacheDuration); err != nil {
		s.log.Errorf("error caching all plugin packs: %v", err)
	}

	return packs
}

// GetPluginPack returns a specific plugin pack by name
func (s *PluginStore) GetPluginPack(name string) (*model.PluginPack, error) {
	key := fmt.Sprintf("pluginstore:pack:%s", name)

	var pack model.PluginPack
	if err := s.redisCache.Get(key, &pack); err == nil {
		s.log.Infof("cache hit for plugin pack: %v", key)
		return &pack, nil
	}

	tx := s.db.Where("name = ?", name).
		Preload("Items.PluginMetadata").
		Preload("PriceDetails").
		First(&pack)

	if tx.Error != nil {
		return nil, fmt.Errorf("failed to find plugin pack with name: %s", name)
	}

	if err := s.redisCache.Set(key, &pack, s.cacheDuration); err != nil {
		s.log.Errorf("error caching plugin pack %s: %v", name, err)
	}

	return &pack, nil
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
