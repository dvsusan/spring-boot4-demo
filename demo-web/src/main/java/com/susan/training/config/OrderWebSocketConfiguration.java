package com.susan.training.config;

import com.susan.training.web.stream.OrderWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册原生 WebSocket 端点。
 */
@Configuration
@EnableWebSocket
public class OrderWebSocketConfiguration implements WebSocketConfigurer {
    private final OrderWebSocketHandler orderWebSocketHandler;

    public OrderWebSocketConfiguration(OrderWebSocketHandler orderWebSocketHandler) {
        this.orderWebSocketHandler = orderWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 示例允许本地页面连接；生产环境应限制为明确的前端域名。
        registry.addHandler(orderWebSocketHandler, "/ws/orders")
                .setAllowedOriginPatterns("*");
    }
}
