package com.susan.training.advanced.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 订单查询指标和业务 Observation 示例。
 */
@Service
@ConditionalOnProperty(
        prefix = "demo.chapter8",
        name = "enabled",
        havingValue = "true"
)
public class Chapter8MonitoringService {
    private final Counter orderQueries;
    private final Timer queryLatency;
    private final ObservationRegistry observationRegistry;

    public Chapter8MonitoringService(
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry
    ) {
        this.orderQueries = Counter.builder("chapter8.orders")
                .description("订单可观测性示例查询次数")
                .tag("status", "SUCCESS")
                .register(meterRegistry);
        this.queryLatency = Timer.builder("chapter8.direct.lookup")
                .description("订单查询示例耗时")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.observationRegistry = observationRegistry;
    }

    public OrderObservation observe(String orderNo) {
        return Observation.createNotStarted(
                        "chapter8.order.lookup",
                        observationRegistry
                )
                .lowCardinalityKeyValue("order.type", "demo")
                .observe(() -> {
                    long startedAt = System.nanoTime();
                    // 这里只模拟查询耗时，真实项目应把数据库和远程调用纳入 Observation。
                    sleep(Duration.ofMillis(15));
                    orderQueries.increment();
                    queryLatency.record(
                            System.nanoTime() - startedAt,
                            TimeUnit.NANOSECONDS
                    );
                    return new OrderObservation(
                            orderNo,
                            "OBSERVED",
                            "chapter8.order.lookup"
                    );
                });
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("可观测性示例被中断", exception);
        }
    }

    public record OrderObservation(
            String orderNo,
            String status,
            String observationName
    ) {
    }
}
