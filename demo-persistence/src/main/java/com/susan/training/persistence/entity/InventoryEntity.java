package com.susan.training.persistence.entity;

import com.susan.training.persistence.service.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 商品库存实体，版本字段用于乐观锁保护。
 */
@Entity
@Table(name = "inventory")
public class InventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private long version;

    protected InventoryEntity() {
    }

    public InventoryEntity(String tenantId, String productId, int availableQuantity) {
        this.tenantId = tenantId;
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    /**
     * 库存不足时直接拒绝，调用方事务会整体回滚。
     */
    public void reserve(int quantity) {
        if (quantity <= 0 || availableQuantity < quantity) {
            throw new InsufficientStockException(productId);
        }
        availableQuantity -= quantity;
    }

    public String getProductId() {
        return productId;
    }

    public Long getId() {
        return id;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        // 仅供教学库存初始化使用，真实库存变更应走入库、出库领域方法。
        this.availableQuantity = availableQuantity;
    }
}
