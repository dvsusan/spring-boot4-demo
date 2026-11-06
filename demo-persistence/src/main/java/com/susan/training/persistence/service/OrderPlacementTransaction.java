package com.susan.training.persistence.service;

import com.susan.training.persistence.entity.CustomerEntity;
import com.susan.training.persistence.entity.InventoryEntity;
import com.susan.training.persistence.entity.OrderItemEntity;
import com.susan.training.persistence.entity.PurchaseOrderEntity;
import com.susan.training.persistence.repository.CustomerJpaRepository;
import com.susan.training.persistence.repository.InventoryJpaRepository;
import com.susan.training.persistence.repository.PurchaseOrderJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 订单写入与库存扣减必须位于同一个本地事务。
 */
@Service
public class OrderPlacementTransaction {
    private final CustomerJpaRepository customerRepository;
    private final PurchaseOrderJpaRepository orderRepository;
    private final InventoryJpaRepository inventoryRepository;

    public OrderPlacementTransaction(
            CustomerJpaRepository customerRepository,
            PurchaseOrderJpaRepository orderRepository,
            InventoryJpaRepository inventoryRepository
    ) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public PersistentOrderView place(PlaceOrderCommand command) {
        CustomerEntity customer = customerRepository
                .findByTenantIdAndCustomerNo(
                        command.tenantId(),
                        command.customerNo()
                )
                .orElseGet(() -> customerRepository.saveAndFlush(
                        new CustomerEntity(
                                command.tenantId(),
                                command.customerNo(),
                                command.customerName()
                        )
                ));

        String orderNo = "PO" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 14)
                .toUpperCase(Locale.ROOT);
        BigDecimal total = command.items()
                .stream()
                .map(item -> item.unitPrice().multiply(
                        BigDecimal.valueOf(item.quantity())
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PurchaseOrderEntity order = new PurchaseOrderEntity(
                orderNo,
                command.tenantId(),
                customer,
                total,
                command.metadata()
        );

        for (PlaceOrderCommand.PlaceOrderItem item : command.items()) {
            // 悲观写锁阻止两个事务同时扣减同一商品库存。
            InventoryEntity inventory = inventoryRepository
                    .findForUpdate(command.tenantId(), item.productId())
                    .orElseThrow(() -> new InsufficientStockException(item.productId()));
            inventory.reserve(item.quantity());
            order.addItem(new OrderItemEntity(
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.unitPrice()
            ));
        }

        PurchaseOrderEntity saved = orderRepository.saveAndFlush(order);
        return toView(saved);
    }

    private PersistentOrderView toView(PurchaseOrderEntity order) {
        List<PersistentOrderView.Item> items = order.getItems()
                .stream()
                .map(item -> new PersistentOrderView.Item(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();
        return new PersistentOrderView(
                order.getOrderNo(),
                order.getCustomer().getCustomerNo(),
                order.getCustomer().getName(),
                order.getAmount(),
                order.getStatus(),
                items,
                order.getMetadata(),
                order.getCreatedAt()
        );
    }
}
