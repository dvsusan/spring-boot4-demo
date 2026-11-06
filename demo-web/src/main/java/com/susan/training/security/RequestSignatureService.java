package com.susan.training.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 校验开放接口请求签名，并阻止同一 nonce 重复使用。
 */
public class RequestSignatureService {
    private final byte[] secret;
    private final Duration maxClockSkew;
    private final Clock clock;
    private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public RequestSignatureService(
            String secret,
            Duration maxClockSkew,
            Clock clock
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxClockSkew = maxClockSkew;
        this.clock = clock;
    }

    public void verify(
            String method,
            String path,
            String timestampText,
            String nonce,
            String signature,
            byte[] body
    ) {
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("签名时间戳格式错误");
        }
        if (nonce == null || nonce.length() < 8 || nonce.length() > 128) {
            throw new IllegalArgumentException("nonce 长度必须在 8 到 128 之间");
        }

        Instant requestTime = Instant.ofEpochSecond(timestamp);
        Instant now = clock.instant();
        Duration difference = Duration.between(requestTime, now).abs();
        if (difference.compareTo(maxClockSkew) > 0) {
            throw new IllegalArgumentException("请求已过期或时间偏差过大");
        }

        String expected = sign(
                method,
                path,
                timestampText,
                nonce,
                sha256(body)
        );
        if (signature == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException("请求签名不匹配");
        }

        Instant expiresAt = requestTime.plus(maxClockSkew);
        removeExpiredNonces(now);
        if (usedNonces.putIfAbsent(nonce, expiresAt) != null) {
            throw new IllegalArgumentException("nonce 已使用，拒绝重复请求");
        }
    }

    public String sign(
            String method,
            String path,
            String timestamp,
            String nonce,
            String bodySha256
    ) {
        String canonical = method + "\n"
                + path + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + bodySha256;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算请求签名", exception);
        }
    }

    public String sha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算请求体摘要", exception);
        }
    }

    private void removeExpiredNonces(Instant now) {
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
