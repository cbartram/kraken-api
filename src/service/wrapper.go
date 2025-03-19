package service

import (
	"gorm.io/gorm"
)

type Wrapper struct {
	CognitoService  *CognitoService
	S3Service       *S3Service
	DiscordService  *DiscordService
	Database        *gorm.DB
	RabbitMqService *RabbitMqService
}
