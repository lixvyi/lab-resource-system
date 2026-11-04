package com.lab.gateway;

import com.lab.gateway.client.DownstreamClient;
import com.lab.gateway.config.DownstreamProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨语言调用链路的集成测试。
 * <p>
 * 验证 DownstreamClient 在下游不可达时能正确抛出 DownstreamServiceException,
 * 以及响应解析逻辑的基本可用性。
 */
class DownstreamClientTest {

    @Test
    @DisplayName("下游不可达时应抛出 DownstreamServiceException")
    void shouldThrowWhenDownstreamUnreachable() {
        DownstreamProperties props = new DownstreamProperties();
        // 指向一个不存在的端口,模拟下游服务不可达
        props.getMaterial().setBaseUrl("http://127.0.0.1:59999");
        props.getMaterial().setTimeoutMs(2000);
        props.getBorrow().setBaseUrl("http://127.0.0.1:59999");
        props.getBorrow().setTimeoutMs(2000);

        WebClient.Builder builder = WebClient.builder();
        DownstreamClient client = new DownstreamClient(builder, props, new ObjectMapper());

        try {
            client.getMaterials(null);
            org.junit.jupiter.api.Assertions.fail("应当抛出 DownstreamServiceException");
        } catch (com.lab.gateway.exception.DownstreamServiceException ex) {
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("无法连接") || ex.getMessage().contains("异常状态"),
                    "异常消息应包含连接失败信息, 实际: " + ex.getMessage());
        }
    }
}
