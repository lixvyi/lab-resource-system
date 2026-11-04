package com.lab.gateway.controller;

import com.lab.gateway.client.DownstreamClient;
import com.lab.gateway.entity.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 物资服务代理 Controller —— 转发到 Node.js/Express 服务。
 * <p>
 * 客户端访问 {@code http://localhost:8080/materials/...},
 * 网关内部通过 WebClient 调用 {@code http://localhost:8082/materials/...}。
 * 这样客户端只与 Java 网关打交道,实现了"统一入口"。
 */
@RestController
public class MaterialProxyController {

    private final DownstreamClient downstreamClient;

    public MaterialProxyController(DownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    /**
     * GET /materials
     * 代理查询物资列表
     */
    @GetMapping("/materials")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String storageRoom,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Map<String, String> params = new HashMap<>();
        putIfNotNull(params, "category", category);
        putIfNotNull(params, "status", status);
        putIfNotNull(params, "keyword", keyword);
        putIfNotNull(params, "storageRoom", storageRoom);
        putIfNotNull(params, "minQuantity", minQuantity);
        putIfNotNull(params, "maxQuantity", maxQuantity);
        putIfNotNull(params, "sortBy", sortBy);
        putIfNotNull(params, "order", order);
        params.put("page", String.valueOf(page));
        params.put("pageSize", String.valueOf(pageSize));

        Map<String, Object> data = downstreamClient.getMaterials(params);
        return ApiResponse.ok(data, "已通过 Java 网关调用 Node.js 物资服务");
    }

    /**
     * GET /materials/stats
     */
    @GetMapping("/materials/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> data = downstreamClient.getMaterialStats();
        return ApiResponse.ok(data, "已通过 Java 网关调用 Node.js 物资服务");
    }

    /**
     * GET /materials/{id}
     */
    @GetMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getById(@PathVariable Long id) {
        Map<String, Object> data = downstreamClient.getMaterialById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "已通过 Java 网关调用 Node.js 物资服务"));
    }

    private static void putIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            map.put(key, String.valueOf(value));
        }
    }
}
