package com.susan.training.security;

import com.susan.training.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 阻止用户通过伪造 X-Tenant-Id 进入其他租户。
 */
public class TenantAccessFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final ObjectMapper objectMapper;

    public TenantAccessFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/security/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String tokenTenant = jwtAuthentication.getToken()
                    .getClaimAsString("tenant_id");
            String requestedTenant = request.getHeader(TENANT_HEADER);
            if (requestedTenant != null
                    && !requestedTenant.isBlank()
                    && !requestedTenant.equals(tokenTenant)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(
                        response.getOutputStream(),
                        ApiResponse.failure(
                                "TENANT_MISMATCH",
                                "请求租户与登录身份不一致"
                        )
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
