package com.susan.training.event;

import java.time.Instant;

/**
 * 订单实时事件，一次业务变更只构造一个事件供 SSE 和 WebSocket 共用。
 */
public record OrderEvent(
        String eventId,
        String type,
        String orderNo,
        String customerId,
        String status,
        Instant occurredAt
) {
}
