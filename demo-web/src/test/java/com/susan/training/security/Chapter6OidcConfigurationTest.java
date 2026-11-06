package com.susan.training.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;

/**
 * 使用本地占位端点验证 OIDC 登录链配置，不访问真实身份提供商。
 */
@SpringBootTest(properties = {
        "demo.chapter6.enabled=true",
        "demo.chapter6.oauth2.enabled=true",
        "spring.security.oauth2.client.registration.corp-oidc.client-id=demo-client",
        "spring.security.oauth2.client.registration.corp-oidc.client-secret=demo-secret",
        "spring.security.oauth2.client.registration.corp-oidc.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.corp-oidc.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.corp-oidc.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.corp-oidc.provider=corp-oidc",
        "spring.security.oauth2.client.provider.corp-oidc.authorization-uri=https://idp.example.com/oauth2/authorize",
        "spring.security.oauth2.client.provider.corp-oidc.token-uri=https://idp.example.com/oauth2/token",
        "spring.security.oauth2.client.provider.corp-oidc.jwk-set-uri=https://idp.example.com/oauth2/jwks",
        "spring.security.oauth2.client.provider.corp-oidc.user-info-uri=https://idp.example.com/oauth2/userinfo",
        "spring.security.oauth2.client.provider.corp-oidc.user-name-attribute=sub"
})
@AutoConfigureMockMvc
class Chapter6OidcConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedSsoPageRedirectsToConfiguredIdentityProvider() throws Exception {
        mockMvc.perform(get("/sso/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/oauth2/authorization/corp-oidc"
                ));

        mockMvc.perform(get("/oauth2/authorization/corp-oidc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith(
                                "https://idp.example.com/oauth2/authorize?"
                        )
                ));
    }
}
