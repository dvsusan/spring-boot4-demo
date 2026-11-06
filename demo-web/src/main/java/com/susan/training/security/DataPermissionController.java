package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 返回执行行级权限和列级脱敏后的订单视图。
 */
@RestController
@RequestMapping("/api/v1/security/data/orders")
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class DataPermissionController {
    private final DataPermissionService permissionService;

    public DataPermissionController(DataPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public ApiResponse<List<SecuredOrderView>> orders(
            Authentication authentication
    ) {
        return ApiResponse.success(
                permissionService.visibleOrders(authentication)
        );
    }
}
