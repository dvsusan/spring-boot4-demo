package com.susan.training.web.database;

import com.susan.training.common.ApiResponse;
import com.susan.training.persistence.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教学用库存管理接口，用于准备和观察事务测试数据。
 */
@RestController
@RequestMapping("/api/v1/db/inventory")
@Profile({"db", "db-h2"})
@Tag(name = "数据库库存", description = "准备库存并验证事务扣减")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PutMapping("/{productId}")
    @Operation(summary = "设置库存", description = "把指定商品库存设置为明确数量")
    public ApiResponse<InventoryService.InventoryView> set(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @PathVariable String productId,
            @RequestParam @Min(0) int quantity
    ) {
        return ApiResponse.success(
                inventoryService.setQuantity(tenantId, productId, quantity)
        );
    }

    @GetMapping("/{productId}")
    @Operation(summary = "查询库存")
    public ApiResponse<InventoryService.InventoryView> get(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @PathVariable String productId
    ) {
        return ApiResponse.success(
                inventoryService.getQuantity(tenantId, productId)
        );
    }
}
