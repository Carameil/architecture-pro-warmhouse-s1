package handlers

import (
	"net/http"

	"device-registry/db"

	"github.com/gin-gonic/gin"
)

// HealthHandler handles health check requests
type HealthHandler struct {
	db *db.DB
}

// NewHealthHandler creates a new health handler
func NewHealthHandler(database *db.DB) *HealthHandler {
	return &HealthHandler{
		db: database,
	}
}

// GetHealth handles GET /health
func (h *HealthHandler) GetHealth(c *gin.Context) {
	status := "healthy"
	dbStatus := "disconnected"

	if h.db.IsHealthy() {
		dbStatus = "connected"
	} else {
		status = "unhealthy"
	}

	statusCode := http.StatusOK
	if status == "unhealthy" {
		statusCode = http.StatusServiceUnavailable
	}

	c.JSON(statusCode, gin.H{
		"status":          status,
		"database_status": dbStatus,
		"service":         "device-registry",
		"version":         "1.0.0",
	})
}
