package service

import (
	"gorm.io/gorm"
)

type Wrapper struct {
	CognitoService  *CognitoService
	S3Service       *MinIOService
	DiscordService  *DiscordService
	Database        *gorm.DB
	RabbitMqService *RabbitMqService
	PluginStore     *PluginStore
}
