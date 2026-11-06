package com.susan.training.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MyBatis 多表查询返回的订单摘要。
 */
public record OrderSummaryRow(
        String orderNo,
        String customerNo,
        String customerName,
        BigDecimal amount,
        String status,
        long itemCount,
        long totalQuantity,
        Instant createdAt
) {
}
