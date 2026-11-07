package com.susan.training.advanced;

import com.susan.training.advanced.ai.DemoChatModel;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 第七章组件只在启用 chapter7 Profile 时创建。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Chapter7Properties.class)
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class Chapter7Configuration {

    @Bean(destroyMethod = "close")
    public ExecutorService chapter7Executor() {
        // 虚拟线程适合大量阻塞式下游调用，但不能绕过连接池和下游限流。
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public CircuitBreakerRegistry chapter7CircuitBreakerRegistry(
            @Value("${resilience4j.circuitbreaker.instances.risk-service.sliding-window-size:4}")
            int slidingWindowSize,
            @Value("${resilience4j.circuitbreaker.instances.risk-service.minimum-number-of-calls:4}")
            int minimumCalls,
            @Value("${resilience4j.circuitbreaker.instances.risk-service.failure-rate-threshold:50}")
            float failureRate,
            @Value("${resilience4j.circuitbreaker.instances.risk-service.wait-duration-in-open-state:10s}")
            Duration waitDuration
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureRate)
                .waitDurationInOpenState(waitDuration)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry chapter7RetryRegistry(
            @Value("${resilience4j.retry.instances.risk-service.max-attempts:3}")
            int maxAttempts,
            @Value("${resilience4j.retry.instances.risk-service.wait-duration:100ms}")
            Duration waitDuration
    ) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(waitDuration)
                .retryExceptions(IllegalStateException.class)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public RateLimiterRegistry chapter7RateLimiterRegistry(
            @Value("${resilience4j.ratelimiter.instances.chapter7-risk.limit-for-period:5}")
            int limitForPeriod,
            @Value("${resilience4j.ratelimiter.instances.chapter7-risk.limit-refresh-period:1s}")
            Duration refreshPeriod,
            @Value("${resilience4j.ratelimiter.instances.chapter7-risk.timeout-duration:100ms}")
            Duration timeoutDuration
    ) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(refreshPeriod)
                .timeoutDuration(timeoutDuration)
                .build();
        return RateLimiterRegistry.of(config);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "demo.chapter7.ai",
            name = "remote-enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatModel demoChatModel(Chapter7Properties properties) {
        // 没有模型密钥时仍能运行完整提示词和响应链路。
        return new DemoChatModel(properties.getAi().getModel());
    }

    @Bean
    public ChatClient chapter7ChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
