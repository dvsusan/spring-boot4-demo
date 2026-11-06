package com.susan.training.web;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.CreateOrderCommand;
import com.susan.training.order.OrderService;
import com.susan.training.order.OrderView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 接口。
 *
 * <p>Controller 只处理 HTTP 协议，业务规则交给 OrderService。</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    // 使用构造器注入，依赖关系清晰，也方便单元测试替换实现。
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单，成功时返回 201 Created。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderView> create(@Valid @RequestBody CreateOrderRequest request) {
        // HTTP 请求对象在这里转换成不依赖 Web 的业务命令。
        OrderView order = orderService.create(
                new CreateOrderCommand(request.customerId(), request.amount())
        );
        return ApiResponse.success(order);
    }

    /**
     * 根据订单号查询订单，找不到时由全局异常处理器返回 404。
     */
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderView> find(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.findByOrderNo(orderNo));
    }
}
