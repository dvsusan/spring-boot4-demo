package com.susan.training.persistence.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据库订单创建命令，金额由服务端按明细重新计算。
 */
public record PlaceOrderCommand(
        String tenantId,
        String customerNo,
        String customerName,
        List<PlaceOrderItem> items,
        Map<String, String> metadata
) {
    /**
     * 单条下单明细。
     */
    public record PlaceOrderItem(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}
