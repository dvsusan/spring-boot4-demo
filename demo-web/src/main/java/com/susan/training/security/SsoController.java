package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 展示 OIDC 登录后的企业身份与应用权限。
 */
@RestController
@RequestMapping("/sso")
@ConditionalOnProperty(
        prefix = "demo.chapter6.oauth2",
        name = "enabled",
        havingValue = "true"
)
public class SsoController {
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(
            @AuthenticationPrincipal OidcUser user,
            Authentication authentication
    ) {
        return ApiResponse.success(Map.of(
                "subject", user.getSubject(),
                "email", String.valueOf(user.getEmail()),
                "name", String.valueOf(user.getFullName()),
                "authorities", authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        ));
    }
}
