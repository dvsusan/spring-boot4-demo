package com.susan.training.security;

import java.math.BigDecimal;

/**
 * 经过行级过滤和列级脱敏后的订单视图。
 */
public record SecuredOrderView(
        String orderId,
        String departmentId,
        String ownerId,
        String customerPhone,
        BigDecimal amount,
        BigDecimal cost
) {
}
