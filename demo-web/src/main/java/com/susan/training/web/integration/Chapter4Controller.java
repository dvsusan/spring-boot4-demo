package com.susan.training.web.integration;

import com.susan.training.common.ApiResponse;
import com.susan.training.integration.cache.CacheConsistencyService;
import com.susan.training.integration.cache.OrderCacheService;
import com.susan.training.integration.lock.OrderLockService;
import com.susan.training.integration.message.OrderMessagePublisher;
import com.susan.training.integration.outbox.OutboxMessageEntity;
import com.susan.training.integration.outbox.OutboxService;
import com.susan.training.integration.search.OrderSearchDocument;
import com.susan.training.integration.search.OrderSearchService;
import com.susan.training.order.CreateOrderCommand;
import com.susan.training.order.OrderService;
import com.susan.training.order.OrderView;
import com.susan.training.web.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 第 22–28 篇使用的可选中间件演示接口。
 *
 * <p>服务未开启时返回明确的 FEATURE_DISABLED，而不是让整个应用启动失败。</p>
 */
@RestController
@RequestMapping("/api/v1/chapter4")
public class Chapter4Controller {
    private final OrderService orderService;
    private final OutboxService outboxService;
    private final ObjectProvider<OrderCacheService> cacheProvider;
    private final ObjectProvider<CacheConsistencyService> consistencyProvider;
    private final ObjectProvider<OrderLockService> lockProvider;
    private final ObjectProvider<OrderMessagePublisher> messageProvider;
    private final ObjectProvider<OrderSearchService> searchProvider;

    public Chapter4Controller(
            OrderService orderService,
            OutboxService outboxService,
            ObjectProvider<OrderCacheService> cacheProvider,
            ObjectProvider<CacheConsistencyService> consistencyProvider,
            ObjectProvider<OrderLockService> lockProvider,
            ObjectProvider<OrderMessagePublisher> messageProvider,
            ObjectProvider<OrderSearchService> searchProvider
    ) {
        this.orderService = orderService;
        this.outboxService = outboxService;
        this.cacheProvider = cacheProvider;
        this.consistencyProvider = consistencyProvider;
        this.lockProvider = lockProvider;
        this.messageProvider = messageProvider;
        this.searchProvider = searchProvider;
    }

    /**
     * 读穿缓存：缓存未命中时从当前订单服务加载。
     */
    @GetMapping("/cache/orders/{orderNo}")
    public ApiResponse<OrderView> cachedOrder(@PathVariable String orderNo) {
        OrderCacheService cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Redis 缓存未开启");
        }
        return ApiResponse.success(
                cache.getOrLoad(orderNo, () -> orderService.findByOrderNo(orderNo))
        );
    }

    /**
     * 创建订单后立即删除旧缓存。
     */
    @PostMapping("/cache/orders")
    public ApiResponse<OrderView> createAndEvict(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CacheConsistencyService service = consistencyProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "缓存一致性演示未开启");
        }
        return ApiResponse.success(service.create(
                new CreateOrderCommand(request.customerId(), request.amount())
        ));
    }

    /**
     * 在 Redisson 分布式锁内执行订单操作。
     */
    @PostMapping("/locks/orders/{orderNo}")
    public ApiResponse<String> lockOrder(@PathVariable String orderNo) {
        OrderLockService service = lockProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Redisson 分布式锁未开启");
        }
        return ApiResponse.success(service.execute(
                orderNo,
                Duration.ofSeconds(2),
                () -> "lock acquired for " + orderNo
        ));
    }

    /**
     * 发送一条订单事件，便于观察 RocketMQ 生产者配置。
     */
    @PostMapping("/messages/orders/{orderNo}")
    public ApiResponse<String> publishOrderEvent(@PathVariable String orderNo) throws Exception {
        OrderMessagePublisher publisher = messageProvider.getIfAvailable();
        if (publisher == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "RocketMQ 未开启");
        }
        String eventId = UUID.randomUUID().toString();
        String payload = "{\"eventId\":\"" + eventId
                + "\",\"orderNo\":\"" + orderNo + "\"}";
        return ApiResponse.success(
                publisher.publish("ORDER_CREATED", payload).getMsgId()
        );
    }

    /**
     * 写入一条 Outbox 消息。
     */
    @PostMapping("/outbox")
    public ApiResponse<OutboxMessageEntity> appendOutbox(
            @RequestParam String eventId,
            @RequestParam String aggregateId,
            @RequestParam(defaultValue = "ORDER_CREATED") String eventType,
            @RequestParam String payload
    ) {
        return ApiResponse.success(outboxService.append(
                eventId,
                aggregateId,
                eventType,
                payload
        ));
    }

    /**
     * 查看当前尚未投递的消息。
     */
    @GetMapping("/outbox")
    public ApiResponse<List<OutboxMessageEntity>> pendingOutbox() {
        return ApiResponse.success(outboxService.pending());
    }

    /**
     * 把订单写入 Elasticsearch。
     */
    @PostMapping("/search/orders/{orderNo}")
    public ApiResponse<OrderSearchDocument> indexOrder(@PathVariable String orderNo) {
        OrderSearchService service = searchProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Elasticsearch 未开启");
        }
        OrderView order = orderService.findByOrderNo(orderNo);
        return ApiResponse.success(service.save(new OrderSearchDocument(
                order.orderNo(),
                order.customerId(),
                order.amount(),
                order.status().name(),
                Instant.now()
        )));
    }

    /**
     * 从 Elasticsearch 读取订单文档。
     */
    @GetMapping("/search/orders/{orderNo}")
    public ApiResponse<OrderSearchDocument> searchOrder(@PathVariable String orderNo) {
        OrderSearchService service = searchProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Elasticsearch 未开启");
        }
        return service.find(orderNo)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.failure("ORDER_NOT_FOUND", "搜索文档不存在"));
    }
}
