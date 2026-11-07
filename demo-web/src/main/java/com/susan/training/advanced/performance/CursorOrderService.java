package com.susan.training.advanced.performance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * 使用“创建时间 + 订单号”作为稳定游标的分页示例。
 */
@Service
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class CursorOrderService {
    private static final int MAX_PAGE_SIZE = 50;
    private final List<PerformanceOrderView> orders = createOrders();

    public CursorOrderPage page(String cursor, int requestedSize) {
        int size = Math.max(1, Math.min(requestedSize, MAX_PAGE_SIZE));
        Cursor position = decode(cursor);
        List<PerformanceOrderView> remaining = orders.stream()
                .filter(order -> position == null || isAfter(order, position))
                .limit((long) size + 1)
                .toList();

        boolean hasMore = remaining.size() > size;
        List<PerformanceOrderView> items = hasMore
                ? remaining.subList(0, size)
                : remaining;
        String nextCursor = hasMore
                ? encode(items.get(items.size() - 1))
                : null;
        return new CursorOrderPage(items, nextCursor, hasMore, size);
    }

    private boolean isAfter(
            PerformanceOrderView order,
            Cursor cursor
    ) {
        if (order.createdAt().isAfter(cursor.createdAt())) {
            return true;
        }
        return order.createdAt().equals(cursor.createdAt())
                && order.orderNo().compareTo(cursor.orderNo()) > 0;
    }

    private String encode(PerformanceOrderView order) {
        String value = order.createdAt().toEpochMilli()
                + "|"
                + order.orderNo();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", 2);
            return new Cursor(
                    Instant.ofEpochMilli(Long.parseLong(parts[0])),
                    parts[1]
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("分页游标格式无效");
        }
    }

    private List<PerformanceOrderView> createOrders() {
        Instant base = Instant.parse("2026-11-07T00:00:00Z");
        List<PerformanceOrderView> result = new ArrayList<>();
        for (int index = 0; index < 120; index++) {
            result.add(new PerformanceOrderView(
                    "SO" + String.format("%04d", index),
                    "C" + (index % 10),
                    BigDecimal.valueOf(100L + index),
                    "CREATED",
                    base.plusSeconds(index)
            ));
        }
        result.sort(Comparator
                .comparing(PerformanceOrderView::createdAt)
                .thenComparing(PerformanceOrderView::orderNo));
        return List.copyOf(result);
    }

    private record Cursor(Instant createdAt, String orderNo) {
    }
}
