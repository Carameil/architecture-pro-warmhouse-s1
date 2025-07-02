# Smart Home Docker Management Makefile

include .env
export

# Default target
.DEFAULT_GOAL := help

# Colors for output
RED = \033[0;31m
GREEN = \033[0;32m
YELLOW = \033[1;33m
BLUE = \033[0;34m
NC = \033[0m # No Color

# Help command
.PHONY: help
help: ## Show this help message
	@echo "${BLUE}Smart Home Docker Management Commands:${NC}"
	@echo ""
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "${GREEN}%-20s${NC} %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sort

# Build commands
.PHONY: build
build: ## Build all Docker images
	@echo "${YELLOW}Building all Docker images...${NC}"
	docker-compose build

.PHONY: build-temp-api
build-temp-api: ## Build only temperature-api image
	@echo "${YELLOW}Building temperature-api image...${NC}"
	docker-compose build temperature-api

.PHONY: build-app
build-app: ## Build only smart_home app image
	@echo "${YELLOW}Building smart_home app image...${NC}"
	docker-compose build app

# Start/Stop commands
.PHONY: up
up: ## Start all services in background
	@echo "${GREEN}Starting all services...${NC}"
	docker-compose up -d

.PHONY: up-build
up-build: ## Build and start all services
	@echo "${GREEN}Building and starting all services...${NC}"
	docker-compose up -d --build

.PHONY: down
down: ## Stop all services
	@echo "${RED}Stopping all services...${NC}"
	docker-compose down

.PHONY: restart
restart: down up ## Restart all services

# Individual service commands
.PHONY: start-postgres
start-postgres: ## Start only PostgreSQL service
	@echo "${GREEN}Starting PostgreSQL...${NC}"
	docker-compose up -d postgres

.PHONY: start-temp-api
start-temp-api: ## Start only temperature-api service
	@echo "${GREEN}Starting temperature-api...${NC}"
	docker-compose up -d temperature-api

.PHONY: start-app
start-app: ## Start only smart_home app service
	@echo "${GREEN}Starting smart_home app...${NC}"
	docker-compose up -d app

# Logs commands
.PHONY: logs
logs: ## Show logs for all services
	docker-compose logs -f

.PHONY: logs-postgres
logs-postgres: ## Show PostgreSQL logs
	docker-compose logs -f postgres

.PHONY: logs-temp-api
logs-temp-api: ## Show temperature-api logs
	docker-compose logs -f temperature-api

.PHONY: logs-app
logs-app: ## Show smart_home app logs
	docker-compose logs -f app

# Status and monitoring
.PHONY: status
status: ## Show status of all services
	@echo "${BLUE}Docker Compose Services Status:${NC}"
	docker-compose ps

.PHONY: health
health: ## Check health status of all services
	@echo "${BLUE}Health Status:${NC}"
	@docker-compose ps | grep -E "(postgres|temperature-api|app)"

# Development commands
.PHONY: shell-temp-api
shell-temp-api: ## Open shell in temperature-api container
	docker-compose exec temperature-api /bin/sh

.PHONY: shell-app
shell-app: ## Open shell in smart_home app container
	docker-compose exec app /bin/sh

.PHONY: shell-postgres
shell-postgres: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U postgres -d smarthome

# Symfony specific commands
.PHONY: create-symfony
create-symfony: ## Create Symfony skeleton in temperature-api/app
	@echo "${YELLOW}Creating Symfony skeleton...${NC}"
	@mkdir -p temperature-api/app
	@if [ ! -f "temperature-api/app/composer.json" ]; then \
		cd temperature-api/app && \
		composer create-project symfony/skeleton:"7.3.x-dev" . && \
		composer require webapp; \
	else \
		echo "${RED}Symfony project already exists!${NC}"; \
	fi

.PHONY: composer-install
composer-install: ## Install Composer dependencies in temperature-api/app
	docker-compose exec temperature-api composer install

.PHONY: symfony-cache-clear
symfony-cache-clear: ## Clear Symfony cache
	docker-compose exec temperature-api php bin/console cache:clear

# Testing commands
.PHONY: test-api
test-api: ## Test temperature API endpoint
	@echo "${BLUE}Testing temperature API...${NC}"
	@curl -f http://localhost:8081/temperature || echo "${RED}API not responding${NC}"

.PHONY: test-health
test-health: ## Test health endpoints
	@echo "${BLUE}Testing health endpoints...${NC}"
	@curl -f http://localhost:8081/health || echo "${RED}Temperature API health check failed${NC}"

.PHONY: test-sensors
test-sensors: ## Test smart home sensors endpoint
	@echo "${BLUE}Testing sensors API...${NC}"
	@curl -f http://localhost:8080/api/v1/sensors || echo "${RED}Sensors API not responding${NC}"

.PHONY: test-unit
test-unit: ## Run PHPUnit tests in temperature-api
	@echo "${BLUE}Running PHPUnit tests...${NC}"
	docker-compose exec temperature-api php bin/phpunit

.PHONY: test-unit-verbose
test-unit-verbose: ## Run PHPUnit tests with verbose output
	@echo "${BLUE}Running PHPUnit tests with verbose output...${NC}"
	docker-compose exec temperature-api php bin/phpunit --verbose

.PHONY: test-coverage
test-coverage: ## Run PHPUnit tests with coverage report
	@echo "${BLUE}Running PHPUnit tests with coverage...${NC}"
	docker-compose exec temperature-api php bin/phpunit --coverage-text

# Cleanup commands
.PHONY: clean
clean: ## Remove all containers and images
	@echo "${RED}Cleaning up Docker containers and images...${NC}"
	docker-compose down -v --rmi all

.PHONY: clean-volumes
clean-volumes: ## Remove all volumes (WARNING: This will delete all data!)
	@echo "${RED}Removing all volumes...${NC}"
	@read -p "Are you sure? This will delete all data! (y/N): " confirm && \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		docker-compose down -v; \
	else \
		echo "Cancelled."; \
	fi

.PHONY: prune
prune: ## Remove unused Docker resources
	@echo "${YELLOW}Removing unused Docker resources...${NC}"
	docker system prune -f

# Development workflow
.PHONY: init
init: ## Initialize complete environment using init.sh
	@echo "${YELLOW}Initializing Smart Home environment...${NC}"
	@chmod +x init.sh
	@./init.sh

.PHONY: dev-setup
dev-setup: create-symfony build up-full ## Complete development setup with infrastructure
	@echo "${GREEN}Development environment setup complete!${NC}"
	@echo ""
	@echo "${BLUE}Core Application Services:${NC}"
	@echo "  - Smart Home App (Go): http://localhost:8080"
	@echo "  - Temperature API (PHP): http://localhost:8081"
	@echo ""
	@echo "${BLUE}Microservices:${NC}"
	@echo "  - Device Registry API (Go): http://localhost:8082"
	@echo "  - Device Control API (Python): http://localhost:8083"
	@echo "  - Telemetry Service (Java): http://localhost:8084"
	@echo ""
	@echo "${BLUE}Database Services:${NC}"
	@echo "  - PostgreSQL (Main): localhost:5432"
	@echo "  - PostgreSQL (Device Registry): localhost:5433"
	@echo "  - Redis Device Control: localhost:6379"
	@echo "  - Redis Shared Cache: localhost:6380"
	@echo "  - InfluxDB (Telemetry): localhost:8086"
	@echo ""
	@echo "${BLUE}Infrastructure Services:${NC}"
	@echo "  - RabbitMQ Management: http://localhost:15672 (admin/admin123)"
	@echo ""
	@echo "${YELLOW}Testing Commands:${NC}"
	@echo "  - Test Device Registry: make test-device-registry"
	@echo "  - Test Device Control: make test-device-control"
	@echo "  - Test Telemetry Service: make test-telemetry"
	@echo "  - Test Infrastructure: make test-infrastructure"

.PHONY: dev-reset
dev-reset: clean-volumes build up ## Reset development environment
	@echo "${GREEN}Development environment reset complete!${NC}"

##@ Infrastructure Services

.PHONY: start-rabbitmq
start-rabbitmq: ## Start RabbitMQ message broker
	@echo "Starting RabbitMQ..."
	@docker-compose up -d rabbitmq

.PHONY: stop-rabbitmq
stop-rabbitmq: ## Stop RabbitMQ
	@echo "Stopping RabbitMQ..."
	@docker-compose stop rabbitmq

.PHONY: logs-rabbitmq
logs-rabbitmq: ## Show RabbitMQ logs
	@docker-compose logs -f rabbitmq

.PHONY: rabbitmq-ui
rabbitmq-ui: ## Open RabbitMQ Management UI
	@echo "Opening RabbitMQ Management UI at http://localhost:15672"
	@echo "Default credentials: admin/admin123"
	@open http://localhost:15672 || xdg-open http://localhost:15672 || echo "Please open http://localhost:15672 in your browser"

.PHONY: start-redis-device
start-redis-device: ## Start Redis for Device Control
	@echo "Starting Redis Device Control..."
	@docker-compose up -d redis-device-control

.PHONY: stop-redis-device
stop-redis-device: ## Stop Redis Device Control
	@echo "Stopping Redis Device Control..."
	@docker-compose stop redis-device-control

.PHONY: logs-redis-device
logs-redis-device: ## Show Redis Device Control logs
	@docker-compose logs -f redis-device-control

.PHONY: redis-device-cli
redis-device-cli: ## Connect to Redis Device Control CLI
	@docker-compose exec redis-device-control redis-cli -a redis123

.PHONY: start-redis-shared
start-redis-shared: ## Start Redis Shared Cache
	@echo "Starting Redis Shared Cache..."
	@docker-compose up -d redis-shared

.PHONY: stop-redis-shared
stop-redis-shared: ## Stop Redis Shared Cache
	@echo "Stopping Redis Shared Cache..."
	@docker-compose stop redis-shared

.PHONY: logs-redis-shared
logs-redis-shared: ## Show Redis Shared Cache logs
	@docker-compose logs -f redis-shared

.PHONY: redis-shared-cli
redis-shared-cli: ## Connect to Redis Shared Cache CLI
	@docker-compose exec redis-shared redis-cli -a redis456

.PHONY: start-influxdb
start-influxdb: ## Start InfluxDB
	@echo "Starting InfluxDB..."
	@docker-compose up -d influxdb

.PHONY: stop-influxdb
stop-influxdb: ## Stop InfluxDB
	@echo "Stopping InfluxDB..."
	@docker-compose stop influxdb

.PHONY: logs-influxdb
logs-influxdb: ## Show InfluxDB logs
	@docker-compose logs -f influxdb

.PHONY: influxdb-ui
influxdb-ui: ## Open InfluxDB UI
	@echo "Opening InfluxDB UI at http://localhost:8086"
	@echo "Default credentials: admin/influx123"
	@open http://localhost:8086 || xdg-open http://localhost:8086 || echo "Please open http://localhost:8086 in your browser"

.PHONY: start-infrastructure
start-infrastructure: ## Start all infrastructure services
	@echo "Starting all infrastructure services..."
	@docker-compose up -d rabbitmq redis-device-control redis-shared influxdb
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@docker-compose ps

.PHONY: stop-infrastructure
stop-infrastructure: ## Stop all infrastructure services
	@echo "Stopping all infrastructure services..."
	@docker-compose stop rabbitmq redis-device-control redis-shared influxdb

.PHONY: logs-infrastructure
logs-infrastructure: ## Show all infrastructure logs
	@docker-compose logs -f rabbitmq redis-device-control redis-shared influxdb

##@ Testing Infrastructure

.PHONY: test-rabbitmq
test-rabbitmq: ## Test RabbitMQ connection
	@echo "Testing RabbitMQ connection..."
	@docker-compose exec rabbitmq rabbitmq-diagnostics ping || echo "RabbitMQ is not running"

.PHONY: test-redis
test-redis: ## Test Redis connections
	@echo "Testing Redis Device Control..."
	@docker-compose exec redis-device-control redis-cli -a redis123 ping || echo "Redis Device Control is not running"
	@echo "Testing Redis Shared Cache..."
	@docker-compose exec redis-shared redis-cli -a redis456 ping || echo "Redis Shared Cache is not running"

.PHONY: test-influxdb
test-influxdb: ## Test InfluxDB connection
	@echo "Testing InfluxDB connection..."
	@docker-compose exec influxdb influx ping || echo "InfluxDB is not running"

.PHONY: test-infrastructure
test-infrastructure: test-rabbitmq test-redis test-influxdb ## Test all infrastructure services
	@echo "Infrastructure tests completed"

.PHONY: test-device-registry
test-device-registry: ## Test Device Registry microservice
	@echo "Testing Device Registry API..."
	@curl -f http://localhost:8082/health || echo "Device Registry health check failed"
	@echo ""
	@echo "Testing Device Types endpoint..."
	@curl -f http://localhost:8082/api/v1/device-types || echo "Device Types endpoint failed"
	@echo ""
	@echo "Testing Devices endpoint..."
	@curl -f http://localhost:8082/api/v1/devices || echo "Devices endpoint failed"

##@ Device Control Service

.PHONY: start-device-control
start-device-control: ## Start Device Control Service
	@echo "Starting Device Control Service..."
	@docker-compose up -d device-control

.PHONY: stop-device-control
stop-device-control: ## Stop Device Control Service
	@echo "Stopping Device Control Service..."
	@docker-compose stop device-control

.PHONY: restart-device-control
restart-device-control: stop-device-control start-device-control ## Restart Device Control Service

.PHONY: logs-device-control
logs-device-control: ## Show Device Control Service logs
	@docker-compose logs -f device-control

.PHONY: build-device-control
build-device-control: ## Build Device Control Service
	@echo "Building Device Control Service..."
	@docker-compose build device-control

.PHONY: shell-device-control
shell-device-control: ## Open shell in Device Control container
	@docker-compose exec device-control /bin/bash

.PHONY: test-device-control
test-device-control: ## Test Device Control Service
	@echo "Testing Device Control API health..."
	@curl -f http://localhost:8083/api/v1/health || echo "Device Control health check failed"
	@echo ""
	@echo "Testing Device Control root endpoint..."
	@curl -f http://localhost:8083/ || echo "Device Control root endpoint failed"

.PHONY: test-device-control-state
test-device-control-state: ## Test device state management (requires device ID)
	@if [ -z "$(DEVICE_ID)" ]; then \
		echo "Please provide DEVICE_ID: make test-device-control-state DEVICE_ID=<uuid>"; \
	else \
		echo "Getting device state for $(DEVICE_ID)..."; \
		curl -f http://localhost:8083/api/v1/devices/$(DEVICE_ID)/state || echo "Failed to get device state"; \
	fi

.PHONY: test-device-control-command
test-device-control-command: ## Send test command to device (requires device ID)
	@if [ -z "$(DEVICE_ID)" ]; then \
		echo "Please provide DEVICE_ID: make test-device-control-command DEVICE_ID=<uuid>"; \
	else \
		echo "Sending turn_on command to device $(DEVICE_ID)..."; \
		curl -X POST http://localhost:8083/api/v1/devices/$(DEVICE_ID)/commands \
			-H "Content-Type: application/json" \
			-d '{"command_type": "turn_on", "parameters": {}}' \
			|| echo "Failed to send command"; \
	fi

##@ Telemetry Service

.PHONY: start-telemetry
start-telemetry: ## Start Telemetry Service
	@echo "Starting Telemetry Service..."
	@docker-compose up -d telemetry-service

.PHONY: stop-telemetry
stop-telemetry: ## Stop Telemetry Service
	@echo "Stopping Telemetry Service..."
	@docker-compose stop telemetry-service

.PHONY: restart-telemetry
restart-telemetry: stop-telemetry start-telemetry ## Restart Telemetry Service

.PHONY: logs-telemetry
logs-telemetry: ## Show Telemetry Service logs
	@docker-compose logs -f telemetry-service

.PHONY: build-telemetry
build-telemetry: ## Build Telemetry Service
	@echo "Building Telemetry Service..."
	@docker-compose build telemetry-service

.PHONY: test-telemetry
test-telemetry: ## Test Telemetry Service
	@echo "Testing Telemetry Service health..."
	@curl -f http://localhost:8084/health || echo "Telemetry Service health check failed"
	@echo ""
	@echo "Testing telemetry data submission..."
	@curl -X POST http://localhost:8084/api/v1/telemetry \
		-H "Content-Type: application/json" \
		-d '{"deviceId": "550e8400-e29b-41d4-a716-446655440000", "houseId": "550e8400-e29b-41d4-a716-446655440001", "locationId": "550e8400-e29b-41d4-a716-446655440002", "measurementType": "temperature", "value": 23.5, "unit": "celsius", "quality": "GOOD"}' \
		|| echo "Failed to submit telemetry data"

.PHONY: test-telemetry-statistics
test-telemetry-statistics: ## Test telemetry statistics endpoint
	@echo "Testing telemetry statistics..."
	@curl -f "http://localhost:8084/api/v1/telemetry/statistics?deviceId=550e8400-e29b-41d4-a716-446655440000&measurementType=temperature&period=1h" || echo "Failed to get statistics"

##@ Full Stack Operations

.PHONY: up-full
up-full: ## Start entire system including new infrastructure
	@echo "Starting full smart home system..."
	@docker-compose up -d
	@echo "Waiting for all services to be healthy..."
	@sleep 15
	@docker-compose ps
	@echo ""
	@echo "System is ready!"
	@echo "- Smart Home App (Go): http://localhost:8080"
	@echo "- Temperature API (PHP): http://localhost:8081"
	@echo "- Device Registry API (Go): http://localhost:8082"
	@echo "- Device Control API (Python): http://localhost:8083"
	@echo "- Telemetry Service (Java): http://localhost:8084"
	@echo "- RabbitMQ Management: http://localhost:15672 (admin/admin123)"
	@echo "- InfluxDB: http://localhost:8086 (admin/influx123)"

.PHONY: down-full
down-full: ## Stop entire system
	@echo "Stopping full smart home system..."
	@docker-compose down

.PHONY: logs-full
logs-full: ## Show logs for all services
	@docker-compose logs -f

# ... existing code ... 