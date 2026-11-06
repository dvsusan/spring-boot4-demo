package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把登录和令牌错误转换为稳定的业务错误响应。
 */
@RestControllerAdvice
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class SecurityExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> badCredentials(
            BadCredentialsException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("BAD_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalidToken(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("INVALID_TOKEN", exception.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> accessDenied(
            AuthorizationDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(
                        "FORBIDDEN",
                        "当前身份没有访问权限"
                ));
    }
}
