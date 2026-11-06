package com.susan.training.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每次 HTTP 请求生成 traceId，并写入 MDC。
 *
 * <p>日志格式通过 %X{traceId} 读取该值。请求结束后必须清理 MDC，
 * 避免线程复用时把上一请求的链路标识带到下一请求。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startedAt = System.nanoTime();

        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("request completed: method={}, path={}, status={}, elapsedMs={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsedMillis);
            MDC.remove("traceId");
        }
    }
}
