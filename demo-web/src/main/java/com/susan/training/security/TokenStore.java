package com.susan.training.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷新令牌与访问令牌撤销记录。
 *
 * <p>当前实现位于进程内存，只适合单实例教学。生产环境应使用 Redis 或数据库，
 * 并为刷新令牌保存哈希、设备、用户和过期时间。</p>
 */
public class TokenStore {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, RefreshSession> refreshSessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> revokedAccessTokens = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration refreshTtl;

    public TokenStore(Clock clock, Duration refreshTtl) {
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    public String createRefreshToken(String username) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshSessions.put(token, new RefreshSession(
                username,
                clock.instant().plus(refreshTtl)
        ));
        return token;
    }

    /**
     * 刷新令牌只能成功使用一次，调用成功后会立即失效并生成新令牌。
     */
    public RefreshRotation rotateRefreshToken(String token) {
        RefreshSession session = refreshSessions.remove(token);
        if (session == null || session.expiresAt().isBefore(clock.instant())) {
            throw new IllegalArgumentException("刷新令牌无效或已过期");
        }
        String next = createRefreshToken(session.username());
        return new RefreshRotation(session.username(), next);
    }

    public void revokeRefreshToken(String token) {
        if (token != null && !token.isBlank()) {
            refreshSessions.remove(token);
        }
    }

    public void revokeAccessToken(String tokenId, Instant expiresAt) {
        if (tokenId != null && expiresAt != null) {
            revokedAccessTokens.put(tokenId, expiresAt);
        }
    }

    public boolean isAccessTokenRevoked(String tokenId) {
        Instant expiresAt = revokedAccessTokens.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(clock.instant())) {
            revokedAccessTokens.remove(tokenId, expiresAt);
            return false;
        }
        return true;
    }

    public record RefreshSession(String username, Instant expiresAt) {
    }

    public record RefreshRotation(String username, String nextRefreshToken) {
    }
}
