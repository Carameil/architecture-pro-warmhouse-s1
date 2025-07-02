package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"device-registry/db"
	"device-registry/events"
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

	// Initialize RabbitMQ subscriber
	rabbitMQURL := getRabbitMQURL()
	eventSubscriber, err := events.NewSubscriber(rabbitMQURL, deviceService, deviceTypeService)
	if err != nil {
		log.Printf("Warning: Unable to connect to RabbitMQ: %v (continuing without events)", err)
		eventSubscriber = nil
	} else {
		defer eventSubscriber.Close()
		log.Println("Connected to RabbitMQ successfully")
	}

	// Initialize handlers
	deviceHandler := handlers.NewDeviceHandler(deviceService, deviceTypeService)

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
	router.GET("/health", func(c *gin.Context) {
		healthData := gin.H{
			"status":   "ok",
			"database": "connected",
		}

		if eventSubscriber != nil && eventSubscriber.IsConnected() {
			healthData["events"] = "connected"
		} else {
			healthData["events"] = "disconnected"
		}

		c.JSON(200, healthData)
	})

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

	// Start event subscriber in a goroutine
	if eventSubscriber != nil {
		go func() {
			ctx := context.Background()
			if err := eventSubscriber.StartListening(ctx); err != nil {
				log.Printf("Event subscriber error: %v", err)
			}
		}()
	}

	log.Printf("Device Registry Service starting on port %s", serverPort)
	log.Printf("Database: %s:%s/%s", dbHost, dbPort, dbName)
	log.Printf("Health check: http://localhost:%s/health", serverPort)
	log.Printf("API: http://localhost:%s/api/v1", serverPort)
	if eventSubscriber != nil {
		log.Printf("Event subscriber: listening for sensor events")
	}

	// Handle graceful shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)

	go func() {
		if err := router.Run(":" + serverPort); err != nil {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Wait for interrupt signal
	<-c
	log.Println("Shutting down Device Registry Service...")

	if eventSubscriber != nil {
		eventSubscriber.Close()
	}

	log.Println("Device Registry Service stopped")
}

// getRabbitMQURL constructs RabbitMQ URL from environment variables
func getRabbitMQURL() string {
	host := getEnv("RABBITMQ_HOST", "localhost")
	port := getEnv("RABBITMQ_PORT", "5672")
	user := getEnv("RABBITMQ_USER", "admin")
	password := getEnv("RABBITMQ_PASSWORD", "admin123")

	return "amqp://" + user + ":" + password + "@" + host + ":" + port + "/"
}

// getEnv gets environment variable with fallback
func getEnv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
