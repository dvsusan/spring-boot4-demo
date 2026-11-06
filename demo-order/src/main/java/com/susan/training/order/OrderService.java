package com.susan.training.order;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单领域服务。
 *
 * <p>本阶段使用内存 Map 保存数据，让读者先把多模块和 Bean 装配跑通。
 * 后续接入数据库时，只需要替换这个模块内部的持久化实现。</p>
 */
public class OrderService {
    // 使用并发 Map 只是为了避免示例被并发请求破坏，它不能替代数据库。
    private final Map<String, OrderView> orders = new ConcurrentHashMap<>();

    /**
     * 创建订单并保存到当前进程的内存中。
     */
    public OrderView create(CreateOrderCommand command) {
        // UUID 片段仅用于教学演示，生产订单号还要考虑唯一性、可读性和防猜测。
        String orderNo = "SO" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);

        // 新订单从 CREATED 状态开始。
        OrderView order = new OrderView(
                orderNo,
                command.customerId(),
                command.amount(),
                OrderStatus.CREATED
        );
        orders.put(orderNo, order);
        return order;
    }

    /**
     * 根据订单号查询订单，找不到时抛出业务异常。
     */
    public OrderView findByOrderNo(String orderNo) {
        OrderView order = orders.get(orderNo);
        if (order == null) {
            throw new OrderNotFoundException(orderNo);
        }
        return order;
    }

    /**
     * 按客户编号分页查询，limit 由 Web 层校验并限制上限。
     */
    public List<OrderView> findByCustomerId(String customerId, int limit) {
        return orders.values()
                .stream()
                .filter(order -> order.customerId().equals(customerId))
                .sorted((left, right) -> left.orderNo().compareTo(right.orderNo()))
                .limit(limit)
                .toList();
    }

    /**
     * 更新订单金额；customerId 为空时保留原客户，避免 PATCH 请求覆盖未提交字段。
     */
    public OrderView update(String orderNo, String customerId, java.math.BigDecimal amount) {
        OrderView current = findByOrderNo(orderNo);
        String nextCustomerId = customerId == null || customerId.isBlank()
                ? current.customerId()
                : customerId;
        OrderView updated = new OrderView(
                current.orderNo(),
                nextCustomerId,
                amount,
                current.status()
        );
        orders.put(orderNo, updated);
        return updated;
    }
}
