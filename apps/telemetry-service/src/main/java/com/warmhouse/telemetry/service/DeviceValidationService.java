package com.warmhouse.telemetry.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for validating devices via Device Registry API
 */
@Service
@RequiredArgsConstructor
public class DeviceValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceValidationService.class);
    
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${device-registry.url}")
    private String deviceRegistryUrl;
    
    private static final String DEVICE_VALIDATION_CACHE_PREFIX = "device:validation:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Validate if device exists in Device Registry
     */
    public boolean validateDevice(UUID deviceId) {
        String cacheKey = DEVICE_VALIDATION_CACHE_PREFIX + deviceId.toString();
        
        // Check cache first
        Boolean cachedResult = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.debug("Device validation cache hit for device: {}", deviceId);
            return cachedResult;
        }
        
        // Call Device Registry API
        try {
            String url = deviceRegistryUrl + "/api/v1/devices/" + deviceId.toString();
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            
            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, isValid, CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
            
            log.info("Device {} validation result: {}", deviceId, isValid);
            return isValid;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Device not found - cache negative result
                redisTemplate.opsForValue().set(cacheKey, false, CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
                log.warn("Device {} not found in Device Registry", deviceId);
                return false;
            }
            log.error("Error validating device {}: {}", deviceId, e.getMessage());
            // Don't cache errors - might be temporary
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Invalidate device validation cache
     */
    public void invalidateDeviceCache(UUID deviceId) {
        String cacheKey = DEVICE_VALIDATION_CACHE_PREFIX + deviceId.toString();
        redisTemplate.delete(cacheKey);
        log.info("Invalidated device validation cache for device: {}", deviceId);
    }
} 