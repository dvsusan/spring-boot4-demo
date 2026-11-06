package com.susan.training.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {

    @Test
    void createdOrderCanBeFoundByNumber() {
        // 准备：业务模块是普通 Java 类，可以直接创建，不需要启动 Spring。
        OrderService service = new OrderService();

        // 执行：创建一笔金额为 99.00 的订单。
        OrderView created = service.create(
                new CreateOrderCommand("C1001", new BigDecimal("99.00"))
        );

        // 执行：使用返回的订单号再次查询。
        OrderView found = service.findByOrderNo(created.orderNo());

        // 断言：查询结果必须与创建时的业务数据一致。
        assertThat(found.customerId()).isEqualTo("C1001");
        assertThat(found.amount()).isEqualByComparingTo("99.00");
        assertThat(found.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void ordersCanBeFilteredAndUpdated() {
        // 创建两笔不同客户的订单，查询时只能返回目标客户的数据。
        OrderService service = new OrderService();
        OrderView first = service.create(
                new CreateOrderCommand("C1001", new BigDecimal("99.00"))
        );
        service.create(new CreateOrderCommand("C2002", new BigDecimal("199.00")));

        // limit 在业务层仍然生效，避免调用方一次取走无限数据。
        assertThat(service.findByCustomerId("C1001", 10))
                .extracting(OrderView::orderNo)
                .containsExactly(first.orderNo());

        // 不传客户编号时只更新金额，原客户编号保持不变。
        OrderView updated = service.update(first.orderNo(), null, new BigDecimal("88.00"));
        assertThat(updated.customerId()).isEqualTo("C1001");
        assertThat(updated.amount()).isEqualByComparingTo("88.00");
    }
}
