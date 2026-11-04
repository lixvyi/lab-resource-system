package com.lab.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient / HttpClient 基础设施配置。
 * <p>
 * 显式配置连接超时与读超时,保证在下游服务(Node.js / Python)
 * 无响应时网关能快速失败并返回 502,而不是长时间挂起。
 */
@Configuration
public class WebClientConfig {

    /**
     * 构建带超时策略的 HttpClient。
     */
    @Bean
    public HttpClient labHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(5, TimeUnit.SECONDS)));
    }

    /**
     * WebClient 连接器(供 GatewayApplication 中的 Builder 使用)。
     */
    @Bean
    public ReactorClientHttpConnector labHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }
}
