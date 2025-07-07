# Telemetry Service

Telemetry Service is a microservice responsible for collecting, storing, and analyzing time-series sensor data from smart home devices.

## Technology Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.5.3
- **Time-Series Database:** InfluxDB
- **Cache:** Redis (Shared)
- **Message Broker:** RabbitMQ
- **Build Tool:** Maven

## Architecture

The service follows clean architecture principles with the following layers:
- **Controllers:** REST API endpoints
- **Services:** Business logic
- **DTOs:** Data transfer objects
- **Models:** Domain entities
- **Config:** Configuration classes

## API Endpoints

- `POST /api/v1/telemetry` - Store telemetry data
- `POST /api/v1/telemetry/batch` - Store batch of telemetry data
- `GET /api/v1/telemetry/devices/{deviceId}` - Get telemetry data for a device
- `GET /api/v1/telemetry/statistics` - Get telemetry statistics
- `GET /health` - Health check endpoint

## Required Environment Variables

```bash
# Service Configuration
TELEMETRY_PORT=8084
LOG_LEVEL=INFO

# InfluxDB Configuration
INFLUXDB_URL=http://localhost:8086
INFLUXDB_TOKEN=my-super-secret-auth-token
INFLUXDB_ORG=warmhouse
INFLUXDB_BUCKET=telemetry
INFLUXDB_RETENTION=30d

# Redis Configuration (Shared Cache)
REDIS_SHARED_HOST=localhost
REDIS_SHARED_PORT=6380
REDIS_SHARED_PASSWORD=redis123

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Device Registry Service
DEVICE_REGISTRY_URL=http://localhost:8082
```

## Features

- **Time-Series Data Storage:** Efficient storage in InfluxDB
- **Batch Processing:** Support for bulk data ingestion
- **Device Validation:** Integration with Device Registry Service
- **Caching:** Redis caching for device validation results
- **Statistics:** Basic analytics (min, max, avg, sum)
- **Health Monitoring:** Comprehensive health checks

## Development

### Build
```bash
mvn clean package
```

### Run locally
```bash
java -jar target/telemetry-service-*.jar
```

### Run with Docker
```bash
docker build -t telemetry-service .
docker run -p 8084:8084 telemetry-service
```

## Testing

### Example telemetry data submission:
```json
POST /api/v1/telemetry
{
  "deviceId": "9745d725-0b1f-4d2f-93f8-454a0d4cca67",
  "houseId": "123e4567-e89b-12d3-a456-426614174000",
  "locationId": "456e7890-e89b-12d3-a456-426614174000",
  "measurementType": "temperature",
  "value": 22.5,
  "unit": "celsius",
  "quality": "GOOD"
}
```

## Data Model

### TelemetryData
- `measurementId`: Unique identifier
- `deviceId`: Device that generated the data
- `houseId`: House identifier
- `locationId`: Location within the house
- `measurementType`: Type of measurement (e.g., temperature, humidity)
- `value`: Measured value
- `unit`: Unit of measurement
- `quality`: Data quality indicator
- `timestamp`: Time of measurement
- `tags`: Additional tags for InfluxDB
- `metadata`: Additional metadata

## Integration

The service integrates with:
- **Device Registry Service:** For device validation
- **InfluxDB:** For time-series data storage
- **Redis:** For caching device validation results
- **RabbitMQ:** For event-driven communication (future implementation) 