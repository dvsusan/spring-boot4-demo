package com.susan.training.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;

/**
 * 登录、刷新与退出业务。
 */
public class AuthService {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenStore tokenStore;
    private final Duration accessTtl;

    public AuthService(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TokenStore tokenStore,
            Duration accessTtl
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenStore = tokenStore;
        this.accessTtl = accessTtl;
    }

    public TokenPair login(String username, String password) {
        Chapter6UserDetails user = (Chapter6UserDetails)
                userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        return issuePair(user);
    }

    public TokenPair refresh(String refreshToken) {
        TokenStore.RefreshRotation rotation =
                tokenStore.rotateRefreshToken(refreshToken);
        Chapter6UserDetails user = (Chapter6UserDetails)
                userDetailsService.loadUserByUsername(rotation.username());
        JwtTokenService.IssuedAccessToken access =
                jwtTokenService.issue(user);
        return new TokenPair(
                "Bearer",
                access.value(),
                rotation.nextRefreshToken(),
                accessTtl.toSeconds()
        );
    }

    public void logout(Jwt accessToken, String refreshToken) {
        tokenStore.revokeAccessToken(
                accessToken.getId(),
                accessToken.getExpiresAt()
        );
        tokenStore.revokeRefreshToken(refreshToken);
    }

    private TokenPair issuePair(Chapter6UserDetails user) {
        JwtTokenService.IssuedAccessToken access =
                jwtTokenService.issue(user);
        String refresh = tokenStore.createRefreshToken(user.getUsername());
        return new TokenPair(
                "Bearer",
                access.value(),
                refresh,
                accessTtl.toSeconds()
        );
    }
}
