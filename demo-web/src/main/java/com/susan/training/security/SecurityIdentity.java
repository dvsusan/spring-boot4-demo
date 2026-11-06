package com.susan.training.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * 业务代码使用的安全身份快照，不直接依赖 JWT 或 Session 实现。
 */
public record SecurityIdentity(
        String username,
        String tenantId,
        String departmentId,
        List<String> roles,
        List<String> permissions
) {
    public static SecurityIdentity from(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return new SecurityIdentity(
                    jwt.getSubject(),
                    jwt.getClaimAsString("tenant_id"),
                    jwt.getClaimAsString("department_id"),
                    stringList(jwt, "roles"),
                    stringList(jwt, "permissions")
            );
        }
        if (authentication != null
                && authentication.getPrincipal() instanceof Chapter6UserDetails user) {
            return new SecurityIdentity(
                    user.getUsername(),
                    user.getTenantId(),
                    user.getDepartmentId(),
                    user.getRoles(),
                    user.getPermissions()
            );
        }
        throw new IllegalStateException("当前认证对象无法转换为业务身份");
    }

    private static List<String> stringList(Jwt jwt, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        return values == null ? List.of() : List.copyOf(values);
    }
}
