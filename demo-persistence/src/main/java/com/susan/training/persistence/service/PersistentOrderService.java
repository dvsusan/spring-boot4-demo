package com.susan.training.persistence.service;

import com.susan.training.persistence.entity.PurchaseOrderEntity;
import com.susan.training.persistence.mapper.OrderQueryMapper;
import com.susan.training.persistence.query.OrderPage;
import com.susan.training.persistence.query.OrderSearchRow;
import com.susan.training.persistence.repository.PurchaseOrderJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 数据库订单应用服务，负责事务编排和 DTO 转换。
 */
@Service
public class PersistentOrderService {
    private final OrderPlacementTransaction placementTransaction;
    private final AuditService auditService;
    private final PurchaseOrderJpaRepository orderRepository;
    private final OrderQueryMapper queryMapper;

    public PersistentOrderService(
            OrderPlacementTransaction placementTransaction,
            AuditService auditService,
            PurchaseOrderJpaRepository orderRepository,
            OrderQueryMapper queryMapper
    ) {
        this.placementTransaction = placementTransaction;
        this.auditService = auditService;
        this.orderRepository = orderRepository;
        this.queryMapper = queryMapper;
    }

    public PersistentOrderView place(PlaceOrderCommand command) {
        try {
            // 真正的事务方法位于另一个 Bean，避免同类自调用绕过代理。
            return placementTransaction.place(command);
        } catch (InsufficientStockException exception) {
            auditService.recordFailure(command.tenantId(), exception.getMessage());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PersistentOrderView find(String tenantId, String orderNo) {
        PurchaseOrderEntity order = orderRepository
                .findByTenantIdAndOrderNo(tenantId, orderNo)
                .orElseThrow(() -> new OrderNotFoundInDatabaseException(orderNo));
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderPage findPage(String tenantId, String customerNo, int page, int size) {
        long offset = (long) (page - 1) * size;
        return new OrderPage(
                queryMapper.findPage(tenantId, customerNo, offset, size),
                queryMapper.count(tenantId, customerNo),
                page,
                size
        );
    }

    @Transactional(readOnly = true)
    public List<OrderSearchRow> search(
            String tenantId,
            String keyword,
            String channel,
            int limit
    ) {
        return queryMapper.search(tenantId, keyword, channel, limit);
    }
}
