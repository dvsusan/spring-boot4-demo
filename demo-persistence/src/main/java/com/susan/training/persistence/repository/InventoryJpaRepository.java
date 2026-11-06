package com.susan.training.persistence.repository;

import com.susan.training.persistence.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 库存仓库，扣减前对目标行加悲观写锁。
 */
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByTenantIdAndProductId(
            String tenantId,
            String productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory
            from InventoryEntity inventory
            where inventory.tenantId = :tenantId
              and inventory.productId = :productId
            """)
    Optional<InventoryEntity> findForUpdate(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId
    );
}
