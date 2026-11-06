package com.susan.training.integration.cache;

import com.susan.training.order.CreateOrderCommand;
import com.susan.training.order.OrderService;
import com.susan.training.order.OrderView;

/**
 * 演示“更新数据后删除缓存”的最小一致性流程。
 *
 * <p>当前 OrderService 仍是内存实现；接入数据库时，应把业务写入与
 * 提交后缓存失效放进同一个应用事务边界。</p>
 */
public class CacheConsistencyService {
    private final OrderService orderService;
    private final OrderCacheService cacheService;

    public CacheConsistencyService(
            OrderService orderService,
            OrderCacheService cacheService
    ) {
        this.orderService = orderService;
        this.cacheService = cacheService;
    }

    public OrderView create(CreateOrderCommand command) {
        OrderView order = orderService.create(command);
        // 删除旧缓存，不能覆盖并发读者可能写入的更新值。
        cacheService.evict(order.orderNo());
        return order;
    }
}
