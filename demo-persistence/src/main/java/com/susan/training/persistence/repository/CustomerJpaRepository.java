package com.susan.training.persistence.repository;

import com.susan.training.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 客户实体仓库。
 */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByTenantIdAndCustomerNo(
            String tenantId,
            String customerNo
    );
}
