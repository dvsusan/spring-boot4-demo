package com.susan.training.order;

import java.math.BigDecimal;

/**
 * 订单查询视图，只暴露调用方需要看到的数据。
 */
public record OrderView(
        String orderNo,
        String customerId,
        BigDecimal amount,
        OrderStatus status
) {
}
