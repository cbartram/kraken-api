package model

import (
	"encoding/json"
	"errors"
	"fmt"
	log "github.com/sirupsen/logrus"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"os"
	"time"
)

func Connect() *gorm.DB {
	db, err := gorm.Open(mysql.New(mysql.Config{
		DSN:               fmt.Sprintf("%s:%s@tcp(%s:3306)/kraken?charset=utf8mb4&parseTime=True&loc=Local", os.Getenv("MYSQL_USER"), os.Getenv("MYSQL_PASSWORD"), os.Getenv("MYSQL_HOST")),
		DefaultStringSize: 256,
	}), &gorm.Config{
		DisableForeignKeyConstraintWhenMigrating: true,
	})

	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	log.Infof("migrating database")
	err = db.AutoMigrate(
		&User{},
		&CognitoCredentials{},
		&Plugin{},
		&HardwareID{},
		&PluginMetadata{},
		&PluginConfig{},
		&PluginPackPriceDetails{},
		&PluginMetadataPriceDetails{},
		&PluginPack{},
		&PluginPackItem{},
		&UserPluginPack{},
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

func GetUser(discordId string, db *gorm.DB) (*User, error) {
	var user User
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
	ID               uint           `gorm:"primaryKey" json:"id"`
	Month            int            `json:"month"`
	ThreeMonth       int            `json:"threeMonth"`
	Year             int            `json:"year"`
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

// Plugin represents a software plugin in the system
type Plugin struct {
	ID                  uint           `gorm:"primaryKey" json:"id"`
	UserID              uint           `gorm:"column:user_id;index" json:"userId"`
	Name                string         `gorm:"column:name;index;not null" json:"name"`
	ExpirationTimestamp time.Time      `gorm:"column:expiration_timestamp" json:"expirationTimestamp"`
	S3JarFilePath       string         `gorm:"column:s3_jar_file_path" json:"s3JarFilePath"`
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
	Version              string                     `gorm:"-" json:"version"`
	ConfigurationOptions []PluginConfig             `gorm:"foreignKey:PluginMetadataID" json:"configurationOptions"` // One-to-many relationship
	PriceDetails         PluginMetadataPriceDetails `gorm:"foreignKey:PluginMetadataID" json:"priceDetails"`         // One-to-one relationship
	Tier                 int                        `json:"tier"`
}

type PluginConfig struct {
	ID               uint     `gorm:"primaryKey" json:"id"`
	Name             string   `json:"name"`
	Section          string   `json:"section"`
	Description      string   `json:"description"`
	Type             string   `json:"type"`
	IsBool           bool     `json:"isBool"`
	Values           string   `gorm:"type:text" json:"-"` // Store as JSON string in DB
	ValuesSlice      []string `gorm:"-" json:"values"`    // For JSON serialization
	PluginMetadataID uint     `json:"pluginMetadataId"`   // Foreign key reference
}

func ImportOrUpdatePluginMetadata(jsonFilePath string, db *gorm.DB) error {
	jsonData, err := os.ReadFile(jsonFilePath)
	if err != nil {
		return fmt.Errorf("failed to read JSON file: %w", err)
	}

	var pluginMetadataList []PluginMetadata
	if err := json.Unmarshal(jsonData, &pluginMetadataList); err != nil {
		return fmt.Errorf("failed to unmarshal JSON data: %w", err)
	}

	tx := db.Begin()
	if tx.Error != nil {
		return fmt.Errorf("failed to begin transaction: %w", tx.Error)
	}

	for i := range pluginMetadataList {
		plugin := &pluginMetadataList[i]

		// Check if the plugin already exists by Name (which should be unique)
		var existingPlugin PluginMetadata
		log.Debugf("finding plugin: %s", plugin.Name)
		result := tx.Where("name = ?", plugin.Name).First(&existingPlugin)

		if result.Error == nil {
			log.Debugf("plugin already exists: %s", plugin.Name)
			continue
		}
		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			// Plugin doesn't exist, create it
			priceDetails := plugin.PriceDetails
			configOptions := plugin.ConfigurationOptions
			plugin.ConfigurationOptions = nil

			if err := tx.Create(plugin).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to create plugin metadata: %w", err)
			}

			priceDetails.PluginMetadataID = plugin.ID
			if err := tx.Create(&priceDetails).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to create price details: %w", err)
			}

			for j := range configOptions {
				if len(configOptions[j].ValuesSlice) > 0 {
					valuesData, err := json.Marshal(configOptions[j].ValuesSlice)
					if err != nil {
						tx.Rollback()
						return fmt.Errorf("failed to marshal values: %w", err)
					}
					configOptions[j].Values = string(valuesData)
				}

				configOptions[j].PluginMetadataID = plugin.ID
				if err := tx.Create(&configOptions[j]).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to create config option: %w", err)
				}
			}
		} else {
			tx.Rollback()
			return fmt.Errorf("error checking for existing plugin: %v", result.Error)
		}
	}

	if err := tx.Commit().Error; err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}

