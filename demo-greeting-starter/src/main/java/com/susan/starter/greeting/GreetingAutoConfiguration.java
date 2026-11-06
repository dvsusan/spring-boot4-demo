package com.susan.starter.greeting;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自定义问候 Starter 的自动配置入口。
 *
 * <p>@AutoConfiguration 表示该类由 Spring Boot 的自动配置机制加载。
 * 条件注解让使用方可以通过配置关闭 Starter，也可以自己提供
 * GreetingService 覆盖默认实现。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(GreetingProperties.class)
@ConditionalOnProperty(
        prefix = "demo.greeting",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GreetingAutoConfiguration {

    /**
     * 只有在容器里还没有 GreetingService 时才创建默认实例。
     */
    @Bean
    @ConditionalOnMissingBean
    public GreetingService greetingService(GreetingProperties properties) {
        return new GreetingService(properties);
    }
}
