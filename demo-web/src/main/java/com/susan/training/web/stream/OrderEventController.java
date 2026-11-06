package com.susan.training.web.stream;

import com.susan.training.event.OrderEventBroker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通过 SSE 持续推送指定客户的订单事件。
 */
@RestController
@RequestMapping("/api/v1/events/orders")
@Tag(name = "订单实时事件", description = "SSE 推送")
public class OrderEventController {
    private final OrderEventBroker eventBroker;

    public OrderEventController(OrderEventBroker eventBroker) {
        this.eventBroker = eventBroker;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅订单事件", description = "保持 HTTP 连接，持续接收 text/event-stream")
    public SseEmitter subscribe(@RequestParam @NotBlank String customerId) {
        return eventBroker.subscribeSse(customerId);
    }
}
