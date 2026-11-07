package com.susan.training.advanced.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 为订单风控调用组合限流、重试和熔断。
 */
@Service
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class RiskAssessmentService {
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;

    public RiskAssessmentService(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry
    ) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("risk-service");
        this.retry = retryRegistry.retry("risk-service");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("chapter7-risk");
    }

    public RiskResult assess(String orderNo, boolean fail) {
        if (!rateLimiter.acquirePermission()) {
            return RiskResult.fallback(
                    orderNo,
                    "RATE_LIMITED",
                    circuitBreaker.getState().name()
            );
        }

        Supplier<RiskResult> downstream = () -> {
            if (fail) {
                throw new IllegalStateException("风控服务超时");
            }
            return new RiskResult(
                    orderNo,
                    "LOW",
                    true,
                    false,
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                    circuitBreaker.getState().name()
            );
        };
        Supplier<RiskResult> protectedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, downstream)
        );

        try {
            return protectedCall.get();
        } catch (CallNotPermittedException exception) {
            return RiskResult.fallback(
                    orderNo,
                    "CIRCUIT_OPEN",
                    circuitBreaker.getState().name()
            );
        } catch (RequestNotPermitted exception) {
            return RiskResult.fallback(
                    orderNo,
                    "RATE_LIMITED",
                    circuitBreaker.getState().name()
            );
        } catch (RuntimeException exception) {
            return RiskResult.fallback(
                    orderNo,
                    "DOWNSTREAM_FAILED",
                    circuitBreaker.getState().name()
            );
        }
    }

    public String circuitState() {
        return circuitBreaker.getState().name();
    }

    public record RiskResult(
            String orderNo,
            String level,
            boolean allowed,
            boolean fallback,
            long successfulCalls,
            String circuitState
    ) {
        static RiskResult fallback(
                String orderNo,
                String reason,
                String circuitState
        ) {
            return new RiskResult(
                    orderNo,
                    reason,
                    false,
                    true,
                    0,
                    circuitState
            );
        }
    }
}
