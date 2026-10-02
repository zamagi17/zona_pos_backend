package com.zonapos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "zona-pos-backend");
        status.put("timestamp", LocalDateTime.now());
        status.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        // Memory stats
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> memory = new HashMap<>();
        memory.put("usedMb", usedMemory);
        memory.put("totalMb", totalMemory);
        memory.put("maxMb", maxMemory);
        status.put("memory", memory);

        // Database Connectivity Health
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);
            if (isValid) {
                status.put("database", "UP");
                status.put("status", "HEALTHY");
                return ResponseEntity.ok(status);
            } else {
                status.put("database", "DOWN");
                status.put("status", "UNHEALTHY");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
            }
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("error", e.getMessage());
            status.put("status", "UNHEALTHY");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }
}
