package com.susan.training.security;

/**
 * 登录或刷新成功后返回的令牌对。
 */
public record TokenPair(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
