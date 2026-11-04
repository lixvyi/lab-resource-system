package com.lab.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
        // 实验要求网关固定 8080。部分运行环境会注入 SERVER_PORT=0，
        // Spring Boot 会把它映射为随机端口(如 57114)。系统属性优先于环境变量。
        if (System.getProperty("server.port") == null) {
            System.setProperty("server.port", "8080");
        }
        SpringApplication.run(GatewayApplication.class, args);
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
