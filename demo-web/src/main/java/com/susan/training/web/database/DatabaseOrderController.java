package com.susan.training.web.database;

import com.susan.training.common.ApiResponse;
import com.susan.training.persistence.query.OrderPage;
import com.susan.training.persistence.query.OrderSearchRow;
import com.susan.training.persistence.service.PersistentOrderService;
import com.susan.training.persistence.service.PersistentOrderView;
import com.susan.training.persistence.service.PlaceOrderCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据库版订单接口，与前 14 篇的内存订单接口并行存在，方便对比。
 */
@RestController
@RequestMapping("/api/v1/db/orders")
@Profile({"db", "db-h2"})
@Tag(name = "数据库订单", description = "JPA 写入、MyBatis 联表查询与 PostgreSQL 高级检索")
public class DatabaseOrderController {
    private final PersistentOrderService orderService;

    public DatabaseOrderController(PersistentOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建数据库订单", description = "订单和库存扣减在同一事务中完成")
    public ApiResponse<PersistentOrderView> create(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @Valid @RequestBody PlaceDatabaseOrderRequest request
    ) {
        List<PlaceOrderCommand.PlaceOrderItem> items = request.items()
                .stream()
                .map(item -> new PlaceOrderCommand.PlaceOrderItem(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();
        Map<String, String> metadata =
                request.metadata() == null ? Map.of() : request.metadata();
        return ApiResponse.success(orderService.place(new PlaceOrderCommand(
                tenantId,
                request.customerNo(),
                request.customerName(),
                items,
                metadata
        )));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询数据库订单", description = "使用 JPA 实体关系加载订单与明细")
    public ApiResponse<PersistentOrderView> find(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(orderService.find(tenantId, orderNo));
    }

    @GetMapping
    @Operation(summary = "分页查询订单", description = "使用 MyBatis 多表联查和显式 LIMIT/OFFSET")
    public ApiResponse<OrderPage> page(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestParam(required = false) String customerNo,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(orderService.findPage(
                tenantId,
                customerNo,
                page,
                size
        ));
    }

    @GetMapping("/search")
    @Operation(summary = "全文与 JSONB 检索", description = "组合商品全文索引和订单元数据过滤")
    public ApiResponse<List<OrderSearchRow>> search(
            @RequestHeader(name = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.success(orderService.search(
                tenantId,
                keyword,
                channel,
                limit
        ));
    }
}
