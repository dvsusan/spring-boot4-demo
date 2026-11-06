package com.susan.training.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PostgreSQL JSONB 与全文检索组合查询结果。
 */
public record OrderSearchRow(
        String orderNo,
        String customerNo,
        BigDecimal amount,
        String status,
        String channel,
        String metadata,
        Instant createdAt
) {
}
