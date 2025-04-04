package plugin

import (
	"github.com/gin-gonic/gin"
	"kraken-api/src/service"
	"net/http"
)

type PluginPackHandler struct{}

func (p *PluginPackHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	name := c.Query("name")

	if name == "" {
		c.JSON(http.StatusOK, w.PluginStore.GetPluginPacks())
		return
	}

	plugin, err := w.PluginStore.GetPluginPack(name)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, plugin)
}
