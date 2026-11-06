package com.susan.training.persistence.shard;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 分片订单视图。
 */
public record ShardOrderView(
        String orderNo,
        String customerNo,
        BigDecimal amount,
        String status,
        int shard,
        Instant createdAt
) {
}
