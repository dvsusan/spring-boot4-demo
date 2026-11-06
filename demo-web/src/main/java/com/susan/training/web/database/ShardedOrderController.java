package com.susan.training.web.database;

import com.susan.training.common.ApiResponse;
import com.susan.training.persistence.shard.ShardOrderView;
import com.susan.training.persistence.shard.ShardedOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 应用层分片演示接口。
 */
@RestController
@RequestMapping("/api/v1/db/shards/orders")
@Profile("db")
@Tag(name = "订单分片", description = "路由、单分片查询与跨分片聚合")
public class ShardedOrderController {
    private final ShardedOrderService shardedOrderService;

    public ShardedOrderController(ShardedOrderService shardedOrderService) {
        this.shardedOrderService = shardedOrderService;
    }

    @PostMapping
    @Operation(summary = "创建分片订单", description = "按订单号哈希选择物理表")
    public ApiResponse<ShardOrderView> create(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestParam @NotBlank String customerNo,
            @RequestParam @DecimalMin("0.01") BigDecimal amount
    ) {
        return ApiResponse.success(
                shardedOrderService.create(tenantId, customerNo, amount)
        );
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询单个分片订单")
    public ApiResponse<ShardOrderView> find(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(
                shardedOrderService.findByOrderNo(tenantId, orderNo)
        );
    }

    @GetMapping
    @Operation(summary = "跨分片按客户查询")
    public ApiResponse<List<ShardOrderView>> findByCustomer(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestParam @NotBlank String customerNo
    ) {
        return ApiResponse.success(
                shardedOrderService.findByCustomer(tenantId, customerNo)
        );
    }
}
