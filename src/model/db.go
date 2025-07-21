package model

import (
	"errors"
	"fmt"
	"go.uber.org/zap"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"kraken-api/src/cache"
	"os"
	"time"
)

func Connect(log *zap.SugaredLogger) *gorm.DB {
	db, err := gorm.Open(mysql.New(mysql.Config{
		DSN:               fmt.Sprintf("%s:%s@tcp(%s:3306)/kraken?charset=utf8mb4&parseTime=True&loc=Local", os.Getenv("MYSQL_USER"), os.Getenv("MYSQL_PASSWORD"), os.Getenv("MYSQL_HOST")),
		DefaultStringSize: 256,
	}), &gorm.Config{
		DisableForeignKeyConstraintWhenMigrating: true,
	})

	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	err = db.AutoMigrate(
		&User{},
		&CognitoCredentials{},
		&Plugin{},
		&HardwareID{},
		&PluginMetadata{
			log: log,
		},
		&PluginVersion{},
		&PluginConfig{},
		&PluginPackPriceDetails{},
		&PluginMetadataPriceDetails{},
		&PluginPack{},
		&PluginPackItem{},
		&UserPluginPack{},
		&PluginSale{},
		&PluginSaleItem{},
	)

	if err != nil {
		log.Fatalf("failed to run database migrations: %v", err)
	}
	return db
}

// User represents a user of the system
type User struct {
	ID                 uint           `gorm:"primaryKey" json:"id"`
	DiscordUsername    string         `gorm:"column:discord_username" json:"discordUsername,omitempty"`
	Email              string         `gorm:"column:email" json:"email,omitempty"`
	AvatarId           string         `gorm:"column:avatar_id" json:"avatarId"`
	Tokens             int64          `gorm:"column:tokens;default:0" json:"tokens"`
	DiscordID          string         `gorm:"column:discord_id;uniqueIndex" json:"discordId,omitempty"`
	CustomerId         string         `gorm:"column:customer_id;uniqueIndex" json:"customerId,omitempty"`
	UsedFreeTrial      bool           `gorm:"column:used_free_trial" json:"usedFreeTrial"`
	FreeTrialStartTime time.Time      `gorm:"column:free_trial_start_time" json:"freeTrialStartTime"`
	FreeTrialEndTime   time.Time      `gorm:"column:free_trial_end_time" json:"freeTrialEndTime"`
	JagexCharacterId   string         `gorm:"column:jagex_character_id" json:"jagexCharacterId"`
	JagexSessionId     string         `gorm:"column:jagex_session_id" json:"jagexSessionId"`
	JagexDisplayName   string         `gorm:"column:jagex_display_name" json:"jagexDisplayName"`
	CreatedAt          time.Time      `json:"createdAt"`
	UpdatedAt          time.Time      `json:"updatedAt"`
	DeletedAt          gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	// Relations
	Credentials CognitoCredentials `gorm:"foreignKey:UserID" json:"credentials,omitempty"`
	Plugins     []Plugin           `gorm:"foreignKey:UserID" json:"plugins"`
	HardwareIDs []HardwareID       `gorm:"foreignKey:UserID" json:"hardwareIds"`
	PluginPacks []UserPluginPack   `gorm:"foreignKey:UserID" json:"pluginPacks"`
}

// UserRepository This interface and implementing struct are required for mocking during unit tests.
type UserRepository interface {
	GetUser(discordId string, db *gorm.DB) (*User, error)
}

type DefaultUserRepository struct {
	Cache *cache.RedisCache
	Log   *zap.SugaredLogger
}

// GetUser Retrieves a user and associated plugin metadata from the database.
func (r *DefaultUserRepository) GetUser(discordId string, db *gorm.DB) (*User, error) {
	cacheKey := fmt.Sprintf("user:discord:%s", discordId)
	var user User

	if err := r.Cache.Get(cacheKey, &user); err == nil {
		r.Log.Infof("cache hit for user: %s", discordId)
		return &user, nil
	}

	tx := db.
		Preload("HardwareIDs").
		Preload("Plugins").
		Preload("PluginPacks").
		Preload("Credentials").
		Where("discord_id = ?", discordId).
		First(&user)

	if tx.Error != nil {
		return nil, tx.Error
	}

	if err := r.Cache.Set(cacheKey, &user, 15*time.Minute); err != nil {
		r.Log.Infof("failed to cache user: %v", err)
	}

	return &user, nil
}

