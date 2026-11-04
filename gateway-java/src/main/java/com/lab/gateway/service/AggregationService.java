package com.lab.gateway.service;

import com.lab.gateway.client.DownstreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跨模块聚合服务。
 * <p>
 * 网关作为唯一入口,把分散在三种语言实现中的数据聚合成一份视图,
 * 这正是"统一 REST 网关"的核心价值 —— 客户端无需感知底层实现的语言差异。
 */
@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final PersonnelService personnelService;
    private final DownstreamClient downstreamClient;

    public AggregationService(PersonnelService personnelService,
                              DownstreamClient downstreamClient) {
        this.personnelService = personnelService;
        this.downstreamClient = downstreamClient;
    }

    /**
     * 实验室资源总览:一次聚合人员、物资、借用三类数据。
     * <p>
     * 三个数据源分别来自 Java 本地业务、Node.js 服务、Python 服务,
     * 体现了跨语言协作的完整链路。
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("personnel", personnelService.list(null, null, null, null, 1, 5));
        result.put("materials", safeCall("materials", () -> downstreamClient.getMaterials(null)));
        result.put("borrows", safeCall("borrows", () -> downstreamClient.getBorrows(null)));
        return result;
    }

    /**
     * 物资与借用的联合统计(物资来自 Node.js,借用来自 Python)。
     */
    public Map<String, Object> resourceStatistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("personnel", personnelService.statistics());
        result.put("materials", safeCall("materials", () -> downstreamClient.getMaterialStats()));
        result.put("borrows", safeCall("borrows", () -> downstreamClient.getBorrowStats()));
        return result;
    }

    /**
     * 查询某个人员的借用记录(本地人员信息 + Python 借用服务数据)。
     */
    public Map<String, Object> personnelBorrows(Long personnelId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("personnel", personnelService.getById(personnelId).orElse(null));
        result.put("borrows", safeCall("borrows",
                () -> downstreamClient.getBorrows(Map.of("personnelId", String.valueOf(personnelId)))));
        return result;
    }

    /**
     * 统一的降级处理:下游不可用时不中断整个聚合,返回错误占位。
     */
    private Map<String, Object> safeCall(String key, java.util.function.Supplier<Map<String, Object>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("[聚合] 调用 {} 失败: {}", key, e.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("available", false);
            fallback.put("error", e.getMessage());
            return fallback;
        }
    }
}
