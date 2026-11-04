package com.lab.gateway.controller;

import com.lab.gateway.config.DownstreamProperties;
import com.lab.gateway.entity.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查 Controller —— 同时探测自身数据库与两个下游异构服务。
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final DownstreamProperties properties;

    public HealthController(JdbcTemplate jdbcTemplate, DownstreamProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * GET /health
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "gateway-java");
        data.put("version", "1.0.0");
        data.put("port", 8080);
        data.put("database", checkDatabase());
        data.put("downstream", checkDownstream());
        data.put("timestamp", java.time.LocalDateTime.now().toString());
        return ApiResponse.ok(data, "gateway-java 存活");
    }

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    private Map<String, String> checkDownstream() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("material-service(Node.js/8082)", probe(properties.getMaterial().getBaseUrl()));
        result.put("borrow-service(Python/8081)", probe(properties.getBorrow().getBaseUrl()));
        return result;
    }

    private String probe(String baseUrl) {
        try {
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) java.net.URI.create(baseUrl + "/health").toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200 ? "UP" : "HTTP " + code;
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }
}
