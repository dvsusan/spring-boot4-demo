package com.susan.training.persistence.repository;

import com.susan.training.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审计日志仓库。
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {
}
