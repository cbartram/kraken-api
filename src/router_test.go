package src

import (
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"kraken-api/src/service"
	"testing"
)

func Test_NewRouter(t *testing.T) {
	log, _ := zap.NewDevelopment()
	w := &service.Wrapper{
		Logger:          log.Sugar(),
		CognitoService:  &service.CognitoService{},
		S3Service:       &service.MinIOService{},
		DiscordService:  &service.DiscordService{},
		Database:        &gorm.DB{},
		RabbitMqService: &service.RabbitMqService{},
		PluginStore:     &service.PluginStore{},
	}
	r := NewRouter(w)
	assert.NotNil(t, r)
}
