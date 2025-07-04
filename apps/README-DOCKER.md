# 🐳 Smart Home Docker Setup

Optimized Docker configuration for the Smart Home system with microservices architecture, message broker, and multiple data stores.

## 📋 Quick Start

```bash
# 1. Show all available commands
make help

# 2. Complete development setup (builds + starts services + composer install for temperature-api)
make dev-setup

# 3. Start full system with microservices infrastructure
make up-full

# 4. Check services status
make status

# 5. Test APIs
make test-api
make test-health
make test-infrastructure
```

## 🏗️ Architecture

### Core Services
- **PostgreSQL** (`postgres:16-alpine`) - Database with init.sql script
- **Temperature API** (`php:8.4-fpm-alpine`) - Symfony API on port 8081
- **Smart Home App** (Go application) - Main app on port 8080

### Microservices Infrastructure (NEW)
- **RabbitMQ** (`rabbitmq:3.13-management-alpine`) - Message broker for event-driven architecture
  - Port 5672: AMQP protocol
  - Port 15672: Management UI (admin/admin123)
- **Redis Device Control** (`redis:7-alpine`) - Dedicated cache for device states
  - Port 6379: Real-time device state management
- **Redis Shared** (`redis:7-alpine`) - Shared cache for sessions, permissions, metadata
  - Port 6380: Cross-service cache data
- **InfluxDB** (`influxdb:2.7-alpine`) - Time-series database for telemetry
  - Port 8086: HTTP API and UI (admin/influx123)

### Implemented Microservices
- **Device Registry Service** (Go) - Port 8082 ✅ COMPLETED
  - Clean Architecture with dependency injection
  - PostgreSQL with device catalog and registry
  - Full REST API with CRUD operations
  - Health monitoring and error handling

- **Device Control Service** (Python FastAPI) - Port 8083 ✅ COMPLETED
  - Redis-only architecture for real-time state management
  - Command queue with priority handling
  - Device state synchronization with Device Registry
  - Simulated command execution for demo purposes
  - Full REST API for device control operations

- **Telemetry Service** (Java Spring Boot) - Port 8084 ✅ COMPLETED
  - InfluxDB time-series database for telemetry data storage
  - Redis caching for device metadata and location mappings
  - Device validation integration with Device Registry
  - Batch telemetry data processing with high performance
  - Statistical analytics (min, max, avg, sum, count)
  - Full REST API for telemetry operations

### Docker Optimizations Applied
✅ **Lightweight Alpine images** - Reduced image size by ~70%  
✅ **Multi-stage build** - Composer separation for smaller final image  
✅ **Minimal layers** - Combined RUN commands to reduce layer count  
✅ **Cache cleanup** - Removed temporary files and package caches  
✅ **Build deps cleanup** - Virtual packages removed after installation  

## 🛠️ Development Commands

### Basic Operations
```bash
make up              # Start all services
make down            # Stop all services  
make restart         # Restart all services
make status          # Show services status
make logs            # Show all logs
```

### Full Stack Operations
```bash
make up-full         # Start entire system with infrastructure
make down-full       # Stop entire system
make logs-full       # Show all service logs
```

### Infrastructure Management
```bash
make start-infrastructure  # Start RabbitMQ, Redis x2, InfluxDB
make stop-infrastructure   # Stop infrastructure services
make logs-infrastructure   # Show infrastructure logs

# Individual services
make start-rabbitmq        # Start RabbitMQ
make start-redis-device    # Start Redis Device Control
make start-redis-shared    # Start Redis Shared Cache
make start-influxdb        # Start InfluxDB
```

### Infrastructure Access
```bash
make rabbitmq-ui           # Open RabbitMQ Management (localhost:15672)
make influxdb-ui           # Open InfluxDB UI (localhost:8086)
make redis-device-cli      # Connect to Redis Device Control CLI
make redis-shared-cli      # Connect to Redis Shared Cache CLI
```

### Testing
```bash
make test-api              # Test temperature API endpoint
make test-health           # Test health endpoints
make test-infrastructure   # Test RabbitMQ, Redis, InfluxDB connections
make test-rabbitmq         # Test RabbitMQ only
make test-redis            # Test both Redis instances
make test-influxdb         # Test InfluxDB only

# Microservices Testing
make test-device-registry  # Test Device Registry API
make test-device-control   # Test Device Control API
make test-device-control-state DEVICE_ID=<uuid>  # Test device state
make test-device-control-command DEVICE_ID=<uuid>  # Send command to device
make test-telemetry        # Test Telemetry Service API
make test-telemetry-statistics  # Test telemetry statistics endpoint
```

### Building
```bash
make build           # Build all images
make build-temp-api  # Build only temperature-api
make up-build        # Build and start services
```

### Individual Services
```bash
make start-postgres  # Start only PostgreSQL
make start-temp-api  # Start only temperature API  
make logs-postgres   # Show PostgreSQL logs
make logs-temp-api   # Show temperature API logs
make logs-rabbitmq   # Show RabbitMQ logs
make logs-redis-device    # Show Redis Device Control logs
make logs-redis-shared    # Show Redis Shared Cache logs
make logs-influxdb   # Show InfluxDB logs
```

### Development Tools
```bash
make shell-temp-api    # Open shell in temperature-api container
make shell-postgres    # Open PostgreSQL shell
make composer-install  # Install PHP dependencies
make symfony-cache-clear # Clear Symfony cache
```

### Cleanup
```bash
make clean           # Remove containers and images
make clean-volumes   # Remove volumes (⚠️ deletes data!)
make prune          # Remove unused Docker resources
```

## 🔧 Configuration

### Environment Variables
Configuration is managed via `.env` file (copy from `.env.example`):

## 🐛 Troubleshooting

### Common Issues

**Service won't start:**
```bash
make logs <service-name>  # Check logs
make health              # Check health status
make test-infrastructure # Test all infrastructure
```

**Database connection issues:**
```bash
make shell-postgres      # Check database directly
```

**Temperature API not responding:**
```bash
make shell-temp-api      # Check container
make logs-temp-api       # Check logs
```

**RabbitMQ issues:**
```bash
make logs-rabbitmq       # Check logs
make rabbitmq-ui         # Access management UI
make test-rabbitmq       # Test connection
```

**Redis issues:**
```bash
make redis-device-cli    # Check Device Control Redis
make redis-shared-cli    # Check Shared Redis
make test-redis          # Test both Redis instances
```

### Reset Environment
```bash
make dev-reset           # Complete reset (⚠️ deletes data!)
```

## 📝 Next Steps

1. **Create .env file** (if not exists):
   ```bash
   cp env.example .env
   ```

2. **Start infrastructure**:
   ```bash
   make up-full
   ```

3. **Verify all services**:
   ```bash
   make test-infrastructure
   make status
   ```

4. **Access UIs**:
   - RabbitMQ: http://localhost:15672
   - InfluxDB: http://localhost:8086

5. **Begin microservices development**:
   - Device Registry Service (Go)
   - Device Control Service (Python)
   - Telemetry Service (Java Spring Boot)