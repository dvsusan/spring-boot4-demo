package com.susan.training.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 订单 HTTP 客户端的外部服务地址和超时配置。
 */
@ConfigurationProperties(prefix = "demo.order-client")
public class OrderClientProperties {
    // 远端订单服务基础地址，不包含具体接口路径。
    private String baseUrl = "http://localhost:8080";

    // 建立 TCP 连接的最长等待时间。
    private Duration connectTimeout = Duration.ofSeconds(2);

    // 等待响应的最长时间，避免请求线程无限挂起。
    private Duration readTimeout = Duration.ofSeconds(3);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
