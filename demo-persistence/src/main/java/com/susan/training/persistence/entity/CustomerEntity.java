package com.susan.training.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户实体，同一客户编号在不同租户中可以重复。
 */
@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 双向关系按需加载，避免查询客户时无意带出全部订单。
    @OneToMany(mappedBy = "customer")
    private List<PurchaseOrderEntity> orders = new ArrayList<>();

    protected CustomerEntity() {
    }

    public CustomerEntity(String tenantId, String customerNo, String name) {
        this.tenantId = tenantId;
        this.customerNo = customerNo;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
