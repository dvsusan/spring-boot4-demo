package com.susan.training.advanced.async;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 并行聚合订单详情需要的多个下游结果。
 */
@Service
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class OrderOverviewService {
    private final ExecutorService executor;
    private final Duration timeout;

    public OrderOverviewService(
            @Qualifier("chapter7Executor") ExecutorService executor,
            com.susan.training.advanced.Chapter7Properties properties
    ) {
        this.executor = executor;
        this.timeout = properties.getAsync().getTimeout();
    }

    public OrderOverview aggregate(String orderNo) {
        CompletableFuture<CustomerInfo> customer = async(() -> {
            sleep(35);
            return new CustomerInfo("C1001", "小明", "138****8001");
        });
        CompletableFuture<InventoryInfo> inventory = async(() -> {
            sleep(50);
            return new InventoryInfo("P1001", 19, true);
        });
        CompletableFuture<CouponInfo> coupon = async(() -> {
            sleep(45);
            return new CouponInfo("NEW20", 20);
        });
        CompletableFuture<RiskInfo> risk = async(() -> {
            sleep(30);
            return new RiskInfo("LOW", true);
        });

        try {
            CompletableFuture.allOf(customer, inventory, coupon, risk)
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .join();
            return new OrderOverview(
                    orderNo,
                    customer.join(),
                    inventory.join(),
                    coupon.join(),
                    risk.join(),
                    false
            );
        } catch (CompletionException exception) {
            // 聚合接口必须明确返回降级状态，不能只返回一个看似完整但缺字段的对象。
            return OrderOverview.unavailable(orderNo);
        }
    }

    private <T> CompletableFuture<T> async(AsyncTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            if (!Thread.currentThread().isVirtual()) {
                throw new IllegalStateException("订单聚合任务必须运行在虚拟线程");
            }
            return task.run();
        }, executor);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模拟下游调用被中断", exception);
        }
    }

    @FunctionalInterface
    private interface AsyncTask<T> {
        T run();
    }

    public record OrderOverview(
            String orderNo,
            CustomerInfo customer,
            InventoryInfo inventory,
            CouponInfo coupon,
            RiskInfo risk,
            boolean degraded
    ) {
        static OrderOverview unavailable(String orderNo) {
            return new OrderOverview(
                    orderNo,
                    null,
                    null,
                    null,
                    new RiskInfo("UNKNOWN", false),
                    true
            );
        }
    }

    public record CustomerInfo(String customerId, String name, String maskedPhone) {
    }

    public record InventoryInfo(String productId, int available, boolean sellable) {
    }

    public record CouponInfo(String code, int discount) {
    }

    public record RiskInfo(String level, boolean allowed) {
    }
}
