package com.susan.training.web.version;

import java.math.BigDecimal;

/**
 * v2 响应在 v1 基础上增加面向展示的状态文本。
 */
public record OrderViewV2(
        String orderNo,
        String customerId,
        BigDecimal amount,
        String status,
        String statusLabel
) {
}
