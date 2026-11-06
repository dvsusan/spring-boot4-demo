package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 需要签名和幂等键的支付接口。
 */
@RestController
@RequestMapping("/api/v1/security/payments")
@ConditionalOnProperty(
        prefix = "demo.chapter6",
        name = "enabled",
        havingValue = "true"
)
public class PaymentController {
    @PostMapping
    @PreAuthorize("hasAuthority('payment:write')")
    public ApiResponse<PaymentResult> pay(
            @Valid @RequestBody PaymentRequest request
    ) {
        // 演示接口返回交易号；真实支付必须调用支付网关并保存业务状态。
        return ApiResponse.success(new PaymentResult(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.amount()
        ));
    }

    public record PaymentRequest(
            @NotBlank String orderId,
            @DecimalMin("0.01") BigDecimal amount
    ) {
    }

    public record PaymentResult(
            String paymentId,
            String orderId,
            BigDecimal amount
    ) {
    }
}
