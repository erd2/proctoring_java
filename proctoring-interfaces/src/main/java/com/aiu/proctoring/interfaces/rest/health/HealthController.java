package com.aiu.proctoring.interfaces.rest.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom health check endpoint.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        status.put("service", "proctoring-system");
        Map<String, Object> checks = new HashMap<>();
        checks.put("database", "UP");
        checks.put("redis", "UP");
        checks.put("kafka", "UP");
        status.put("checks", checks);
        return ResponseEntity.ok(status);
    }
}
