package com.susan.training.event;

import com.susan.training.order.OrderView;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内的实时事件总线，只用于单实例教学。
 *
 * <p>多实例部署时应把事件发布到 Redis Pub/Sub、RocketMQ 或其他广播通道，
 * 再把消息分发到各实例上的 SSE/WebSocket 连接。</p>
 */
@Component
public class OrderEventBroker {
    private static final Logger log = LoggerFactory.getLogger(OrderEventBroker.class);

    private final ObjectMapper objectMapper;
    private final Map<String, SseSubscription> sseSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSubscription> webSocketSubscriptions = new ConcurrentHashMap<>();

    public OrderEventBroker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 创建只接收指定客户事件的 SSE 连接。
     */
    public SseEmitter subscribeSse(String customerId) {
        String subscriptionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(30_000L);
        sseSubscriptions.put(subscriptionId, new SseSubscription(customerId, emitter));

        emitter.onCompletion(() -> sseSubscriptions.remove(subscriptionId));
        emitter.onTimeout(() -> sseSubscriptions.remove(subscriptionId));
        emitter.onError(error -> sseSubscriptions.remove(subscriptionId));

        try {
            // 先发送 connected 事件，客户端可以据此确认订阅已经建立。
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("customerId", customerId, "transport", "sse")));
        } catch (IOException exception) {
            sseSubscriptions.remove(subscriptionId);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    /**
     * 保存 WebSocket 会话及其订阅的客户编号。
     */
    public void registerWebSocket(String customerId, WebSocketSession session) {
        webSocketSubscriptions.put(session.getId(), new WebSocketSubscription(customerId, session));
    }

    public void unregisterWebSocket(String sessionId) {
        webSocketSubscriptions.remove(sessionId);
    }

    /**
     * 订单创建或更新后向匹配客户的在线连接广播事件。
     */
    public void publish(String eventType, OrderView order) {
        OrderEvent event = new OrderEvent(
                UUID.randomUUID().toString(),
                eventType,
                order.orderNo(),
                order.customerId(),
                order.status().name(),
                Instant.now()
        );
        sendToSse(event);
        sendToWebSocket(event);
    }

    private void sendToSse(OrderEvent event) {
        sseSubscriptions.forEach((id, subscription) -> {
            if (!subscription.customerId().equals(event.customerId())) {
                return;
            }
            try {
                subscription.emitter().send(SseEmitter.event()
                        .id(event.eventId())
                        .name(event.type())
                        .data(event));
            } catch (Exception exception) {
                // 连接断开后先移除订阅，避免后续每次广播都重复失败。
                sseSubscriptions.remove(id);
                log.debug("remove closed SSE subscription: {}", id);
            }
        });
    }

    private void sendToWebSocket(OrderEvent event) {
        webSocketSubscriptions.forEach((id, subscription) -> {
            if (!subscription.customerId().equals(event.customerId())
                    || !subscription.session().isOpen()) {
                return;
            }
            try {
                String payload = objectMapper.writeValueAsString(event);
                subscription.session().sendMessage(new TextMessage(payload));
            } catch (Exception exception) {
                webSocketSubscriptions.remove(id);
                log.debug("remove failed WebSocket subscription: {}", id);
            }
        });
    }

    @PreDestroy
    public void closeAll() {
        // 正常停机时主动完成连接，浏览器不会一直等待已经失效的通道。
        sseSubscriptions.values().forEach(subscription -> subscription.emitter().complete());
        sseSubscriptions.clear();
        webSocketSubscriptions.clear();
    }

    private record SseSubscription(String customerId, SseEmitter emitter) {
    }

    private record WebSocketSubscription(String customerId, WebSocketSession session) {
    }
}
