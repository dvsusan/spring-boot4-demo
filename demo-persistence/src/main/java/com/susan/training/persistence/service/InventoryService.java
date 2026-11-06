package com.susan.training.persistence.service;

import com.susan.training.persistence.entity.InventoryEntity;
import com.susan.training.persistence.repository.InventoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教学环境中的库存初始化和查询服务。
 */
@Service
public class InventoryService {
    private final InventoryJpaRepository inventoryRepository;

    public InventoryService(InventoryJpaRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryView setQuantity(String tenantId, String productId, int quantity) {
        InventoryEntity inventory = inventoryRepository
                .findByTenantIdAndProductId(tenantId, productId)
                .orElseGet(() -> new InventoryEntity(tenantId, productId, quantity));
        if (inventory.getId() != null) {
            inventory.setAvailableQuantity(quantity);
        }
        InventoryEntity saved = inventoryRepository.save(inventory);
        return new InventoryView(saved.getProductId(), saved.getAvailableQuantity());
    }

    @Transactional(readOnly = true)
    public InventoryView getQuantity(String tenantId, String productId) {
        InventoryEntity inventory = inventoryRepository
                .findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new InsufficientStockException(productId));
        return new InventoryView(inventory.getProductId(), inventory.getAvailableQuantity());
    }

    /**
     * 库存查询响应。
     */
    public record InventoryView(String productId, int availableQuantity) {
    }
}
