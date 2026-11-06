package com.susan.training.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存幂等键与第一次请求的响应。
 *
 * <p>当前使用进程内存；多实例部署应使用数据库唯一键或 Redis。</p>
 */
public class IdempotencyStore {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration retention;

    public IdempotencyStore(Clock clock, Duration retention) {
        this.clock = clock;
        this.retention = retention;
    }

    public Reservation reserve(String key) {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));

        CompletableFuture<StoredResponse> future = new CompletableFuture<>();
        Entry candidate = new Entry(future, now.plus(retention));
        Entry existing = entries.putIfAbsent(key, candidate);
        if (existing == null) {
            return new Reservation(key, future, false);
        }
        if (existing.response().isDone()) {
            return new Reservation(key, existing.response(), true);
        }
        throw new IllegalStateException("相同幂等键的请求正在处理中");
    }

    public void complete(Reservation reservation, StoredResponse response) {
        reservation.future().complete(response);
    }

    public void abort(Reservation reservation) {
        entries.remove(reservation.key());
    }

    private record Entry(
            CompletableFuture<StoredResponse> response,
            Instant expiresAt
    ) {
    }

    public record StoredResponse(
            int status,
            String contentType,
            byte[] body
    ) {
        public StoredResponse {
            body = body.clone();
        }
    }

    public record Reservation(
            String key,
            CompletableFuture<StoredResponse> future,
            boolean replay
    ) {
    }
}
