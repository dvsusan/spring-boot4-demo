package com.susan.training.web.version;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.OrderService;
import com.susan.training.order.OrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 同一业务资源的不同 API 版本示例。
 *
 * <p>URL 版本使用 /api/v1；Header 和媒体类型版本复用 /api/orders，
 * 通过请求头或 Accept 选择响应契约。</p>
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单 API 版本管理", description = "Header 与媒体类型两种版本选择方式")
public class OrderVersionController {
    private final OrderService orderService;

    public OrderVersionController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Header 版本将版本信息与资源路径分离，适合无法改 URL 的客户端。
     */
    @GetMapping(value = "/{orderNo}", headers = "X-Api-Version=2")
    @Operation(summary = "查询订单 v2", description = "请求头 X-Api-Version: 2 选择 v2 契约")
    public ApiResponse<OrderViewV2> findV2(
            @PathVariable String orderNo,
            HttpServletResponse response
    ) {
        // 告诉缓存层同一 URL 会因版本请求头产生不同结果。
        response.addHeader("Vary", "X-Api-Version");
        OrderView order = orderService.findByOrderNo(orderNo);
        return ApiResponse.success(new OrderViewV2(
                order.orderNo(),
                order.customerId(),
                order.amount(),
                order.status().name(),
                statusLabel(order)
        ));
    }

    /**
     * 媒体类型版本把版本写进响应格式，HTTP 语义更明确，但客户端配置更复杂。
     */
    @GetMapping(value = "/{orderNo}", produces = "application/vnd.susan.order.v3+json")
    @Operation(summary = "查询订单 v3", description = "Accept 使用 v3 厂商媒体类型选择 v3 契约")
    public ApiResponse<OrderViewV3> findV3(@PathVariable String orderNo) {
        OrderView order = orderService.findByOrderNo(orderNo);
        return ApiResponse.success(new OrderViewV3(
                order.orderNo(),
                order.customerId(),
                order.amount(),
                order.status().name(),
                Map.of(
                        "self", "/api/orders/" + orderNo,
                        "events", "/api/v1/events/orders/" + orderNo
                )
        ));
    }

    private String statusLabel(OrderView order) {
        return switch (order.status()) {
            case CREATED -> "已创建";
        };
    }
}
