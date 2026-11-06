package com.susan.training.security;

import java.util.List;

/**
 * 把企业 OIDC 的用户组映射为应用角色和权限。
 */
public class SsoIdentityMapper {
    public SsoIdentity map(boolean emailVerified, List<String> groups) {
        if (!emailVerified) {
            return new SsoIdentity(List.of(), List.of());
        }
        if (groups.contains("platform-admins")) {
            return new SsoIdentity(
                    List.of("ADMIN"),
                    List.of(
                            "order:read",
                            "order:write",
                            "order:delete",
                            "payment:write",
                            "data:all",
                            "data:pii:read",
                            "data:cost:read"
                    )
            );
        }
        if (groups.contains("sales-analysts")) {
            return new SsoIdentity(
                    List.of("ANALYST"),
                    List.of("order:read", "data:department", "data:pii:read")
            );
        }
        return new SsoIdentity(List.of("EMPLOYEE"), List.of("order:read"));
    }

    public record SsoIdentity(List<String> roles, List<String> permissions) {
    }
}
