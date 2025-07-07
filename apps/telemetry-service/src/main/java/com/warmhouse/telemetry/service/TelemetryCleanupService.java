package com.warmhouse.telemetry.service;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.domain.DeletePredicateRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Service for cleaning up telemetry data when devices are deleted
 */
@Service
@RequiredArgsConstructor
public class TelemetryCleanupService {
    
    private static final Logger log = LoggerFactory.getLogger(TelemetryCleanupService.class);
    
    private final DeleteApi deleteApi;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${influxdb.bucket}")
    private String bucket;
    
    @Value("${influxdb.org}")
    private String org;
    
    @PostConstruct
    public void init() {
        log.info("TelemetryCleanupService @PostConstruct called - bean is ready");
    }
    
    private static final String DEVICE_CACHE_PREFIX = "device:";
    private static final String LOCATION_CACHE_PREFIX = "location:";
    private static final String DEVICE_VALIDATION_PREFIX = "device_validation:";
    private static final String SENSOR_CORRELATION_PREFIX = "sensor_correlation:";
    
    /**
     * Clean up all telemetry data for a deleted device
     * 
     * @param deviceId UUID of the device to clean up
     * @return true if cleanup was successful, false otherwise
     */
    public boolean cleanupDeviceData(UUID deviceId) {
        log.info("Starting cleanup for device: {}", deviceId);
        
        boolean success = true;
        
        try {
            // 1. Delete telemetry data from InfluxDB
            success &= deleteInfluxDBData(deviceId);
            
            // 2. Clear Redis cache entries
            success &= clearRedisCache(deviceId);
            
            if (success) {
                log.info("Successfully completed cleanup for device: {}", deviceId);
            } else {
                log.warn("Cleanup completed with some errors for device: {}", deviceId);
            }
            
        } catch (Exception e) {
            log.error("Error during cleanup for device {}: {}", deviceId, e.getMessage(), e);
            success = false;
        }
        
        return success;
    }
    
    /**
     * Delete all telemetry data for a device from InfluxDB
     */
    private boolean deleteInfluxDBData(UUID deviceId) {
        try {
            log.info("Deleting InfluxDB data for device: {}", deviceId);
            
            // Create delete predicate to delete all data for this device
            // Delete all data from the beginning of time to now
            OffsetDateTime start = OffsetDateTime.parse("1970-01-01T00:00:00Z");
            OffsetDateTime stop = OffsetDateTime.now();
            
            // Create delete request with device_id filter
            String predicate = String.format("device_id=\"%s\"", deviceId.toString());
            
            DeletePredicateRequest deleteRequest = new DeletePredicateRequest()
                    .start(start)
                    .stop(stop)
                    .predicate(predicate);
            
            // Execute delete
            deleteApi.delete(deleteRequest, bucket, org);
            
            log.info("Successfully deleted InfluxDB data for device: {}", deviceId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to delete InfluxDB data for device {}: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clear all Redis cache entries related to a device
     */
    private boolean clearRedisCache(UUID deviceId) {
        try {
            log.info("Clearing Redis cache for device: {}", deviceId);
            
            String deviceIdStr = deviceId.toString();
            int deletedKeys = 0;
            
            // 1. Clear device cache entries
            String deviceCacheKey = DEVICE_CACHE_PREFIX + deviceIdStr;
            if (Boolean.TRUE.equals(redisTemplate.delete(deviceCacheKey))) {
                deletedKeys++;
            }
            
            // Clear device name cache
            String deviceNameKey = deviceCacheKey + ":name";
            if (Boolean.TRUE.equals(redisTemplate.delete(deviceNameKey))) {
                deletedKeys++;
            }
            
            // 2. Clear device validation cache
            String validationKey = DEVICE_VALIDATION_PREFIX + deviceIdStr;
            if (Boolean.TRUE.equals(redisTemplate.delete(validationKey))) {
                deletedKeys++;
            }
            
            // 3. Clear sensor correlation data
            String correlationPattern = SENSOR_CORRELATION_PREFIX + "*";
            Set<String> correlationKeys = redisTemplate.keys(correlationPattern);
            if (correlationKeys != null) {
                for (String key : correlationKeys) {
                    try {
                        Object data = redisTemplate.opsForValue().get(key);
                        if (data != null && data.toString().contains(deviceIdStr)) {
                            if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
                                deletedKeys++;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error checking correlation key {}: {}", key, e.getMessage());
                    }
                }
            }
            
            // 4. Clear any other cache entries containing the device ID
            String devicePattern = "*" + deviceIdStr + "*";
            Set<String> deviceKeys = redisTemplate.keys(devicePattern);
            if (deviceKeys != null) {
                for (String key : deviceKeys) {
                    try {
                        if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
                            deletedKeys++;
                        }
                    } catch (Exception e) {
                        log.warn("Error deleting cache key {}: {}", key, e.getMessage());
                    }
                }
            }
            
            log.info("Successfully cleared {} Redis cache entries for device: {}", deletedKeys, deviceId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to clear Redis cache for device {}: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clean up telemetry data older than specified days
     * This is a general cleanup method for data retention
     * 
     * @param retentionDays Number of days to retain data
     * @return Number of deleted records (if supported by the implementation)
     */
    public long cleanupOldData(int retentionDays) {
        try {
            log.info("Cleaning up telemetry data older than {} days", retentionDays);
            
            OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);
            OffsetDateTime start = OffsetDateTime.parse("1970-01-01T00:00:00Z");
            
            // Delete all data older than cutoff time
            DeletePredicateRequest deleteRequest = new DeletePredicateRequest()
                    .start(start)
                    .stop(cutoffTime);
            
            deleteApi.delete(deleteRequest, bucket, org);
            
            log.info("Successfully cleaned up old telemetry data");
            return 1; // InfluxDB doesn't return count, so return 1 for success
            
        } catch (Exception e) {
            log.error("Failed to clean up old telemetry data: {}", e.getMessage(), e);
            return 0;
        }
    }
} 