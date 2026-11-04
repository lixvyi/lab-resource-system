package com.lab.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 下游服务配置绑定。
 * <p>
 * 绑定 application.yml 中的 {@code lab.services.*} 配置节,
 * 通过类型安全的 getter 注入 {@link com.lab.gateway.client.DownstreamClient}。
 */
@Component
@ConfigurationProperties(prefix = "lab.services")
public class DownstreamProperties {

    private ServiceConfig material = new ServiceConfig();
    private ServiceConfig borrow = new ServiceConfig();

    public ServiceConfig getMaterial() {
        return material;
    }

    public void setMaterial(ServiceConfig material) {
        this.material = material;
    }

    public ServiceConfig getBorrow() {
        return borrow;
    }

    public void setBorrow(ServiceConfig borrow) {
        this.borrow = borrow;
    }

    /**
     * 单个下游服务配置
     */
    public static class ServiceConfig {
        /** 服务基地址,如 http://localhost:8082 */
        private String baseUrl;
        /** 超时时间(毫秒) */
        private int timeoutMs = 5000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
