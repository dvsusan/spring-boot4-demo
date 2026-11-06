package com.susan.training.integration.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Outbox 消息持久化入口。
 */
public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, Long> {
    List<OutboxMessageEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
