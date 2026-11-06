package com.susan.training.database;

import com.susan.training.persistence.repository.AuditLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 在 H2 PostgreSQL 模式下验证 JPA、MyBatis 分页和事务回滚的主流程。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("db-h2")
class DatabaseCoreIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogJpaRepository auditLogRepository;

    @Test
    void jpaWriteAndMyBatisPageWorkTogether() throws Exception {
        prepareInventory("P1001", 20);
        String orderNo = createOrder("C-DB-1", "机械键盘", "P1001", 2);

        mockMvc.perform(get("/api/v1/db/orders")
                        .param("customerNo", "C-DB-1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderNo").value(orderNo))
                .andExpect(jsonPath("$.data.content[0].itemCount").value(1))
                .andExpect(jsonPath("$.data.content[0].totalQuantity").value(2));
    }

    @Test
    void insufficientStockRollsBackOrderButPersistsAudit() throws Exception {
        prepareInventory("P2002", 1);

        mockMvc.perform(post("/api/v1/db/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("C-DB-2", "显示器", "P2002", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        mockMvc.perform(get("/api/v1/db/orders")
                        .param("customerNo", "C-DB-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // REQUIRES_NEW 审计事务独立提交，不受订单事务回滚影响。
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction().equals("ORDER_REJECTED")
                        && log.getDetail().contains("P2002"));
    }

    private void prepareInventory(String productId, int quantity) throws Exception {
        mockMvc.perform(put("/api/v1/db/inventory/{productId}", productId)
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk());
    }

    private String createOrder(
            String customerNo,
            String productName,
            String productId,
            int quantity
    ) throws Exception {
        String response = mockMvc.perform(post("/api/v1/db/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(customerNo, productName, productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("orderNo").asText();
    }

    private String orderJson(
            String customerNo,
            String productName,
            String productId,
            int quantity
    ) {
        return """
                {
                  "customerNo": "%s",
                  "customerName": "数据库测试客户",
                  "items": [
                    {
                      "productId": "%s",
                      "productName": "%s",
                      "quantity": %d,
                      "unitPrice": 100.00
                    }
                  ],
                  "metadata": {"channel": "web"}
                }
                """.formatted(customerNo, productId, productName, quantity);
    }
}
