package com.susan.training.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 教学用户除了账号密码，还携带租户、部门和权限声明。
 */
public class Chapter6UserDetails extends User {
    private final String tenantId;
    private final String departmentId;
    private final List<String> roles;
    private final List<String> permissions;

    public Chapter6UserDetails(
            String username,
            String password,
            String tenantId,
            String departmentId,
            List<String> roles,
            List<String> permissions
    ) {
        super(
                username,
                password,
                authorities(roles, permissions)
        );
        this.tenantId = tenantId;
        this.departmentId = departmentId;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    private static List<GrantedAuthority> authorities(
            List<String> roles,
            List<String> permissions
    ) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }
}
