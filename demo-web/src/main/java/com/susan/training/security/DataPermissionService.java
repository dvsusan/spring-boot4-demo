package com.susan.training.security;

import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

/**
 * 对订单执行租户、部门、负责人和敏感列过滤。
 */
public class DataPermissionService {
    private final List<SecuredOrderRecord> orders = List.of(
            new SecuredOrderRecord(
                    "SO-SALES-01", "tenant_acme", "SALES", "analyst",
                    "13812348001", new BigDecimal("199.00"), new BigDecimal("120.00")
            ),
            new SecuredOrderRecord(
                    "SO-SALES-02", "tenant_acme", "SALES", "clerk",
                    "13912348002", new BigDecimal("299.00"), new BigDecimal("180.00")
            ),
            new SecuredOrderRecord(
                    "SO-FIN-01", "tenant_acme", "FINANCE", "finance",
                    "13712348003", new BigDecimal("399.00"), new BigDecimal("240.00")
            ),
            new SecuredOrderRecord(
                    "SO-GLOBEX-01", "tenant_globex", "SALES", "other",
                    "13612348004", new BigDecimal("499.00"), new BigDecimal("310.00")
            )
    );

    public List<SecuredOrderView> visibleOrders(Authentication authentication) {
        SecurityIdentity identity = SecurityIdentity.from(authentication);
        DataScope scope = resolveScope(identity);
        boolean canReadPii = identity.permissions().contains("data:pii:read");
        boolean canReadCost = identity.permissions().contains("data:cost:read");

        return orders.stream()
                .filter(order -> order.tenantId().equals(identity.tenantId()))
                .filter(order -> visibleByScope(order, identity, scope))
                .map(order -> new SecuredOrderView(
                        order.orderId(),
                        order.departmentId(),
                        order.ownerId(),
                        canReadPii
                                ? order.customerPhone()
                                : maskPhone(order.customerPhone()),
                        order.amount(),
                        canReadCost ? order.cost() : null
                ))
                .toList();
    }

    private DataScope resolveScope(SecurityIdentity identity) {
        if (identity.roles().contains("ADMIN")
                || identity.permissions().contains("data:all")) {
            return DataScope.ALL;
        }
        if (identity.permissions().contains("data:department")) {
            return DataScope.DEPARTMENT;
        }
        return DataScope.SELF;
    }

    private boolean visibleByScope(
            SecuredOrderRecord order,
            SecurityIdentity identity,
            DataScope scope
    ) {
        return switch (scope) {
            case ALL -> true;
            case DEPARTMENT -> order.departmentId()
                    .equals(identity.departmentId());
            case SELF -> order.ownerId().equals(identity.username());
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3)
                + "****"
                + phone.substring(phone.length() - 4);
    }
}
