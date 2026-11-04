package com.lab.gateway.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.gateway.config.DownstreamProperties;
import com.lab.gateway.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 跨语言服务调用客户端(Java -> Node.js / Python)
 * ---------------------------------------------------------------
 * <p>这是本实验"跨语言调用"的核心实现:</p>
 * <ul>
 *   <li>使用 Spring {@link WebClient}(响应式,基于 Reactor Netty)发起 HTTP 调用;</li>
 *   <li>把 Node.js(Express)与 Python(Flask)的 JSON 响应统一反序列化为
 *       {@code Map<String,Object>},屏蔽不同语言序列化实现的差异;</li>
 *   <li>下游服务不可用时抛出 {@link DownstreamServiceException},
 *       由 {@code GlobalExceptionHandler} 转换为 502 Bad Gateway。</li>
 * </ul>
 *
 * <p><b>协议约定</b>:三个服务统一使用
 * {@code {"code":0,"message":"...","data":...}} 信封格式,
 * 因此网关可以直接把下游的 data 字段透传给客户端。</p>
 */
@Component
public class DownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(DownstreamClient.class);

    private final WebClient webClient;
    private final DownstreamProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数。
     * <p>
     * 注入的是默认的 {@code WebClient.Builder}(带连接/读超时),
     * 下游服务通过绝对地址直连,不经过服务注册中心。
     */
    public DownstreamClient(WebClient.Builder builder,
                            DownstreamProperties properties,
                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = builder
                .defaultHeader("X-Caller-Language", "Java/SpringBoot")
                .defaultHeader("X-Caller-Service", "gateway-java")
                .build();
    }

    /**
     * 调用 Node.js 物资服务:GET /materials
     *
     * @param queryParams 查询参数(可为空)
     * @return 下游返回的 data 字段(通常是含 items/total 的分页对象)
     */
    public Map<String, Object> getMaterials(Map<String, String> queryParams) {
        return get(properties.getMaterial().getBaseUrl() + "/materials", queryParams, "Node.js 物资服务");
    }

    /**
     * 调用 Node.js 物资服务:GET /materials/stats
     */
    public Map<String, Object> getMaterialStats() {
        return get(properties.getMaterial().getBaseUrl() + "/materials/stats", null, "Node.js 物资服务");
    }

    /**
     * 调用 Node.js 物资服务:GET /materials/{id}
     */
    public Map<String, Object> getMaterialById(Long id) {
        return get(properties.getMaterial().getBaseUrl() + "/materials/" + id, null, "Node.js 物资服务");
    }

    /**
     * 调用 Python 借用服务:GET /borrows
     */
    public Map<String, Object> getBorrows(Map<String, String> queryParams) {
        return get(properties.getBorrow().getBaseUrl() + "/borrows", queryParams, "Python 借用服务");
    }

    /**
     * 调用 Python 借用服务:GET /borrows/{id}
     */
    public Map<String, Object> getBorrowById(Long id) {
        return get(properties.getBorrow().getBaseUrl() + "/borrows/" + id, null, "Python 借用服务");
    }

    /**
     * 调用 Python 借用服务:GET /borrows/stats
     */
    public Map<String, Object> getBorrowStats() {
        return get(properties.getBorrow().getBaseUrl() + "/borrows/stats", null, "Python 借用服务");
    }

    // ------------------------------------------------------------------
    // 内部通用 GET 方法
    // ------------------------------------------------------------------
    private Map<String, Object> get(String url, Map<String, String> queryParams, String serviceName) {
        // 组装完整 URI(含查询参数)
        StringBuilder fullUrl = new StringBuilder(url);
        if (queryParams != null && !queryParams.isEmpty()) {
            fullUrl.append('?');
            boolean first = true;
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (!first) {
                    fullUrl.append('&');
                }
                fullUrl.append(UriUtils.encodeQueryParam(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(UriUtils.encodeQueryParam(e.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        String requestUrl = fullUrl.toString();

        try {
            String body = webClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.getMaterial().getTimeoutMs()))
                    .block();

            log.info("[跨语言调用] GET {} -> 200, 响应长度 {}", requestUrl, body == null ? 0 : body.length());
            return extractData(body, serviceName);

        } catch (WebClientResponseException e) {
            log.error("[跨语言调用] GET {} -> 下游返回异常状态 {}", requestUrl, e.getStatusCode());
            throw new DownstreamServiceException(
                    serviceName + " 返回异常状态: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[跨语言调用] GET {} 调用失败: {}", requestUrl, e.getMessage());
            throw new DownstreamServiceException(
                    "无法连接 " + serviceName + "(" + requestUrl + "): " + e.getMessage(), e);
        }
    }

    /**
     * 从下游响应体中提取 data 字段。
     * <p>
     * 下游统一信封为 {@code {"code":0,"message":"...","data":...}},
     * 这里用 Jackson 解析后取出 data;若结构不符合预期则原样返回整个 body 的 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(String body, String serviceName) {
        if (body == null || body.isBlank()) {
            throw new DownstreamServiceException(serviceName + " 返回了空响应体");
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(
                    body, new TypeReference<Map<String, Object>>() {
                    });
            Object data = envelope.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
            // data 不是对象时,构造一个包装 Map 保持返回类型一致
            Map<String, Object> wrapper = new java.util.LinkedHashMap<>();
            wrapper.put("value", data);
            return wrapper;
        } catch (Exception e) {
            throw new DownstreamServiceException(
                    "解析 " + serviceName + " 响应失败: " + e.getMessage(), e);
        }
    }
}
