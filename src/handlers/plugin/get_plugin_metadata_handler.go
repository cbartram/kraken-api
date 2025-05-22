package plugin

import (
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
)

type PluginMetadataHandler struct{}

func (p *PluginMetadataHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	name := c.Query("name")

	if name == "" {
		plugins := w.PluginStore.GetPlugins()

		// Single efficient query to get all active sales
		salesLookup, err := model.GetActiveSalesLookup(w.Database)
		if err != nil {
			log.Errorf("failed to get sales lookup: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to load sales data"})
			return
		}

		var response []model.PluginMetadata

		for _, plugin := range plugins {
			if discount, hasSale := salesLookup[plugin.ID]; hasSale && discount > 0 {
				plugin.SaleDiscount = discount
				plugin.PriceDetails.SaleMonth = util.CalculateDiscountedPrice(plugin.PriceDetails.Month, discount)
				plugin.PriceDetails.SaleThreeMonth = util.CalculateDiscountedPrice(plugin.PriceDetails.ThreeMonth, discount)
				plugin.PriceDetails.SaleYear = util.CalculateDiscountedPrice(plugin.PriceDetails.Year, discount)
			}

			response = append(response, plugin)
		}

		c.JSON(http.StatusOK, response)
		return
	}

	// Single plugin request - can still use the individual method for simplicity
	plugin, err := w.PluginStore.GetPlugin(name, w.S3Service)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	sale, err := plugin.GetCurrentSaleDiscount(w.Database)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to get sale data: " + err.Error()})
		return
	}

	if sale > 0 {
		plugin.SaleDiscount = sale
		plugin.PriceDetails.SaleMonth = util.CalculateDiscountedPrice(plugin.PriceDetails.Month, sale)
		plugin.PriceDetails.SaleThreeMonth = util.CalculateDiscountedPrice(plugin.PriceDetails.ThreeMonth, sale)
		plugin.PriceDetails.SaleYear = util.CalculateDiscountedPrice(plugin.PriceDetails.Year, sale)
	}

	c.JSON(http.StatusOK, plugin)
}
