package com.lab.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 跨语言调用的实验室资源管理系统 —— Java 网关服务启动类
 * <p>
 * 职责:
 * <ul>
 *   <li>以 REST API 形式统一暴露 /personnels(本地业务 + MySQL 持久化)</li>
 *   <li>通过 WebClient 跨语言调用 Node.js 物资服务(8082)与 Python 借用服务(8081)</li>
 * </ul>
 * 该应用是整个系统的"单一入口",客户端只与 Java 网关交互,
 * 由网关完成对异构语言微服务的编排与聚合。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        // 实验要求网关固定 8080。部分 IDE / 代理会注入 SERVER_PORT=0 或
        // --server.port=0，Spring Boot 会把它映射成随机端口(如 57114)。
        System.setProperty("server.port", "8080");
        String[] filtered = java.util.Arrays.stream(args == null ? new String[0] : args)
                .filter(a -> a != null && !a.startsWith("--server.port") && !a.startsWith("--SERVER_PORT"))
                .toArray(String[]::new);
        SpringApplication app = new SpringApplication(GatewayApplication.class);
        app.setDefaultProperties(java.util.Map.of("server.port", "8080"));
        app.run(filtered);
    }

    /**
     * 在全部配置源（含 SERVER_PORT=0、--server.port=0）绑定完成之后再写死端口。
     * 默认的 ServletWebServerFactoryCustomizer 顺序是 0，这里用最低优先级盖掉它，
     * 避免 IDE 把 Tomcat 绑到随机端口（例如 57114）。
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> pinGatewayPort() {
        return factory -> factory.setPort(8080);
    }

    /**
     * 默认的 WebClient.Builder(带超时配置,见 {@link com.lab.gateway.config.WebClientConfig})。
     * <p>
     * 下游 Node.js / Python 服务通过绝对地址直连,不经过服务注册中心,
     * 因此不需要 {@code @LoadBalanced} 注解。
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            org.springframework.http.client.reactive.ReactorClientHttpConnector connector) {
        return WebClient.builder().clientConnector(connector);
    }
}
