package com.susan.training.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 创建订单的 HTTP 请求对象。
 *
 * @param customerId 客户编号，不能为空白
 * @param amount     订单金额，不能为空且必须大于 0
 */
public record CreateOrderRequest(
        @NotBlank(message = "客户编号不能为空")
        String customerId,

        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
        BigDecimal amount
) {
}
