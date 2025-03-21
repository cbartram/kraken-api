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
	}), &gorm.Config{})

	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	err = db.AutoMigrate(
		&User{},
		&CognitoCredentials{},
		&Plugin{},
		&HardwareID{},
		&PluginMetadata{},
		&PluginConfig{},
		&PriceDetails{},
	)

	if err != nil {
		log.Fatalf("failed to run database migrations: %v", err)
	}

	return db
}

// User represents a user of the system
type User struct {
	ID              uint           `gorm:"primaryKey" json:"id"`
	DiscordUsername string         `gorm:"column:discord_username" json:"discordUsername,omitempty"`
	Email           string         `gorm:"column:email" json:"email,omitempty"`
	AvatarId        string         `gorm:"column:avatar_id" json:"avatarId"`
	Tokens          int64          `gorm:"column:tokens;default:0" json:"tokens"`
	DiscordID       string         `gorm:"column:discord_id;uniqueIndex" json:"discordId,omitempty"`
	CustomerId      string         `gorm:"column:customer_id;uniqueIndex" json:"customerId,omitempty"`
	CreatedAt       time.Time      `json:"createdAt"`
	UpdatedAt       time.Time      `json:"updatedAt"`
	DeletedAt       gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	// Relations
	Credentials CognitoCredentials `gorm:"foreignKey:UserID" json:"credentials,omitempty"`
	Plugins     []Plugin           `gorm:"foreignKey:UserID" json:"plugins"`
	HardwareIDs []HardwareID       `gorm:"foreignKey:UserID" json:"hardwareIds"`
}

func GetUser(discordId string, db *gorm.DB) (*User, error) {
	var user User
	tx := db.
		Preload("HardwareIDs").
		Preload("Plugins").
		Preload("Credentials").
		Where("discord_id = ?", discordId).
		First(&user)

	if tx.Error != nil {
		return nil, tx.Error
	}

	return &user, nil
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
	CreatedAt           time.Time      `json:"createdAt"`
	UpdatedAt           time.Time      `json:"updatedAt"`
	DeletedAt           gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	User User `gorm:"foreignKey:UserID" json:"-"`
}

func (Plugin) TableName() string {
	return "plugins"
}

type PriceDetails struct {
	ID               uint `gorm:"primaryKey" json:"id"`
	Month            int  `json:"month"`
	ThreeMonth       int  `json:"threeMonth"`
	Year             int  `json:"year"`
	PluginMetadataID uint `json:"pluginMetadataId"`
}

