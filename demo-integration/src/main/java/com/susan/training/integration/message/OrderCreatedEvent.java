package com.susan.training.integration.message;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 订单创建事件。正式项目应包含唯一 eventId 和版本号，便于幂等消费。
 */
public record OrderCreatedEvent(
        String eventId,
        String orderNo,
        String customerId,
        BigDecimal amount,
        Instant occurredAt
) {
}
