package com.susan.training.web.database;

import com.susan.training.persistence.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从请求头提取租户标识，并在请求结束后清理 ThreadLocal。
 */
@Component
@Profile("db")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.set(tenantId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Web 容器会复用线程，必须防止租户标识泄漏到下一请求。
            TenantContext.clear();
        }
    }
}