func ImportOrUpdatePluginPacks(jsonFilePath string, db *gorm.DB) error {
	// Read the JSON file
	jsonData, err := os.ReadFile(jsonFilePath)
	if err != nil {
		return fmt.Errorf("failed to read JSON file: %w", err)
	}

	// Define a struct to match the JSON structure
	type PluginPackInput struct {
		Name         string                 `json:"name"`
		Title        string                 `json:"title"`
		Description  string                 `json:"description"`
		ImageUrl     string                 `json:"imageUrl"`
		Discount     float32                `json:"discount"`
		Active       bool                   `json:"active"`
		Plugins      []string               `json:"plugins"`
		PriceDetails PluginPackPriceDetails `json:"priceDetails"`
	}

	// Unmarshal the JSON data
	var pluginPackInputs []PluginPackInput
	if err := json.Unmarshal(jsonData, &pluginPackInputs); err != nil {
		return fmt.Errorf("failed to unmarshal JSON data: %w", err)
	}

	// Begin a transaction
	tx := db.Begin()
	if tx.Error != nil {
		return fmt.Errorf("failed to begin transaction: %w", tx.Error)
	}

	for _, packInput := range pluginPackInputs {
		// Check if the pack already exists
		var existingPack PluginPack
		result := tx.Where("name = ?", packInput.Name).First(&existingPack)

		if result.Error == nil {
			log.Debugf("pack already exists: %s", packInput.Name)
			continue
		}

		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			// Create new pack
			pack := PluginPack{
				Name:        packInput.Name,
				Title:       packInput.Title,
				Description: packInput.Description,
				ImageUrl:    packInput.ImageUrl,
				Discount:    packInput.Discount,
				Active:      packInput.Active,
			}

			// Create the plugin pack
			if err := tx.Create(&pack).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to create plugin pack: %w", err)
			}

			// Create the price details - explicitly set PluginMetadataID to zero/NULL
			priceDetails := PluginPackPriceDetails{
				Month:        packInput.PriceDetails.Month,
				ThreeMonth:   packInput.PriceDetails.ThreeMonth,
				Year:         packInput.PriceDetails.Year,
				PluginPackID: pack.ID,
			}

			// Use SQL that doesn't include PluginMetadataID in the INSERT statement
			if err := tx.Omit("PluginMetadataID").Create(&priceDetails).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to create price details: %w", err)
			}

			// Link the plugins to the pack
			for _, pluginName := range packInput.Plugins {
				// Find the plugin metadata by name
				var pluginMetadata PluginMetadata
				if err := tx.Where("name = ?", pluginName).First(&pluginMetadata).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to find plugin metadata '%s': %w", pluginName, err)
				}

				// Create the pack item
				packItem := PluginPackItem{
					PackID:           pack.ID,
					PluginMetadataID: pluginMetadata.ID,
				}
				if err := tx.Create(&packItem).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to create plugin pack item: %w", err)
				}
			}
		} else if result.Error != nil {
			tx.Rollback()
			return fmt.Errorf("error checking for existing plugin pack: %w", result.Error)
		} else {
			// Pack exists, update it
			existingPack.Title = packInput.Title
			existingPack.Description = packInput.Description
			existingPack.ImageUrl = packInput.ImageUrl
			existingPack.Discount = packInput.Discount
			existingPack.Active = packInput.Active

			if err := tx.Save(&existingPack).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to update plugin pack: %w", err)
			}

			// Update price details - make sure we're not updating the PluginMetadataID
			if err := tx.Model(&PluginPackPriceDetails{}).
				Where("plugin_pack_id = ?", existingPack.ID).
				Updates(map[string]interface{}{
					"month":       packInput.PriceDetails.Month,
					"three_month": packInput.PriceDetails.ThreeMonth,
					"year":        packInput.PriceDetails.Year,
				}).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to update price details: %w", err)
			}

			// Delete existing pack items
			if err := tx.Where("pack_id = ?", existingPack.ID).Delete(&PluginPackItem{}).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to delete existing plugin pack items: %w", err)
			}

			// Re-add the plugins
			for _, pluginName := range packInput.Plugins {
				// Find the plugin metadata by name
				var pluginMetadata PluginMetadata
				if err := tx.Where("name = ?", pluginName).First(&pluginMetadata).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to find plugin metadata '%s': %w", pluginName, err)
				}

				// Create the pack item
				packItem := PluginPackItem{
					PackID:           existingPack.ID,
					PluginMetadataID: pluginMetadata.ID,
				}
				if err := tx.Create(&packItem).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to create plugin pack item: %w", err)
				}
			}
		}
	}

	// Commit the transaction
	if err := tx.Commit().Error; err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}
