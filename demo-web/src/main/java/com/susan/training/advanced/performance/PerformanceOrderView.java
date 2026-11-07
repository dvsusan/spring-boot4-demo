package com.susan.training.advanced.performance;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 性能章节只暴露页面真正需要的字段，避免把 ORM 实体和关联对象整体序列化。
 */
public record PerformanceOrderView(
        String orderNo,
        String customerId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
}
