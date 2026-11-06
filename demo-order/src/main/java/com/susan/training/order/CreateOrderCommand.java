package com.susan.training.order;

import java.math.BigDecimal;

/**
 * 创建订单的业务命令。
 *
 * <p>它不包含 HTTP 注解，Web 层需要先把请求对象转换成这个命令，
 * 业务模块因此不需要依赖 Jakarta Validation。</p>
 */
public record CreateOrderCommand(
        String customerId,
        BigDecimal amount
) {
}
