package com.susan.training.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 从 HTTP 层验证第六章认证、授权、数据权限、幂等和防重放。
 */
@SpringBootTest(properties = {
        "demo.chapter6.enabled=true",
        "demo.chapter6.request-signing.enabled=true"
})
@AutoConfigureMockMvc
class Chapter6SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestSignatureService signatureService;

    @Test
    void publicLoginRoleAndUnauthorizedPathsAreProtected() throws Exception {
        mockMvc.perform(get("/api/v1/security/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("public"));

        mockMvc.perform(get("/api/v1/security/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/security/me")
                        .with(httpBasic("analyst", "analyst123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("analyst"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant_acme"));

        mockMvc.perform(get("/api/v1/security/admin")
                        .with(httpBasic("clerk", "clerk123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/security/admin")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void refreshRotationAndLogoutInvalidateOldCredentials() throws Exception {
        LoginTokens first = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/security/me")
                        .header("Authorization", "Bearer " + first.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));

        JsonNode refreshed = postJson(
                "/api/v1/auth/refresh",
                "{\"refreshToken\":\"" + first.refresh() + "\"}"
        );
        LoginTokens second = tokens(refreshed);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + first.refresh() + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + second.access())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + second.refresh() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/security/me")
                        .header("Authorization", "Bearer " + second.access()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rbacAndTenantClaimPreventUnauthorizedOperations() throws Exception {
        LoginTokens analyst = login("analyst", "analyst123");

        mockMvc.perform(get("/api/v1/security/rbac/orders/SO1001")
                        .header("Authorization", "Bearer " + analyst.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("read:SO1001"));

        mockMvc.perform(delete("/api/v1/security/rbac/orders/SO1001")
                        .header("Authorization", "Bearer " + analyst.access()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/security/data/orders")
                        .header("Authorization", "Bearer " + analyst.access())
                        .header("X-Tenant-Id", "tenant_globex"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_MISMATCH"));
    }

    @Test
    void rowScopeAndColumnMaskingFollowTheCurrentUser() throws Exception {
        LoginTokens admin = login("admin", "admin123");
        LoginTokens analyst = login("analyst", "analyst123");
        LoginTokens clerk = login("clerk", "clerk123");

        mockMvc.perform(get("/api/v1/security/data/orders")
                        .header("Authorization", "Bearer " + admin.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].cost").isNumber());

        mockMvc.perform(get("/api/v1/security/data/orders")
                        .header("Authorization", "Bearer " + analyst.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].customerPhone").value("13812348001"))
                .andExpect(jsonPath("$.data[0].cost").doesNotExist());

        mockMvc.perform(get("/api/v1/security/data/orders")
                        .header("Authorization", "Bearer " + clerk.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderId").value("SO-SALES-02"))
                .andExpect(jsonPath("$.data[0].customerPhone").value("139****8002"));
    }

    @Test
    void signatureReplayAndIdempotentResponseWorkTogether() throws Exception {
        LoginTokens admin = login("admin", "admin123");
        String path = "/api/v1/security/payments";
        String body = "{\"orderId\":\"SO1001\",\"amount\":199.00}";
        String idempotencyKey = "payment-SO1001-" + UUID.randomUUID();
        String firstNonce = UUID.randomUUID().toString();
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = signature(timestamp, firstNonce, body);

        String firstBody = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + admin.access())
                        .header("Idempotency-Key", idempotencyKey)
                        .header(RequestSigningFilter.TIMESTAMP_HEADER, timestamp)
                        .header(RequestSigningFilter.NONCE_HEADER, firstNonce)
                        .header(RequestSigningFilter.SIGNATURE_HEADER, signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("SO1001"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondNonce = UUID.randomUUID().toString();
        String replayBody = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + admin.access())
                        .header("Idempotency-Key", idempotencyKey)
                        .header(RequestSigningFilter.TIMESTAMP_HEADER, timestamp)
                        .header(RequestSigningFilter.NONCE_HEADER, secondNonce)
                        .header(
                                RequestSigningFilter.SIGNATURE_HEADER,
                                signature(timestamp, secondNonce, body)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "true"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(replayBody).isEqualTo(firstBody);

        mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + admin.access())
                        .header("Idempotency-Key", "another-" + UUID.randomUUID())
                        .header(RequestSigningFilter.TIMESTAMP_HEADER, timestamp)
                        .header(RequestSigningFilter.NONCE_HEADER, firstNonce)
                        .header(RequestSigningFilter.SIGNATURE_HEADER, signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));
    }

    private LoginTokens login(String username, String password) throws Exception {
        JsonNode json = postJson(
                "/api/v1/auth/login",
                "{\"username\":\"" + username
                        + "\",\"password\":\"" + password + "\"}"
        );
        return tokens(json);
    }

    private JsonNode postJson(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private LoginTokens tokens(JsonNode response) {
        JsonNode data = response.path("data");
        return new LoginTokens(
                data.path("accessToken").asString(),
                data.path("refreshToken").asString()
        );
    }

    private String signature(
            String timestamp,
            String nonce,
            String body
    ) {
        String digest = signatureService.sha256(
                body.getBytes(StandardCharsets.UTF_8)
        );
        return signatureService.sign(
                "POST",
                RequestSigningFilter.PATH,
                timestamp,
                nonce,
                digest
        );
    }

    private record LoginTokens(String access, String refresh) {
    }
}
