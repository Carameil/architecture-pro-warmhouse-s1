package com.warmhouse.telemetry.controller;

import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller
 */
@RestController
@RequiredArgsConstructor
public class HealthController {
    
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    
    private final InfluxDBClient influxDBClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Health check endpoint
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "telemetry-service");
        
        // Check InfluxDB connection
        try {
            if (influxDBClient.ping()) {
                health.put("influxdb", "UP");
            } else {
                health.put("influxdb", "DOWN");
            }
        } catch (Exception e) {
            health.put("influxdb", "DOWN");
            health.put("influxdb_error", e.getMessage());
        }
        
        // Check Redis connection
        try {
            redisTemplate.opsForValue().get("health:check");
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN");
            health.put("redis_error", e.getMessage());
        }
        
        // Overall status
        if ("DOWN".equals(health.get("influxdb")) || "DOWN".equals(health.get("redis"))) {
            health.put("status", "DEGRADED");
        }
        
        return ResponseEntity.ok(health);
    }
} 