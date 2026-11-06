package com.susan.training.client;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.OrderView;
import com.susan.training.web.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动随机端口服务，并用真实 RestClient 驱动的 HTTP Interface 调用它。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderHttpClientIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private OrderHttpClientFactory clientFactory;

    @Autowired
    private OrderClientProperties properties;

    @Test
    void httpInterfaceRoundTripsJson() {
        OrderApiClient client = clientFactory.create(
                "http://localhost:" + port,
                restClientBuilder,
                properties
        );

        ApiResponse<OrderView> created = client.create(
                new CreateOrderRequest("C9009", new BigDecimal("520.00"))
        );
        ApiResponse<OrderView> found = client.findByOrderNo(
                created.data().orderNo()
        );

        assertThat(created.code()).isEqualTo("0");
        assertThat(found.data().customerId()).isEqualTo("C9009");
        assertThat(found.data().amount()).isEqualByComparingTo("520.00");
    }
}
