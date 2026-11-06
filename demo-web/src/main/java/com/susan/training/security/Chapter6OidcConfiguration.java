package com.susan.training.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 企业 OIDC 登录链。
 *
 * <p>只有配置了 client registration 并显式开启 oauth2 后才创建，
 * 普通 JWT API 不依赖外部身份提供商。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "demo.chapter6.oauth2",
        name = "enabled",
        havingValue = "true"
)
public class Chapter6OidcConfiguration {

    @Bean
    @Order(0)
    public SecurityFilterChain oidcLoginSecurityFilterChain(
            HttpSecurity http,
            SsoAuthoritiesMapper authoritiesMapper
    ) throws Exception {
        http.securityMatcher(
                        "/sso/**",
                        "/oauth2/**",
                        "/login/oauth2/**"
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(authoritiesMapper)
                        )
                        .defaultSuccessUrl("/sso/me", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/sso/logout")
                        .logoutSuccessUrl("/")
                );
        return http.build();
    }
}
