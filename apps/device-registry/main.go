package main

import (
	"log"
	"os"

	"device-registry/db"
	"device-registry/handlers"
	"device-registry/services"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables
	if err := godotenv.Load("../.env"); err != nil {
		log.Printf("Warning: .env file not found: %v", err)
	}

	// Configuration from environment
	dbHost := getEnv("DEVICE_REGISTRY_DB_HOST", "localhost")
	dbPort := getEnv("DEVICE_REGISTRY_POSTGRES_PORT", "5433")
	dbUser := getEnv("DEVICE_REGISTRY_DB_USER", "device_registry")
	dbPassword := getEnv("DEVICE_REGISTRY_DB_PASSWORD", "device123")
	dbName := getEnv("DEVICE_REGISTRY_DB_NAME", "device_registry")
	serverPort := getEnv("DEVICE_REGISTRY_PORT", "8082")

	// Connect to database
	database, err := db.NewConnection(dbHost, dbPort, dbUser, dbPassword, dbName)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer database.Close()

	// Initialize services
	deviceService := services.NewDeviceService(database)
	deviceTypeService := services.NewDeviceTypeService(database)

	// Initialize handlers
	deviceHandler := handlers.NewDeviceHandler(deviceService, deviceTypeService)
	healthHandler := handlers.NewHealthHandler(database)

	// Setup router
	router := gin.Default()

	// Add CORS middleware for development
	router.Use(func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	})

	// Health check (no authentication required)
	router.GET("/health", healthHandler.GetHealth)

	// API routes
	v1 := router.Group("/api/v1")
	{
		// Device management endpoints
		v1.GET("/devices", deviceHandler.GetDevices)
		v1.POST("/devices", deviceHandler.CreateDevice)
		v1.GET("/devices/:deviceId", deviceHandler.GetDeviceByID)
		v1.PUT("/devices/:deviceId", deviceHandler.UpdateDevice)
		v1.DELETE("/devices/:deviceId", deviceHandler.DeleteDevice)

		// Device types catalog
		v1.GET("/device-types", deviceHandler.GetDeviceTypes)
	}

	log.Printf("Device Registry Service starting on port %s", serverPort)
	log.Printf("Database: %s:%s/%s", dbHost, dbPort, dbName)
	log.Printf("Health check: http://localhost:%s/health", serverPort)
	log.Printf("API: http://localhost:%s/api/v1", serverPort)

	if err := router.Run(":" + serverPort); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

// getEnv gets environment variable with fallback
func getEnv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
