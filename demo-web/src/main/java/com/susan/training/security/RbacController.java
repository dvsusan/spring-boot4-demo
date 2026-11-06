package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 使用细粒度权限保护资源操作。
 */
@RestController
@RequestMapping("/api/v1/security/rbac/orders")
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class RbacController {
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:read')")
    public ApiResponse<String> read(@PathVariable String orderId) {
        return ApiResponse.success("read:" + orderId);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:delete')")
    public ApiResponse<String> delete(@PathVariable String orderId) {
        return ApiResponse.success("deleted:" + orderId);
    }
}
