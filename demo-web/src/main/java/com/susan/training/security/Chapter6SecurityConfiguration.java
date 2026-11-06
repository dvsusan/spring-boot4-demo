package com.susan.training.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 第六章总装配：默认放行，启用 chapter6 后切换为认证链。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Chapter6SecurityProperties.class)
@EnableMethodSecurity
public class Chapter6SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Chapter6UserDetailsService chapter6UserDetailsService(
            PasswordEncoder passwordEncoder
    ) {
        return new Chapter6UserDetailsService(passwordEncoder);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "demo.chapter6",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public SecurityFilterChain chapter6DisabledSecurity(
            HttpSecurity http
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * 只有显式开启第六章时才创建 JWT、租户过滤和幂等组件。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "demo.chapter6",
            name = "enabled",
            havingValue = "true"
    )
    static class EnabledSecurityConfiguration {

        @Bean
        public Clock securityClock() {
            return Clock.systemUTC();
        }

        @Bean
        public SecretKey jwtSigningKey(Chapter6SecurityProperties properties) {
            byte[] secret = properties.getToken().getSigningKey()
                    .getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                throw new IllegalStateException(
                        "demo.chapter6.token.signing-key 至少需要 32 字节"
                );
            }
            return new SecretKeySpec(secret, "HmacSHA256");
        }

        @Bean
        public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
            return new NimbusJwtEncoder(
                    new ImmutableSecret<>(jwtSigningKey)
            );
        }

        @Bean
        public JwtDecoder jwtDecoder(
                SecretKey jwtSigningKey,
                Chapter6SecurityProperties properties,
                TokenStore tokenStore
        ) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withSecretKey(jwtSigningKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
            OAuth2TokenValidator<Jwt> accessOnly =
                    new JwtClaimValidator<>(
                            "token_use",
                            "access"::equals
                    );
            OAuth2TokenValidator<Jwt> notRevoked = jwt -> {
                if (tokenStore.isAccessTokenRevoked(jwt.getId())) {
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    "invalid_token",
                                    "访问令牌已退出",
                                    null
                            )
                    );
                }
                return OAuth2TokenValidatorResult.success();
            };
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(
                            properties.getJwt().getIssuer()
                    ),
                    accessOnly,
                    notRevoked
            ));
            return decoder;
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtAuthenticationConverter converter =
                    new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                List<GrantedAuthority> authorities = new ArrayList<>();
                claimList(jwt, "roles").stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
                claimList(jwt, "permissions").stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
                return authorities;
            });
            return converter;
        }

        @Bean
        public TokenStore tokenStore(
                Clock securityClock,
                Chapter6SecurityProperties properties
        ) {
            return new TokenStore(
                    securityClock,
                    properties.getToken().getRefreshTtl()
            );
        }

        @Bean
        public JwtTokenService jwtTokenService(
                JwtEncoder jwtEncoder,
                Clock securityClock,
                Chapter6SecurityProperties properties
        ) {
            return new JwtTokenService(
                    jwtEncoder,
                    securityClock,
                    properties.getJwt().getIssuer(),
                    properties.getJwt().getAccessTtl()
            );
        }

        @Bean
        public AuthService authService(
                Chapter6UserDetailsService userDetailsService,
                PasswordEncoder passwordEncoder,
                JwtTokenService jwtTokenService,
                TokenStore tokenStore,
                Chapter6SecurityProperties properties
        ) {
            return new AuthService(
                    userDetailsService,
                    passwordEncoder,
                    jwtTokenService,
                    tokenStore,
                    properties.getJwt().getAccessTtl()
            );
        }

        @Bean
        public DataPermissionService dataPermissionService() {
            return new DataPermissionService();
        }

        @Bean
        public TenantAccessFilter tenantAccessFilter(ObjectMapper objectMapper) {
            return new TenantAccessFilter(objectMapper);
        }

        @Bean
        public IdempotencyStore idempotencyStore(
                Clock securityClock
        ) {
            return new IdempotencyStore(
                    securityClock,
                    Duration.ofHours(24)
            );
        }

        @Bean
        public IdempotencyFilter idempotencyFilter(
                IdempotencyStore idempotencyStore,
                ObjectMapper objectMapper
        ) {
            return new IdempotencyFilter(idempotencyStore, objectMapper);
        }

        @Bean
        @ConditionalOnProperty(
                prefix = "demo.chapter6.request-signing",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public RequestSignatureService requestSignatureService(
                Chapter6SecurityProperties properties,
                Clock securityClock
        ) {
            return new RequestSignatureService(
                    properties.getToken().getWebSecret(),
                    properties.getRequestSigning().getMaxClockSkew(),
                    securityClock
            );
        }

        @Bean
        @ConditionalOnProperty(
                prefix = "demo.chapter6.request-signing",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public RequestSigningFilter requestSigningFilter(
                RequestSignatureService signatureService,
                ObjectMapper objectMapper
        ) {
            return new RequestSigningFilter(signatureService, objectMapper);
        }

        @Bean
        public SsoIdentityMapper ssoIdentityMapper() {
            return new SsoIdentityMapper();
        }

        @Bean
        public SsoAuthoritiesMapper ssoAuthoritiesMapper(
                SsoIdentityMapper identityMapper
        ) {
            return new SsoAuthoritiesMapper(identityMapper);
        }

        @Bean
        public SecurityFilterChain chapter6SecurityFilterChain(
                HttpSecurity http,
                JwtAuthenticationConverter jwtAuthenticationConverter,
                TenantAccessFilter tenantAccessFilter,
                IdempotencyFilter idempotencyFilter,
                org.springframework.beans.factory.ObjectProvider<RequestSigningFilter>
                        requestSigningFilter,
                ObjectMapper objectMapper
        ) throws Exception {
            http.securityMatcher("/api/v1/security/**", "/api/v1/auth/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(
                                    "/api/v1/security/public",
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh"
                            ).permitAll()
                            .requestMatchers("/api/v1/security/admin")
                            .hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .httpBasic(Customizer.withDefaults())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                    jwtAuthenticationConverter
                            ))
                            .authenticationEntryPoint((request, response, exception) ->
                                    writeSecurityError(
                                            response,
                                            objectMapper,
                                            401,
                                            "UNAUTHORIZED",
                                            "请先登录或提供有效访问令牌"
                                    )
                            )
                    )
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, exception) ->
                                    writeSecurityError(
                                            response,
                                            objectMapper,
                                            401,
                                            "UNAUTHORIZED",
                                            "请先登录"
                                    )
                            )
                            .accessDeniedHandler((request, response, exception) ->
                                    writeSecurityError(
                                            response,
                                            objectMapper,
                                            403,
                                            "FORBIDDEN",
                                            "当前身份没有访问权限"
                                    )
                            )
                    );

            RequestSigningFilter signing = requestSigningFilter.getIfAvailable();
            if (signing != null) {
                http.addFilterBefore(
                        signing,
                        BearerTokenAuthenticationFilter.class
                );
            }
            http.addFilterAfter(
                            tenantAccessFilter,
                            BearerTokenAuthenticationFilter.class
                    )
                    .addFilterAfter(
                            idempotencyFilter,
                            TenantAccessFilter.class
                    );
            return http.build();
        }

        private static List<String> claimList(Jwt jwt, String claim) {
            List<String> values = jwt.getClaimAsStringList(claim);
            return values == null ? List.of() : values;
        }

        private static void writeSecurityError(
                jakarta.servlet.http.HttpServletResponse response,
                ObjectMapper objectMapper,
                int status,
                String code,
                String message
        ) throws java.io.IOException {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.failure(code, message)
            );
        }
    }
}
