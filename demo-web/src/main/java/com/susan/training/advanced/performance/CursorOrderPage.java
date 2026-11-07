package com.susan.training.advanced.performance;

import java.util.List;

/**
 * 基于游标的稳定分页结果。
 */
public record CursorOrderPage(
        List<PerformanceOrderView> items,
        String nextCursor,
        boolean hasMore,
        int pageSize
) {
}
