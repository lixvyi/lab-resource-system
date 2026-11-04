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
 * 借用服务代理 Controller —— 转发到 Python/Flask 服务。
 * <p>
 * 客户端访问 {@code http://localhost:8080/borrows/...},
 * 网关内部通过 WebClient 调用 {@code http://localhost:8081/borrows/...}。
 */
@RestController
public class BorrowProxyController {

    private final DownstreamClient downstreamClient;

    public BorrowProxyController(DownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    /**
     * GET /borrows
     * 代理查询借用记录
     */
    @GetMapping("/borrows")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Map<String, String> params = new HashMap<>();
        if (personnelId != null) {
            params.put("personnelId", String.valueOf(personnelId));
        }
        if (materialId != null) {
            params.put("materialId", String.valueOf(materialId));
        }
        if (status != null && !status.isBlank()) {
            params.put("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            params.put("keyword", keyword);
        }
        params.put("page", String.valueOf(page));
        params.put("pageSize", String.valueOf(pageSize));

        Map<String, Object> data = downstreamClient.getBorrows(params);
        return ApiResponse.ok(data, "已通过 Java 网关调用 Python 借用服务");
    }

    /**
     * GET /borrows/stats
     */
    @GetMapping("/borrows/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> data = downstreamClient.getBorrowStats();
        return ApiResponse.ok(data, "已通过 Java 网关调用 Python 借用服务");
    }

    /**
     * GET /borrows/{id}
     */
    @GetMapping("/borrows/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getById(@PathVariable Long id) {
        Map<String, Object> data = downstreamClient.getBorrowById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "已通过 Java 网关调用 Python 借用服务"));
    }
}
