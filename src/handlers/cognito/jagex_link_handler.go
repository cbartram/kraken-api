package cognito

import (
	"encoding/json"
	"errors"
	"io"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type JagexLinkHandler struct{}

type JagexLink struct {
	JagexCharacterId string `json:"jagexCharacterId"`
	JagexSessionId   string `json:"jagexSessionId"`
	JagexDisplayName string `json:"jagexDisplayName"`
	Ip               string `json:"ip"`
}

func (j *JagexLinkHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody JagexLink
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	// Validate required fields
	if reqBody.JagexCharacterId == "" || reqBody.JagexDisplayName == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "jagex character id and display name are required"})
		return
	}

	user, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not found in context"})
		return
	}

	userModel, ok := user.(*model.User)
	if !ok {
		log.Errorf("failed to cast user to User model")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "internal server error"})
		return
	}

	// Try to find existing character
	var character model.Character
	err = w.Database.Where("user_id = ? AND jagex_character_id = ?", userModel.ID, reqBody.JagexCharacterId).First(&character).Error

	now := time.Now()

	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			// Character doesn't exist, create new one
			log.Infof("creating new character for user %d with jagex character id %s and display name %s", userModel.ID, reqBody.JagexCharacterId, reqBody.JagexDisplayName)

			newCharacter := model.Character{
				UserID:              userModel.ID,
				JagexCharacterId:    reqBody.JagexCharacterId,
				JagexSessionId:      reqBody.JagexSessionId,
				JagexDisplayName:    reqBody.JagexDisplayName,
				LastClientLoginTime: now,
			}

			if err := w.Database.Create(&newCharacter).Error; err != nil {
				log.Errorf("failed to create character: %s", err)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create character: " + err.Error()})
				return
			}

			log.Infof("successfully created character: %s for user: %s", newCharacter.JagexDisplayName, userModel.DiscordUsername)
			c.JSON(http.StatusCreated, gin.H{
				"message":     "character created successfully",
				"characterId": newCharacter.ID,
			})
			return
		} else {
			log.Errorf("failed to query character: %s", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to query character: " + err.Error()})
			return
		}
	}

	// Character exists, update it
	log.Infof("updating existing character: %s for user: %s with jagex character id %s", character.JagexDisplayName, userModel.DiscordUsername, reqBody.JagexCharacterId)

	character.JagexSessionId = reqBody.JagexSessionId
	character.JagexDisplayName = reqBody.JagexDisplayName
	character.LastClientLoginTime = now

	if err := w.Database.Save(&character).Error; err != nil {
		log.Errorf("failed to update character: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to update character: " + err.Error()})
		return
	}

	log.Infof("successfully updated character: %s for user: %s", character.JagexDisplayName, userModel.DiscordUsername)
	c.JSON(http.StatusOK, gin.H{
		"message":     "character updated successfully",
		"characterId": character.ID,
	})
}
