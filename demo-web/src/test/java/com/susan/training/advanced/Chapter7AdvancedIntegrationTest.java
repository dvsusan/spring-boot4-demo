package com.susan.training.advanced;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 从 HTTP 层验证异步聚合、容错、游标分页和 Spring AI 演示链路。
 */
@SpringBootTest(properties = {
        "demo.chapter7.enabled=true",
        "demo.chapter7.ai.remote-enabled=false",
        "spring.ai.openai.api-key=demo-key",
        "resilience4j.ratelimiter.instances.chapter7-risk.limit-for-period=100"
})
@AutoConfigureMockMvc
class Chapter7AdvancedIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void virtualThreadsAggregateOrderDownstreamCalls() throws Exception {
        mockMvc.perform(get("/api/v1/chapter7/async/orders/SO1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO1001"))
                .andExpect(jsonPath("$.data.degraded").value(false))
                .andExpect(jsonPath("$.data.inventory.available").value(19))
                .andExpect(jsonPath("$.data.risk.allowed").value(true));
    }

    @Test
    void retryAndCircuitBreakerReturnControlledFallback() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(get("/api/v1/chapter7/resilience/risk/SO1001")
                            .param("fail", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fallback").value(true));
        }

        mockMvc.perform(get("/api/v1/chapter7/resilience/risk/SO1001")
                        .param("fail", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("CIRCUIT_OPEN"));
    }

    @Test
    void cursorPaginationDoesNotRepeatRows() throws Exception {
        String firstResponse = mockMvc.perform(
                        get("/api/v1/chapter7/performance/orders")
                                .param("limit", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode first = objectMapper.readTree(firstResponse).path("data");
        String cursor = first.path("nextCursor").asString();

        Set<String> firstIds = new LinkedHashSet<>();
        first.path("items").forEach(item ->
                firstIds.add(item.path("orderNo").asString())
        );

        String secondResponse = mockMvc.perform(
                        get("/api/v1/chapter7/performance/orders")
                                .param("cursor", cursor)
                                .param("limit", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode second = objectMapper.readTree(secondResponse).path("data");
        Set<String> secondIds = new LinkedHashSet<>();
        second.path("items").forEach(item ->
                secondIds.add(item.path("orderNo").asString())
        );

        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
    }

    @Test
    void springAiCustomerServiceUsesRetrievedKnowledge() throws Exception {
        mockMvc.perform(post("/api/v1/chapter7/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"订单签收后怎么申请退款？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiAnswered").value(true))
                .andExpect(jsonPath("$.data.sources[0]").value("订单退款规则"))
                .andExpect(jsonPath("$.data.answer").isNotEmpty());

        mockMvc.perform(post("/api/v1/chapter7/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"火星基地如何报税？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiAnswered").value(false))
                .andExpect(jsonPath("$.data.sources.length()").value(0));
    }
}