// PluginPack represents a collection of plugins sold together
type PluginPack struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	Name        string         `gorm:"column:name;not null" json:"name"`
	Title       string         `json:"title"`
	Description string         `json:"description"`
	ImageUrl    string         `json:"imageUrl"`
	Discount    float32        `gorm:"column:discount;default:0" json:"discount"` // Percentage discount when buying the pack
	Active      bool           `gorm:"column:active;default:true" json:"active"`
	CreatedAt   time.Time      `json:"createdAt"`
	UpdatedAt   time.Time      `json:"updatedAt"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	// Relations
	Items        []PluginPackItem       `gorm:"foreignKey:PackID" json:"items"`
	PriceDetails PluginPackPriceDetails `gorm:"foreignKey:PluginPackID" json:"priceDetails"`
}

// PluginPackItem represents a plugin that belongs to a pack
type PluginPackItem struct {
	ID               uint           `gorm:"primaryKey" json:"id"`
	PackID           uint           `gorm:"column:pack_id;index" json:"packId"`
	PluginMetadataID uint           `gorm:"column:plugin_metadata_id;index" json:"pluginMetadataId"`
	CreatedAt        time.Time      `json:"createdAt"`
	UpdatedAt        time.Time      `json:"updatedAt"`
	DeletedAt        gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	// Relations
	PluginMetadata PluginMetadata `gorm:"foreignKey:PluginMetadataID" json:"pluginMetadata"`
}

// UserPluginPack represents a plugin pack purchased by a user
type UserPluginPack struct {
	ID                  uint           `gorm:"primaryKey" json:"id"`
	UserID              uint           `gorm:"column:user_id;index" json:"userId"`
	PluginPackID        uint           `gorm:"column:plugin_pack_id;index" json:"pluginPackId"`
	ExpirationTimestamp time.Time      `gorm:"column:expiration_timestamp" json:"expirationTimestamp"`
	LicenseKey          string         `gorm:"column:license_key;uniqueIndex" json:"licenseKey"`
	CreatedAt           time.Time      `json:"createdAt"`
	UpdatedAt           time.Time      `json:"updatedAt"`
	DeletedAt           gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	// Relations
	User       User       `gorm:"foreignKey:UserID" json:"-"`
	PluginPack PluginPack `gorm:"foreignKey:PluginPackID" json:"pluginPack"`
}

type PluginMetadataPriceDetails struct {
	ID         uint `gorm:"primaryKey" json:"id"`
	Month      int  `json:"month"`
	ThreeMonth int  `json:"threeMonth"`
	Year       int  `json:"year"`

	// In a JSON serialized response additional metadata about the sale price for a plugin can be included optionally in the response.
	// If a sale is not happening for a plugin these fields can be safely ignored and will not be returned in the response.
	SaleMonth        int            `gorm:"-" json:"saleMonth,omitempty"`
	SaleThreeMonth   int            `gorm:"-" json:"saleThreeMonth,omitempty"`
	SaleYear         int            `gorm:"-" json:"saleYear,omitempty"`
	PluginMetadataID uint           `gorm:"column:plugin_metadata_id;index;not null" json:"pluginMetadataId"`
	CreatedAt        time.Time      `json:"createdAt"`
	UpdatedAt        time.Time      `json:"updatedAt"`
	DeletedAt        gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`
}

func (p PluginMetadataPriceDetails) TableName() string {
	return "plugin_metadata_price_details"
}

// PluginPackPriceDetails specifically for pack pricing
type PluginPackPriceDetails struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	Month        int            `json:"month"`
	ThreeMonth   int            `json:"threeMonth"`
	Year         int            `json:"year"`
	PluginPackID uint           `gorm:"column:plugin_pack_id;index;not null" json:"pluginPackId"`
	CreatedAt    time.Time      `json:"createdAt"`
	UpdatedAt    time.Time      `json:"updatedAt"`
	DeletedAt    gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`
}

func (p PluginPackPriceDetails) TableName() string {
	return "plugin_pack_price_details"
}

func (u User) InFreeTrialPeriod() bool {
	return u.UsedFreeTrial == true && time.Now().After(u.FreeTrialStartTime) && time.Now().Before(u.FreeTrialEndTime)
}

func (User) TableName() string {
	return "users"
}

type CognitoCredentials struct {
	ID              uint           `gorm:"primaryKey" json:"id"`
	UserID          uint           `gorm:"column:user_id;index" json:"userId"`
	RefreshToken    string         `gorm:"column:refresh_token;type:LONGTEXT" json:"refreshToken,omitempty"`
	TokenExpiration int32          `gorm:"column:token_expiration" json:"tokenExpirationSeconds,omitempty"`
	AccessToken     string         `gorm:"column:access_token;type:LONGTEXT" json:"accessToken,omitempty"`
	IdToken         string         `gorm:"column:id_token;type:LONGTEXT" json:"idToken,omitempty"`
	CreatedAt       time.Time      `json:"createdAt"`
	UpdatedAt       time.Time      `json:"updatedAt"`
	DeletedAt       gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`
}

