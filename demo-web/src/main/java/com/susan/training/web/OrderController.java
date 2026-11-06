package com.susan.training.web;

import com.susan.training.common.ApiResponse;
import com.susan.training.event.OrderEventBroker;
import com.susan.training.order.CreateOrderCommand;
import com.susan.training.order.OrderService;
import com.susan.training.order.OrderView;
import com.susan.training.web.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单 REST 接口。
 *
 * <p>Controller 只处理 HTTP 协议，业务规则交给 OrderService。</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单 API v1", description = "订单创建、查询与金额更新")
public class OrderController {
    private final OrderService orderService;
    private final OrderEventBroker eventBroker;

    // 使用构造器注入，依赖关系清晰，也方便单元测试替换实现。
    public OrderController(OrderService orderService, OrderEventBroker eventBroker) {
        this.orderService = orderService;
        this.eventBroker = eventBroker;
    }

    /**
     * 创建订单，成功时返回 201 Created。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建订单", description = "创建成功返回 201 和订单快照")
    public ApiResponse<OrderView> create(
            @Validated(ValidationGroups.Create.class)
            @RequestBody
            CreateOrderRequest request
    ) {
        // HTTP 请求对象在这里转换成不依赖 Web 的业务命令。
        OrderView order = orderService.create(
                new CreateOrderCommand(request.customerId(), request.amount())
        );
        // 业务数据落库成功后事件才可发布；当前内存示例在 create 返回时即可发布。
        eventBroker.publish("order-created", order);
        return ApiResponse.success(order);
    }

    /**
     * 根据订单号查询订单，找不到时由全局异常处理器返回 404。
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单", description = "根据订单号查询单个订单")
    public ApiResponse<OrderView> find(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.findByOrderNo(orderNo));
    }

    /**
     * 查询参数通过 @RequestParam 绑定，limit 在进入业务代码前先校验边界。
     */
    @GetMapping
    @Operation(summary = "按客户查询订单", description = "返回指定客户的有限数量订单")
    public ApiResponse<List<OrderView>> findByCustomer(
            @RequestParam
            @Parameter(description = "客户编号", example = "C1001")
            String customerId,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{order.limit.range}")
            @Max(value = 100, message = "{order.limit.range}")
            @Parameter(description = "最大返回条数", example = "20")
            int limit
    ) {
        return ApiResponse.success(orderService.findByCustomerId(customerId, limit));
    }

    /**
     * PATCH 只提交需要修改的字段；省略 customerId 时保留原客户编号。
     */
    @PatchMapping("/{orderNo}")
    @Operation(summary = "更新订单金额", description = "使用更新校验组校验金额并返回最新快照")
    public ApiResponse<OrderView> update(
            @PathVariable String orderNo,
            @Validated(ValidationGroups.Update.class)
            @RequestBody
            CreateOrderRequest request
    ) {
        OrderView updated = orderService.update(
                orderNo,
                request.customerId(),
                request.amount()
        );
        eventBroker.publish("order-updated", updated);
        return ApiResponse.success(updated);
    }
}
