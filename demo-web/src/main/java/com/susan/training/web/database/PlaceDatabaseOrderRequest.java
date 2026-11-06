package com.susan.training.web.database;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据库订单创建请求。
 */
public record PlaceDatabaseOrderRequest(
        @NotBlank(message = "客户编号不能为空")
        String customerNo,

        @NotBlank(message = "客户名称不能为空")
        String customerName,

        @NotEmpty(message = "订单至少需要一个商品")
        List<@Valid Item> items,

        // metadata 会写入 PostgreSQL JSONB，适合渠道、活动等弹性字段。
        Map<String, String> metadata
) {
    /**
     * 商品明细请求。
     */
    public record Item(
            @NotBlank(message = "商品编号不能为空")
            String productId,

            @NotBlank(message = "商品名称不能为空")
            String productName,

            @Min(value = 1, message = "商品数量必须大于 0")
            int quantity,

            @NotNull(message = "商品单价不能为空")
            @DecimalMin(value = "0.01", message = "商品单价必须大于 0")
            BigDecimal unitPrice
    ) {
    }
}
