package com.warmhouse.telemetry.config;

import com.warmhouse.telemetry.events.TelemetryEventListener;
import com.warmhouse.telemetry.service.DeviceValidationService;
import com.warmhouse.telemetry.service.TelemetryCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class EventListenerConfig {
    
    private static final Logger log = LoggerFactory.getLogger(EventListenerConfig.class);
    
    public EventListenerConfig() {
        log.info("EventListenerConfig constructor called");
    }
    
    @Bean
    public TelemetryEventListener telemetryEventListener(DeviceValidationService deviceValidationService,
                                                         RedisTemplate<String, Object> redisTemplate,
                                                         TelemetryCleanupService cleanupService) {
        log.info("Creating TelemetryEventListener bean explicitly");
        return new TelemetryEventListener(deviceValidationService, redisTemplate, cleanupService);
    }
} 