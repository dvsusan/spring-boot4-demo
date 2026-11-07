package com.susan.training.advanced;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证业务指标、健康探针与 Prometheus 抓取端点。
 */
@SpringBootTest(properties = {
        "demo.chapter8.enabled=true",
        "management.health.redis.enabled=false",
        "management.health.elasticsearch.enabled=false",
        "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
        "management.otlp.tracing.export.enabled=false"
})
@AutoConfigureMockMvc
class Chapter8ObservabilityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void metricsAndHealthProbesExposeOperationalSignals() throws Exception {
        mockMvc.perform(get("/api/v1/chapter8/observability/orders/SO1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OBSERVED"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("chapter8_orders_total")
                ))
                .andExpect(content().string(
                        containsString("chapter8_direct_lookup_seconds")
                ));
    }
}
