package com.warmhouse.telemetry.controller;

import com.warmhouse.telemetry.service.TelemetryCleanupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for cleanup operations
 */
@RestController
@RequestMapping("/api/v1/cleanup")
@RequiredArgsConstructor
public class CleanupController {
    
    private static final Logger log = LoggerFactory.getLogger(CleanupController.class);
    
    private final TelemetryCleanupService cleanupService;
    
    /**
     * Clean up all telemetry data for a specific device
     * DELETE /api/v1/cleanup/device/{deviceId}
     */
    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> cleanupDeviceData(@PathVariable UUID deviceId) {
        log.info("Cleanup request received for device: {}", deviceId);
        
        try {
            boolean success = cleanupService.cleanupDeviceData(deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("device_id", deviceId.toString());
            response.put("status", success ? "success" : "partial_failure");
            response.put("message", success ? 
                "Successfully cleaned up all data for device" : 
                "Cleanup completed with some errors");
            
            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(207).body(response); // 207 Multi-Status
            }
            
        } catch (Exception e) {
            log.error("Error during device cleanup for {}: {}", deviceId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("device_id", deviceId.toString());
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to cleanup device data: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Clean up old telemetry data based on retention policy
     * POST /api/v1/cleanup/retention
     */
    @PostMapping("/retention")
    public ResponseEntity<Map<String, Object>> cleanupOldData(
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        log.info("Retention cleanup request received for {} days", retentionDays);
        
        if (retentionDays < 1) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Retention days must be at least 1");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            long deletedCount = cleanupService.cleanupOldData(retentionDays);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("retention_days", retentionDays);
            response.put("deleted_count", deletedCount);
            response.put("message", String.format("Successfully cleaned up data older than %d days", retentionDays));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during retention cleanup: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("retention_days", retentionDays);
            errorResponse.put("message", "Failed to cleanup old data: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check for cleanup service
     * GET /api/v1/cleanup/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "telemetry-cleanup");
        response.put("message", "Cleanup service is operational");
        
        return ResponseEntity.ok(response);
    }
} 