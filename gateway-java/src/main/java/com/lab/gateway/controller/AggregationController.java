package com.lab.gateway.controller;

import com.lab.gateway.entity.ApiResponse;
import com.lab.gateway.service.AggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 聚合报表 Controller —— 跨语言数据聚合的统一入口。
 */
@RestController
public class AggregationController {

    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    /**
     * GET /dashboard/overview
     * 实验室资源总览(人员 + 物资 + 借用)
     */
    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = aggregationService.overview();
        return ApiResponse.ok(data, "实验室资源总览(聚合 Java + Node.js + Python 三方数据)");
    }

    /**
     * GET /dashboard/statistics
     * 三方统计聚合
     */
    @GetMapping("/dashboard/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        Map<String, Object> data = aggregationService.resourceStatistics();
        return ApiResponse.ok(data, "三方统计聚合完成");
    }

    /**
     * GET /personnels/{id}/borrows
     * 某个人的借用记录(Java 人员信息 + Python 借用数据)
     */
    @GetMapping("/personnels/{id}/borrows")
    public ApiResponse<Map<String, Object>> personnelBorrows(@PathVariable Long id) {
        Map<String, Object> data = aggregationService.personnelBorrows(id);
        return ApiResponse.ok(data, "已聚合 Java 人员信息与 Python 借用记录");
    }
}
