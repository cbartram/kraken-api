package sale

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type CreateSaleHandler struct{}

func (cr *CreateSaleHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	log.Infof("user: %#v", user)
	if user.DiscordUsername != "runewraith" || user.DiscordID != "215299779352068097" {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not sale"})
		return
	}

	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.PluginSale
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	if reqBody.Discount <= 0 || reqBody.Discount >= 100 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "sales must be at least 1% and no more than 99% off. enter integer between 1 and 99"})
		return
	}

	if len(reqBody.PluginNames) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "no plugin names provided"})
		return
	}

	if reqBody.StartTime.After(reqBody.EndTime) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "start time must be before end time"})
		return
	}

	sale, err := CreatePluginSale(w.Database, &reqBody)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not create plugin sale: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, sale)
}

func CreatePluginSale(db *gorm.DB, sale *model.PluginSale) (*model.PluginSale, error) {
	tx := db.Begin()
	if tx.Error != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", tx.Error)
	}

	// Create the sale
	if err := tx.Create(&sale).Error; err != nil {
		tx.Rollback()
		return nil, fmt.Errorf("failed to create sale: %w", err)
	}

	// Add plugins to the sale
	for _, pluginName := range sale.PluginNames {
		var pluginMetadata model.PluginMetadata
		if err := tx.Where("name = ?", pluginName).First(&pluginMetadata).Error; err != nil {
			tx.Rollback()
			return nil, fmt.Errorf("failed to find plugin '%s': %w", pluginName, err)
		}

		saleItem := model.PluginSaleItem{
			SaleID:           sale.ID,
			PluginMetadataID: pluginMetadata.ID,
		}

		if err := tx.Create(&saleItem).Error; err != nil {
			tx.Rollback()
			return nil, fmt.Errorf("failed to create sale item for plugin '%s': %w", pluginName, err)
		}
	}

	return sale, tx.Commit().Error
}
