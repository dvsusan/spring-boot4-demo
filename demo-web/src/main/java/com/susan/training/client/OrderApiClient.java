package com.susan.training.client;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.OrderView;
import com.susan.training.web.CreateOrderRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 声明式订单客户端，接口定义与普通 Controller 非常接近。
 */
@HttpExchange("/api/v1/orders")
public interface OrderApiClient {

    @PostExchange
    ApiResponse<OrderView> create(@RequestBody CreateOrderRequest request);

    @GetExchange("/{orderNo}")
    ApiResponse<OrderView> findByOrderNo(@PathVariable String orderNo);

    @PatchExchange("/{orderNo}")
    ApiResponse<OrderView> update(
            @PathVariable String orderNo,
            @RequestBody CreateOrderRequest request
    );
}
