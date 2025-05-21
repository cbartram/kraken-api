package plugin

import (
	"github.com/gin-gonic/gin"
	"kraken-api/src/service"
	"net/http"
)

type PluginMetadataHandler struct{}

func (p *PluginMetadataHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	name := c.Query("name")

	if name == "" {
		c.JSON(http.StatusOK, w.PluginStore.GetPlugins())
		return
	}

	plugin, err := w.PluginStore.GetPlugin(name, w.S3Service)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, plugin)
}
