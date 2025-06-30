# Smart Home Docker Management Makefile

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
dev-setup: create-symfony build up ## Complete development setup
	@echo "${GREEN}Development environment setup complete!${NC}"
	@echo "Services available at:"
	@echo "  - Temperature API: http://localhost:8081"
	@echo "  - Smart Home App: http://localhost:8080"
	@echo "  - PostgreSQL: localhost:5432"

.PHONY: dev-reset
dev-reset: clean-volumes build up ## Reset development environment
	@echo "${GREEN}Development environment reset complete!${NC}" 