// HardwareID represents a hardware identifier associated with a user
type HardwareID struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	Value     string         `gorm:"uniqueIndex;not null"`
	UserID    uint           `gorm:"column:user_id;index" json:"userId"`
	CreatedAt time.Time      `json:"createdAt"`
	UpdatedAt time.Time      `json:"updatedAt"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`
}

func (HardwareID) TableName() string {
	return "hardware_ids"
}

// Plugin represents a software plugin in the system. We do not track the latest version of a plugin in this table because
// maintaining state between MinIO and the database is not worth the complexity. Instead, we rely on the MinIO service to provide all versions
// and the latest version of a plugin when requested.
type Plugin struct {
	ID                  uint           `gorm:"primaryKey" json:"id"`
	UserID              uint           `gorm:"column:user_id;index" json:"userId"`
	Name                string         `gorm:"column:name;index;not null" json:"name"`
	ExpirationTimestamp time.Time      `gorm:"column:expiration_timestamp" json:"expirationTimestamp"`
	LicenseKey          string         `gorm:"column:license_key;uniqueIndex" json:"licenseKey"`
	TrialPlugin         bool           `gorm:"column:trial_plugin" json:"trialPlugin"`
	CreatedAt           time.Time      `json:"createdAt"`
	UpdatedAt           time.Time      `json:"updatedAt"`
	DeletedAt           gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	User User `gorm:"foreignKey:UserID" json:"-"`
}

func (Plugin) TableName() string {
	return "plugins"
}

type PluginMetadata struct {
	ID                   uint                       `gorm:"primaryKey" json:"id"`
	Name                 string                     `gorm:"uniqueIndex" json:"name"`
	Title                string                     `json:"title"`
	Description          string                     `json:"description"`
	ImageUrl             string                     `json:"imageUrl"`
	VideoUrl             string                     `json:"videoUrl"`
	TopPick              bool                       `json:"topPick"`
	IsInBeta             bool                       `json:"isInBeta"`
	Versions             []PluginVersion            `gorm:"foreignKey:PluginMetadataID" json:"versions"`
	SaleDiscount         float32                    `gorm:"-" json:"saleDiscount"` // Sale discounts are pulled from the db but included only in API responses not on actual rows in db.
	ConfigurationOptions []PluginConfig             `gorm:"foreignKey:PluginMetadataID" json:"configurationOptions"`
	PriceDetails         PluginMetadataPriceDetails `gorm:"foreignKey:PluginMetadataID" json:"priceDetails"`
	Tier                 int                        `json:"tier"`
	LatestVersion        string                     `gorm:"-" json:"latestVersion"`
	log                  *zap.SugaredLogger
}

type PluginVersion struct {
	ID               uint   `gorm:"primaryKey" json:"id"`
	PluginMetadataID uint   `gorm:"column:plugin_metadata_id" json:"-"`
	Version          string `json:"version"`
	Latest           bool   `gorm:"column:latest;default:false" json:"latest"`
}

type PluginConfig struct {
	ID               uint     `gorm:"primaryKey" json:"id"`
	Name             string   `json:"name"`
	Section          string   `json:"section"`
	Description      string   `json:"description"`
	Type             string   `json:"type"`
	IsBool           bool     `json:"isBool"`
	Values           string   `gorm:"type:text" json:"-"`
	ValuesSlice      []string `gorm:"-" json:"values"`
	PluginMetadataID uint     `json:"pluginMetadataId"`
}

