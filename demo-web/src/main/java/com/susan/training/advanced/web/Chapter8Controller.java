package com.susan.training.advanced.web;

import com.susan.training.advanced.observability.Chapter8MonitoringService;
import com.susan.training.common.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 可观测性章节示例接口。
 */
@RestController
@RequestMapping("/api/v1/chapter8")
@ConditionalOnProperty(
        prefix = "demo.chapter8",
        name = "enabled",
        havingValue = "true"
)
public class Chapter8Controller {
    private final Chapter8MonitoringService monitoringService;

    public Chapter8Controller(Chapter8MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/observability/orders/{orderNo}")
    public ApiResponse<Chapter8MonitoringService.OrderObservation> order(
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(monitoringService.observe(orderNo));
    }
}
