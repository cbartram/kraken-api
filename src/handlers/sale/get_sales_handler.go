package sale

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"time"
)

type GetSaleHandler struct{}

func (h *GetSaleHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	sales, err := GetActiveSales(w.Database)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to get sales: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, sales)
}

func GetActiveSales(db *gorm.DB) ([]model.PluginSale, error) {
	var sales []model.PluginSale
	now := time.Now()

	result := db.Preload("SaleItems").
		//Preload("SaleItems.PluginMetadata").
		Where("active = true AND start_time <= ? AND end_time >= ?", now, now).
		Find(&sales)

	return sales, result.Error
}
