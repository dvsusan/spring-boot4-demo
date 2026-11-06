package com.susan.training.persistence.repository;

import com.susan.training.persistence.entity.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 订单聚合仓库，查询详情时一次抓取订单项。
 */
public interface PurchaseOrderJpaRepository
        extends JpaRepository<PurchaseOrderEntity, Long> {

    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<PurchaseOrderEntity> findByTenantIdAndOrderNo(
            String tenantId,
            String orderNo
    );
}
