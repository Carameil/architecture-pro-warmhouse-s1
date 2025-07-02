package com.warmhouse.telemetry.service;

import com.warmhouse.telemetry.dto.TelemetryRequest;
import com.warmhouse.telemetry.dto.TelemetryResponse;
import com.warmhouse.telemetry.dto.TelemetryStatistics;
import com.warmhouse.telemetry.model.TelemetryData;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main telemetry service for business logic
 */
@Service
@RequiredArgsConstructor
public class TelemetryService {
    
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    
    private final InfluxDBService influxDBService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceValidationService deviceValidationService;
    
    private static final String DEVICE_CACHE_PREFIX = "device:";
    private static final String LOCATION_CACHE_PREFIX = "location:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    
    /**
     * Store telemetry data
     */
    public TelemetryResponse storeTelemetryData(TelemetryRequest request) {
        log.info("Storing telemetry data for device: {}", request.getDeviceId());
        
        // Validate device exists
        if (!deviceValidationService.validateDevice(request.getDeviceId())) {
            throw new IllegalArgumentException("Invalid device ID: " + request.getDeviceId());
        }
        
        // Create telemetry data model
        TelemetryData data = TelemetryData.builder()
                .measurementId(UUID.randomUUID())
                .deviceId(request.getDeviceId())
                .houseId(request.getHouseId())
                .locationId(request.getLocationId())
                .measurementType(request.getMeasurementType())
                .value(request.getValue())
                .unit(request.getUnit() != null ? request.getUnit() : "")
                .quality(request.getQuality() != null ? request.getQuality() : "GOOD")
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .build();
        
        // Write to InfluxDB
        influxDBService.writeTelemetryData(data);
        
        // Cache device location mapping
        cacheDeviceLocation(request.getDeviceId(), request.getLocationId());
        
        // Return response
        return TelemetryResponse.fromModel(data);
    }
    
    /**
     * Get telemetry data for a device
     */
    public List<TelemetryResponse> getTelemetryByDevice(UUID deviceId, String period) {
        log.info("Fetching telemetry data for device: {} with period: {}", deviceId, period);
        
        Instant end = Instant.now();
        Instant start = calculateStartTime(end, period);
        
        List<TelemetryData> dataList = influxDBService.queryByDeviceId(deviceId, start, end);
        
        return dataList.stream()
                .map(TelemetryResponse::fromModel)
                .collect(Collectors.toList());
    }
    
    /**
     * Get telemetry statistics
     */
    public TelemetryStatistics getTelemetryStatistics(UUID deviceId, String measurementType, String period) {
        log.info("Calculating statistics for device: {}, type: {}, period: {}", 
                deviceId, measurementType, period);
        
        TelemetryStatistics stats = influxDBService.calculateStatistics(deviceId, measurementType, period);
        
        // Enrich with cached device/location names if available
        enrichStatisticsWithNames(stats);
        
        return stats;
    }
    
    /**
     * Store batch of telemetry data
     */
    public void storeTelemetryDataBatch(List<TelemetryRequest> requests) {
        log.info("Storing batch of {} telemetry data points", requests.size());
        
        // Convert requests to data models
        List<TelemetryData> dataList = requests.stream()
                .map(request -> {
                    // Validate each device
                    if (!deviceValidationService.validateDevice(request.getDeviceId())) {
                        log.warn("Skipping invalid device: {}", request.getDeviceId());
                        return null;
                    }
                    
                    return TelemetryData.builder()
                            .measurementId(UUID.randomUUID())
                            .deviceId(request.getDeviceId())
                            .houseId(request.getHouseId())
                            .locationId(request.getLocationId())
                            .measurementType(request.getMeasurementType())
                            .value(request.getValue())
                            .unit(request.getUnit() != null ? request.getUnit() : "")
                            .quality(request.getQuality() != null ? request.getQuality() : "GOOD")
                            .timestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now())
                            .tags(request.getTags())
                            .metadata(request.getMetadata())
                            .build();
                })
                .filter(data -> data != null)
                .collect(Collectors.toList());
        
        if (!dataList.isEmpty()) {
            influxDBService.writeTelemetryDataBatch(dataList);
        }
    }
    
    private void cacheDeviceLocation(UUID deviceId, UUID locationId) {
        String key = DEVICE_CACHE_PREFIX + deviceId.toString();
        redisTemplate.opsForValue().set(key, locationId.toString(), CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private void enrichStatisticsWithNames(TelemetryStatistics stats) {
        // Try to get device name from cache
        String deviceKey = DEVICE_CACHE_PREFIX + stats.getDeviceId().toString();
        Object cachedDeviceName = redisTemplate.opsForValue().get(deviceKey + ":name");
        if (cachedDeviceName != null) {
            stats.setDeviceName(cachedDeviceName.toString());
        }
        
        // Try to get location name from cache
        Object cachedLocationId = redisTemplate.opsForValue().get(deviceKey);
        if (cachedLocationId != null) {
            String locationKey = LOCATION_CACHE_PREFIX + cachedLocationId.toString();
            Object cachedLocationName = redisTemplate.opsForValue().get(locationKey + ":name");
            if (cachedLocationName != null) {
                stats.setLocationName(cachedLocationName.toString());
            }
        }
    }
    
    private Instant calculateStartTime(Instant end, String period) {
        switch (period.toLowerCase()) {
            case "1h":
                return end.minus(1, ChronoUnit.HOURS);
            case "24h":
            case "1d":
                return end.minus(1, ChronoUnit.DAYS);
            case "7d":
            case "1w":
                return end.minus(7, ChronoUnit.DAYS);
            case "30d":
            case "1m":
                return end.minus(30, ChronoUnit.DAYS);
            default:
                return end.minus(1, ChronoUnit.HOURS);
        }
    }
} 