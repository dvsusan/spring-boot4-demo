package com.susan.training.integration.outbox;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxServiceTest {

    @Test
    void appendCreatesPendingMessage() {
        // Repository 用 Mock 替代，测试只关注 Outbox 初始状态。
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        when(repository.save(any(OutboxMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        OutboxService service = new OutboxService(repository);
        OutboxMessageEntity message = service.append(
                "evt-1",
                "order-1",
                "ORDER_CREATED",
                "{\"orderNo\":\"order-1\"}"
        );

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getEventId()).isEqualTo("evt-1");
    }
}
