package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT 登录、刷新和退出接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(
                authService.login(request.username(), request.password())
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPair> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        return ApiResponse.success(
                authService.refresh(request.refreshToken())
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authService.logout(
                accessToken,
                request == null ? null : request.refreshToken()
        );
        return ApiResponse.success(null);
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }
}