type PluginMetadata struct {
	ID                   uint           `gorm:"primaryKey" json:"id"`
	Name                 string         `gorm:"uniqueIndex" json:"name"`
	Title                string         `json:"title"`
	Description          string         `json:"description"`
	ImageUrl             string         `json:"imageUrl"`
	VideoUrl             string         `json:"videoUrl"`
	TopPick              bool           `json:"topPick"`
	ConfigurationOptions []PluginConfig `gorm:"foreignKey:PluginMetadataID" json:"configurationOptions"` // One-to-many relationship
	PriceDetails         PriceDetails   `gorm:"foreignKey:PluginMetadataID" json:"priceDetails"`         // One-to-one relationship
	Tier                 int            `json:"tier"`
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

	// Parse the JSON into a slice of your structs
	var pluginMetadataList []PluginMetadata
	if err := json.Unmarshal(jsonData, &pluginMetadataList); err != nil {
		return fmt.Errorf("failed to unmarshal JSON data: %w", err)
	}

	// Begin a transaction
	tx := db.Begin()
	if tx.Error != nil {
		return fmt.Errorf("failed to begin transaction: %w", tx.Error)
	}

	for i := range pluginMetadataList {
		plugin := &pluginMetadataList[i]

		// Check if the plugin already exists by Name (which should be unique)
		var existingPlugin PluginMetadata
		result := tx.Where("name = ?", plugin.Name).First(&existingPlugin)

		if result.Error == nil {
			// Plugin exists, update it
			plugin.ID = existingPlugin.ID // Keep the same ID

			// Store references for relationship handling
			priceDetails := plugin.PriceDetails
			configOptions := plugin.ConfigurationOptions
			plugin.ConfigurationOptions = nil

			// Update the main plugin metadata
			if err := tx.Model(&existingPlugin).Updates(map[string]interface{}{
				"title":       plugin.Title,
				"description": plugin.Description,
				"image_url":   plugin.ImageUrl,
				"video_url":   plugin.VideoUrl,
				"top_pick":    plugin.TopPick,
				"tier":        plugin.Tier,
			}).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to update plugin metadata: %w", err)
			}

			// Handle PriceDetails (update or create)
			var existingPriceDetails PriceDetails
			if err := tx.Where("plugin_metadata_id = ?", existingPlugin.ID).First(&existingPriceDetails).Error; err != nil {
				if errors.Is(err, gorm.ErrRecordNotFound) {
					// Create new price details if not exists
					priceDetails.PluginMetadataID = existingPlugin.ID
					if err := tx.Create(&priceDetails).Error; err != nil {
						tx.Rollback()
						return fmt.Errorf("failed to create price details: %w", err)
					}
				} else {
					tx.Rollback()
					return fmt.Errorf("failed to query price details: %w", err)
				}
			} else {
				// Update existing price details
				if err := tx.Model(&existingPriceDetails).Updates(map[string]interface{}{
					"month":       priceDetails.Month,
					"three_month": priceDetails.ThreeMonth,
					"year":        priceDetails.Year,
				}).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to update price details: %w", err)
				}
			}

			// Handle ConfigurationOptions (more complex as it's one-to-many)
			// First, get all existing config options
			var existingConfigOptions []PluginConfig
			if err := tx.Where("plugin_metadata_id = ?", existingPlugin.ID).Find(&existingConfigOptions).Error; err != nil {
				tx.Rollback()
				return fmt.Errorf("failed to query existing config options: %w", err)
			}

			// Create a map for easier lookup
			existingConfigMap := make(map[string]PluginConfig)
			for _, config := range existingConfigOptions {
				existingConfigMap[config.Name] = config
			}

			// Process each config option
			for j := range configOptions {
				// Handle the Values slice to string conversion
				if len(configOptions[j].ValuesSlice) > 0 {
					valuesData, err := json.Marshal(configOptions[j].ValuesSlice)
					if err != nil {
						tx.Rollback()
						return fmt.Errorf("failed to marshal values: %w", err)
					}
					configOptions[j].Values = string(valuesData)
				}

				configOptions[j].PluginMetadataID = existingPlugin.ID

				// Check if this config exists
				if existingConfig, exists := existingConfigMap[configOptions[j].Name]; exists {
					// Update existing config
					if err := tx.Model(&existingConfig).Updates(map[string]interface{}{
						"section":     configOptions[j].Section,
						"description": configOptions[j].Description,
						"type":        configOptions[j].Type,
						"is_bool":     configOptions[j].IsBool,
						"values":      configOptions[j].Values,
					}).Error; err != nil {
						tx.Rollback()
						return fmt.Errorf("failed to update config option: %w", err)
					}

					// Remove from map to track which ones we've processed
					delete(existingConfigMap, configOptions[j].Name)
				} else {
					// Create new config option
					if err := tx.Create(&configOptions[j]).Error; err != nil {
						tx.Rollback()
						return fmt.Errorf("failed to create config option: %w", err)
					}
				}
			}

			for _, remainingConfig := range existingConfigMap {
				if err := tx.Delete(&remainingConfig).Error; err != nil {
					tx.Rollback()
					return fmt.Errorf("failed to delete obsolete config option: %w", err)
				}
			}

		} else if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			// Plugin doesn't exist, create it (similar to original function)
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
			// Some other error occurred
			tx.Rollback()
			return fmt.Errorf("error checking for existing plugin: %w", result.Error)
		}
	}

	// Commit the transaction
	if err := tx.Commit().Error; err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}
