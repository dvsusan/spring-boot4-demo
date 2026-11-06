package com.susan.training.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 签发短期访问令牌。
 */
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTtl;

    public JwtTokenService(
            JwtEncoder encoder,
            Clock clock,
            String issuer,
            Duration accessTtl
    ) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    public IssuedAccessToken issue(Chapter6UserDetails user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTtl);
        String tokenId = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .id(tokenId)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_use", "access")
                .claim("tenant_id", user.getTenantId())
                .claim("department_id", user.getDepartmentId())
                .claim("roles", user.getRoles())
                .claim("permissions", user.getPermissions())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
        return new IssuedAccessToken(token, tokenId, expiresAt);
    }

    public record IssuedAccessToken(
            String value,
            String tokenId,
            Instant expiresAt
    ) {
    }
}
