package com.susan.training.web.version;

import java.math.BigDecimal;
import java.util.Map;

/**
 * v3 响应增加可发现的资源链接，调用方不必硬编码相邻接口地址。
 */
public record OrderViewV3(
        String orderNo,
        String customerId,
        BigDecimal amount,
        String status,
        Map<String, String> links
) {
}
