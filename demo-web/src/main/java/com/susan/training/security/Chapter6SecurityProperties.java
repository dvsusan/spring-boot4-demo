package com.susan.training.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 第六章安全能力的集中配置。
 */
@ConfigurationProperties(prefix = "demo.chapter6")
public class Chapter6SecurityProperties {
    private boolean enabled;
    private final OAuth2 oauth2 = new OAuth2();
    private final Jwt jwt = new Jwt();
    private final Token token = new Token();
    private final RequestSigning requestSigning = new RequestSigning();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Token getToken() {
        return token;
    }

    public RequestSigning getRequestSigning() {
        return requestSigning;
    }

    public static class OAuth2 {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Jwt {
        private String issuer = "spring-boot4-demo";
        private Duration accessTtl = Duration.ofMinutes(15);

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getAccessTtl() {
            return accessTtl;
        }

        public void setAccessTtl(Duration accessTtl) {
            this.accessTtl = accessTtl;
        }
    }

    public static class Token {
        private String signingKey =
                "chapter6-demo-signing-key-change-before-production";
        private String webSecret = "chapter6-request-signing-secret";
        private Duration refreshTtl = Duration.ofDays(7);

        public String getSigningKey() {
            return signingKey;
        }

        public void setSigningKey(String signingKey) {
            this.signingKey = signingKey;
        }

        public String getWebSecret() {
            return webSecret;
        }

        public void setWebSecret(String webSecret) {
            this.webSecret = webSecret;
        }

        public Duration getRefreshTtl() {
            return refreshTtl;
        }

        public void setRefreshTtl(Duration refreshTtl) {
            this.refreshTtl = refreshTtl;
        }
    }

    public static class RequestSigning {
        private boolean enabled = true;
        private Duration maxClockSkew = Duration.ofMinutes(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getMaxClockSkew() {
            return maxClockSkew;
        }

        public void setMaxClockSkew(Duration maxClockSkew) {
            this.maxClockSkew = maxClockSkew;
        }
    }
}
