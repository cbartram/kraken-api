package model

import (
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
	DiscordID       string         `gorm:"column:discord_id;uniqueIndex" json:"discordId,omitempty"`
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
	S3JarFilePath       string         `json:"s3JarFilePath"`
	LicenseKey          string         `gorm:"uniqueIndex" json:"-"`
	CreatedAt           time.Time      `json:"createdAt"`
	UpdatedAt           time.Time      `json:"updatedAt"`
	DeletedAt           gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`

	User User `gorm:"foreignKey:UserID" json:"-"`
}

func (Plugin) TableName() string {
	return "plugins"
}
