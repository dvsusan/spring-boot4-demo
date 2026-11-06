package com.susan.training.config;

import com.susan.training.client.OrderApiClient;
import com.susan.training.client.OrderClientProperties;
import com.susan.training.client.OrderHttpClientFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 注册可在其他 Service 中直接注入的订单 HTTP 客户端。
 */
@Configuration
@EnableConfigurationProperties(OrderClientProperties.class)
public class OrderClientConfiguration {

    @Bean
    public OrderApiClient orderApiClient(
            OrderClientProperties properties,
            OrderHttpClientFactory factory,
            RestClient.Builder builder
    ) {
        return factory.create(properties.getBaseUrl(), builder, properties);
    }
}
