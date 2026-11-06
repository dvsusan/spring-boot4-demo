package com.susan.training.web.stream;

import com.susan.training.event.OrderEventBroker;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 订单 WebSocket 处理器，从握手 URL 的 customerId 参数建立订阅关系。
 */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
    private final OrderEventBroker eventBroker;
    private final ObjectMapper objectMapper;

    public OrderWebSocketHandler(OrderEventBroker eventBroker, ObjectMapper objectMapper) {
        this.eventBroker = eventBroker;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String customerId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("customerId");
        if (customerId == null || customerId.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("customerId is required"));
            return;
        }
        eventBroker.registerWebSocket(customerId, session);
        String connected = objectMapper.writeValueAsString(Map.of(
                "type", "connected",
                "customerId", customerId,
                "transport", "websocket"
        ));
        session.sendMessage(new TextMessage(connected));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        eventBroker.unregisterWebSocket(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        eventBroker.unregisterWebSocket(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