type PluginSale struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	Name        string         `gorm:"column:name;not null" json:"name"`            // e.g., "Black Friday Sale", "Summer Special"
	Description string         `gorm:"column:description" json:"description"`       // Optional description
	Discount    float32        `gorm:"column:discount;not null" json:"discount"`    // Percentage discount (0-100)
	StartTime   time.Time      `gorm:"column:start_time;not null" json:"startTime"` // When sale begins
	EndTime     time.Time      `gorm:"column:end_time;not null" json:"endTime"`     // When sale ends
	Active      bool           `gorm:"column:active;default:true" json:"active"`    // Admin can manually disable
	CreatedAt   time.Time      `json:"createdAt"`
	UpdatedAt   time.Time      `json:"updatedAt"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	SaleItems []PluginSaleItem `gorm:"foreignKey:SaleID" json:"-"`

	// This is included so this struct can be used as a request body when creating new sales. This field has no bearing on the
	// plugin/sale association in the db.
	PluginNames []string `gorm:"-" json:"pluginNames,omitempty"`
}

// PluginSaleItem represents which plugins are included in a sale
type PluginSaleItem struct {
	ID               uint           `gorm:"primaryKey" json:"id"`
	SaleID           uint           `gorm:"column:sale_id;index;not null" json:"saleId"`
	PluginMetadataID uint           `gorm:"column:plugin_metadata_id;index;not null" json:"pluginMetadataId"`
	CreatedAt        time.Time      `json:"createdAt"`
	UpdatedAt        time.Time      `json:"updatedAt"`
	DeletedAt        gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	Sale           PluginSale     `gorm:"foreignKey:SaleID" json:"sale"`
	PluginMetadata PluginMetadata `gorm:"foreignKey:PluginMetadataID" json:"pluginMetadata"`
}

// IsCurrentlyActive Returns true when a sale is currently active and false otherwise
func (ps *PluginSale) IsCurrentlyActive() bool {
	now := time.Now()
	return ps.Active && now.After(ps.StartTime) && now.Before(ps.EndTime)
}

// GetCurrentSaleDiscount returns a percentage discount of a sale for a given plugin
func (pm *PluginMetadata) GetCurrentSaleDiscount(db *gorm.DB, cache *cache.RedisCache) (float32, error) {
	cacheKey := fmt.Sprintf("plugin:%d:current_sale_discount", pm.ID)

	var sale PluginSale

	if err := cache.Get(cacheKey, &sale); err == nil {
		pm.log.Infof("cache hit on get current sale discount for plugin %d", pm.ID)
		if sale.IsCurrentlyActive() {
			return sale.Discount, nil
		}
	}

	now := time.Now()

	result := db.Joins("JOIN plugin_sale_items ON plugin_sales.id = plugin_sale_items.sale_id").
		Where("plugin_sale_items.plugin_metadata_id = ? AND plugin_sales.active = true AND plugin_sales.start_time <= ? AND plugin_sales.end_time >= ?",
							pm.ID, now, now).
		Order("plugin_sales.discount DESC"). // Get highest discount if multiple sales apply
		First(&sale)

	if result.Error != nil {
		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			return 0, nil
		}
		return 0, result.Error
	}

	if sale.IsCurrentlyActive() {
		if err := cache.Set(cacheKey, &sale, 5*time.Minute); err != nil {
			pm.log.Errorf("failed to cache current sale discount for plugin %d: %v", pm.ID, err)
		}
		return sale.Discount, nil
	}

	return 0, nil
}

// GetActiveSalesLookup calculates all active sales as a lookup map
func GetActiveSalesLookup(db *gorm.DB, cache *cache.RedisCache) (map[uint]float32, error) {
	cacheKey := "sales:active:lookup"

	lookup := make(map[uint]float32)
	if err := cache.Get(cacheKey, &lookup); err == nil {
		return lookup, nil
	}

	now := time.Now()

	// Single query to get all active sales with their plugin associations
	var results []struct {
		PluginMetadataID uint    `json:"plugin_metadata_id"`
		Discount         float32 `json:"discount"`
	}

	err := db.Table("plugin_sales").
		Select("plugin_sale_items.plugin_metadata_id, MAX(plugin_sales.discount) as discount").
		Joins("JOIN plugin_sale_items ON plugin_sales.id = plugin_sale_items.sale_id").
		Where("plugin_sales.active = true AND plugin_sales.start_time <= ? AND plugin_sales.end_time >= ?", now, now).
		Group("plugin_sale_items.plugin_metadata_id").
		Scan(&results).Error

	if err != nil {
		return nil, err
	}

	// Build the lookup map
	for _, result := range results {
		lookup[result.PluginMetadataID] = result.Discount
	}

	cache.Set(cacheKey, lookup, 5*time.Minute)
	return lookup, nil
}
