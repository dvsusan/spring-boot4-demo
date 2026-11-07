package com.susan.training.advanced.web;

import com.susan.training.advanced.ai.KnowledgeAnswerService;
import com.susan.training.advanced.async.OrderOverviewService;
import com.susan.training.advanced.performance.CursorOrderPage;
import com.susan.training.advanced.performance.CursorOrderService;
import com.susan.training.advanced.resilience.RiskAssessmentService;
import com.susan.training.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 第七章高级能力接口。
 */
@RestController
@RequestMapping("/api/v1/chapter7")
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class Chapter7Controller {
    private final OrderOverviewService overviewService;
    private final RiskAssessmentService riskService;
    private final CursorOrderService cursorOrderService;
    private final KnowledgeAnswerService knowledgeAnswerService;

    public Chapter7Controller(
            OrderOverviewService overviewService,
            RiskAssessmentService riskService,
            CursorOrderService cursorOrderService,
            KnowledgeAnswerService knowledgeAnswerService
    ) {
        this.overviewService = overviewService;
        this.riskService = riskService;
        this.cursorOrderService = cursorOrderService;
        this.knowledgeAnswerService = knowledgeAnswerService;
    }

    @GetMapping("/async/orders/{orderNo}")
    public ApiResponse<OrderOverviewService.OrderOverview> orderOverview(
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(overviewService.aggregate(orderNo));
    }

    @GetMapping("/resilience/risk/{orderNo}")
    public ApiResponse<RiskAssessmentService.RiskResult> risk(
            @PathVariable String orderNo,
            @RequestParam(defaultValue = "false") boolean fail
    ) {
        return ApiResponse.success(riskService.assess(orderNo, fail));
    }

    @GetMapping("/resilience/state")
    public ApiResponse<Map<String, String>> circuitState() {
        return ApiResponse.success(Map.of(
                "circuitBreaker", riskService.circuitState()
        ));
    }

    @GetMapping("/performance/orders")
    public ApiResponse<CursorOrderPage> cursorPage(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(cursorOrderService.page(cursor, limit));
    }

    @PostMapping("/ai/chat")
    public ApiResponse<KnowledgeAnswerService.KnowledgeAnswer> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        return ApiResponse.success(
                knowledgeAnswerService.answer(request.question())
        );
    }

    public record ChatRequest(@NotBlank String question) {
    }
}
