package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 按“用户 + Idempotency-Key”返回首次请求的原始响应。
 */
public class IdempotencyFilter extends OncePerRequestFilter {
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            IdempotencyStore store,
            ObjectMapper objectMapper
    ) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !RequestSigningFilter.PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (rawKey == null || rawKey.length() < 8 || rawKey.length() > 128) {
            writeError(response, 400, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key 长度必须在 8 到 128 之间");
            return;
        }

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        String key = username + ":" + RequestSigningFilter.PATH + ":" + rawKey;

        IdempotencyStore.Reservation reservation;
        try {
            reservation = store.reserve(key);
        } catch (IllegalStateException exception) {
            writeError(response, 409, "REQUEST_IN_PROGRESS", exception.getMessage());
            return;
        }
        if (reservation.replay()) {
            replay(response, reservation);
            return;
        }

        ContentCachingResponseWrapper wrapped =
                new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapped);
            store.complete(reservation, new IdempotencyStore.StoredResponse(
                    wrapped.getStatus(),
                    wrapped.getContentType(),
                    wrapped.getContentAsByteArray()
            ));
            wrapped.copyBodyToResponse();
        } catch (Exception exception) {
            store.abort(reservation);
            throw exception;
        }
    }

    private void replay(
            HttpServletResponse response,
            IdempotencyStore.Reservation reservation
    ) throws IOException {
        IdempotencyStore.StoredResponse stored;
        try {
            stored = reservation.future().get(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("读取幂等响应被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            writeError(response, 409, "REQUEST_IN_PROGRESS", "相同请求仍在处理中");
            return;
        }
        response.setStatus(stored.status());
        response.setContentType(stored.contentType());
        response.setHeader("X-Idempotent-Replay", "true");
        response.getOutputStream().write(stored.body());
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(code, message)
        );
    }
}
