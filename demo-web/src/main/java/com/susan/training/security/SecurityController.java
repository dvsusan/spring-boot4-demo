package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一章安全示例：公开、登录和角色接口。
 */
@RestController
@RequestMapping("/api/v1/security")
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class SecurityController {
    @GetMapping("/public")
    public ApiResponse<String> publicEndpoint() {
        return ApiResponse.success("public");
    }

    @GetMapping("/me")
    public ApiResponse<SecurityIdentity> me(Authentication authentication) {
        return ApiResponse.success(SecurityIdentity.from(authentication));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> admin() {
        return ApiResponse.success("admin-only");
    }
}
