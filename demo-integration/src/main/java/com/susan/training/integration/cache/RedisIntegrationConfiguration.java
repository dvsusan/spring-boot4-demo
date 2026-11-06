package com.susan.training.integration.cache;

import com.susan.training.order.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 按需开启 Redis 缓存 Bean，默认配置不需要连接 Redis。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter4.redis",
        name = "enabled",
        havingValue = "true"
)
public class RedisIntegrationConfiguration {

    @Bean
    public OrderCacheService orderCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${demo.chapter4.redis.ttl:5m}") Duration ttl
    ) {
        return new OrderCacheService(redis, objectMapper, ttl);
    }

    @Bean
    public CacheConsistencyService cacheConsistencyService(
            OrderService orderService,
            OrderCacheService orderCacheService
    ) {
        return new CacheConsistencyService(orderService, orderCacheService);
    }
}
