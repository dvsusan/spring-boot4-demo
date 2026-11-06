package com.susan.training.persistence.service;

import com.susan.training.persistence.entity.AuditLogEntity;
import com.susan.training.persistence.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 独立事务写入审计记录，订单回滚时审计仍然保留。
 */
@Service
public class AuditService {
    private final AuditLogJpaRepository auditRepository;

    public AuditService(AuditLogJpaRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String tenantId, String detail) {
        // REQUIRES_NEW 会暂停调用方事务，并在独立事务中提交这条记录。
        auditRepository.save(new AuditLogEntity(
                tenantId,
                "ORDER_REJECTED",
                detail
        ));
    }
}
