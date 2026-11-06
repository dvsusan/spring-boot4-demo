package com.susan.training.integration.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端只在显式开启分布式锁章节时连接 Redis。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter4.redisson",
        name = "enabled",
        havingValue = "true"
)
public class RedissonIntegrationConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${demo.chapter4.redisson.address:redis://127.0.0.1:6379}")
            String address
    ) {
        Config config = new Config();
        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }

    @Bean
    public OrderLockService orderLockService(RedissonClient redissonClient) {
        return new OrderLockService(redissonClient);
    }
}
