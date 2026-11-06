package com.susan.training.config;

import com.susan.training.order.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web 层的 Bean 装配配置。
 *
 * <p>订单模块保持普通 Java，由这里把 OrderService 加入 Spring 容器。</p>
 */
@Configuration
public class OrderConfiguration {

    /**
     * 创建订单服务实例，供 Controller 通过构造器注入。
     */
    @Bean
    public OrderService orderService() {
        return new OrderService();
    }
}
