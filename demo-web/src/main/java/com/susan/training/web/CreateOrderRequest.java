package com.susan.training.web;

import com.susan.training.web.validation.MoneyAmount;
import com.susan.training.web.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建或更新订单共用的 HTTP 请求对象。
 *
 * <p>创建场景要求客户编号必填；更新场景允许省略客户编号，
 * 由 Spring Validation 的分组机制根据接口语义选择不同约束。</p>
 */
public record CreateOrderRequest(
        @NotBlank(
                groups = ValidationGroups.Create.class,
                message = "{order.customerId.required}"
        )
        @Size(
                max = 32,
                groups = {ValidationGroups.Create.class, ValidationGroups.Update.class},
                message = "{order.customerId.size}"
        )
        String customerId,

        @NotNull(
                groups = {ValidationGroups.Create.class, ValidationGroups.Update.class},
                message = "{order.amount.required}"
        )
        @MoneyAmount(
                groups = {ValidationGroups.Create.class, ValidationGroups.Update.class},
                message = "{order.amount.range}"
        )
        BigDecimal amount
) {
}
