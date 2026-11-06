package com.susan.training.security;

import java.math.BigDecimal;

/**
 * 数据权限教学使用的订单原始数据。
 */
public record SecuredOrderRecord(
        String orderId,
        String tenantId,
        String departmentId,
        String ownerId,
        String customerPhone,
        BigDecimal amount,
        BigDecimal cost
) {
}
