package service

import (
	"go.uber.org/zap"
	"gorm.io/gorm"
)

type Wrapper struct {
	Logger          *zap.SugaredLogger
	CognitoService  *CognitoService
	S3Service       *MinIOService
	DiscordService  *DiscordService
	Database        *gorm.DB
	RabbitMqService *RabbitMqService
	PluginStore     *PluginStore
}
