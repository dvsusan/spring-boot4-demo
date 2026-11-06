package com.susan.training.persistence.query;

import java.util.List;

/**
 * 带总数的分页结果。
 */
public record OrderPage(
        List<OrderSummaryRow> content,
        long totalElements,
        int page,
        int size
) {
}
