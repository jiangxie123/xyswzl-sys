package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Result<Map<String, Object>> check() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "UP");
            data.put("timestamp", System.currentTimeMillis());
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            data.put("database", "connected");
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "DOWN");
            data.put("error", e.getMessage());
            return Result.success(data);
        }
    }
}
