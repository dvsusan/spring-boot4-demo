package com.susan.training.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 从 HTTP 层验证 URL、Header、媒体类型版本、错误码和参数校验。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v1SupportsCreateFindSearchAndPatch() throws Exception {
        String orderNo = createOrder("C1001", "99.00");

        mockMvc.perform(get("/api/v1/orders/{orderNo}", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.amount").value(99.0));

        mockMvc.perform(get("/api/v1/orders")
                        .param("customerId", "C1001")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderNo").value(orderNo));

        mockMvc.perform(patch("/api/v1/orders/{orderNo}", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":88.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value("C1001"))
                .andExpect(jsonPath("$.data.amount").value(88.0));
    }

    @Test
    void headerAndMediaTypeSelectDifferentRepresentations() throws Exception {
        String orderNo = createOrder("C3003", "66.00");

        mockMvc.perform(get("/api/orders/{orderNo}", orderNo)
                        .header("X-Api-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "X-Api-Version"))
                .andExpect(jsonPath("$.data.statusLabel").value("已创建"));

        mockMvc.perform(get("/api/orders/{orderNo}", orderNo)
                        .accept("application/vnd.susan.order.v3+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.links.self")
                        .value("/api/orders/" + orderNo));
    }

    @Test
    void validationUsesGroupsAndChineseMessages() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Accept-Language", "zh-CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.data[?(@.field == 'customerId')].message")
                        .value("客户编号不能为空"))
                .andExpect(jsonPath("$.data[?(@.field == 'amount')].message")
                        .value("订单金额必须在 0.01 到 1000000.00 之间"));

        mockMvc.perform(get("/api/v1/orders")
                        .param("customerId", "C1001")
                        .param("limit", "0")
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message")
                        .value("Limit must be between 1 and 100"));
    }

    @Test
    void missingOrderReturnsStableNotFoundCode() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderNo}", "SO-NOT-FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void openApiDocumentDescribesOrderEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Spring Boot 4 订单履约平台 API"))
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists());
    }

    private String createOrder(String customerId, String amount) throws Exception {
        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","amount":%s}
                                """.formatted(customerId, amount)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("orderNo").asText();
    }
}
