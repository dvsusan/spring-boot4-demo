package com.susan.training.persistence.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 数据库订单详情 DTO，Entity 不直接暴露给 HTTP。
 */
public record PersistentOrderView(
        String orderNo,
        String customerNo,
        String customerName,
        BigDecimal amount,
        String status,
        List<Item> items,
        Map<String, String> metadata,
        Instant createdAt
) {
    /**
     * 下单时冻结的商品快照。
     */
    public record Item(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}
