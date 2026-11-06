package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 对写支付接口执行 HMAC 签名、时间窗和 nonce 防重放校验。
 */
public class RequestSigningFilter extends OncePerRequestFilter {
    public static final String PATH = "/api/v1/security/payments";
    public static final String TIMESTAMP_HEADER = "X-Signature-Timestamp";
    public static final String NONCE_HEADER = "X-Signature-Nonce";
    public static final String SIGNATURE_HEADER = "X-Signature";

    private final RequestSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public RequestSigningFilter(
            RequestSignatureService signatureService,
            ObjectMapper objectMapper
    ) {
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyHttpServletRequest wrapped =
                new CachedBodyHttpServletRequest(request);
        try {
            signatureService.verify(
                    wrapped.getMethod(),
                    wrapped.getRequestURI(),
                    wrapped.getHeader(TIMESTAMP_HEADER),
                    wrapped.getHeader(NONCE_HEADER),
                    wrapped.getHeader(SIGNATURE_HEADER),
                    wrapped.getBody()
            );
            filterChain.doFilter(wrapped, response);
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.failure("INVALID_SIGNATURE", exception.getMessage())
            );
        }
    }
}
