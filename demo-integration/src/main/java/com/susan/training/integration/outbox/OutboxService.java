package com.susan.training.integration.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 写入与投递状态服务。
 */
@Service
public class OutboxService {
    private final OutboxMessageRepository repository;

    public OutboxService(OutboxMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OutboxMessageEntity append(
            String eventId,
            String aggregateId,
            String eventType,
            String payload
    ) {
        return repository.save(new OutboxMessageEntity(
                eventId,
                aggregateId,
                eventType,
                payload
        ));
    }

    @Transactional(readOnly = true)
    public List<OutboxMessageEntity> pending() {
        return repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }
}
