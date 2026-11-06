package com.susan.training.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 将 OIDC 用户组映射成 Spring Security 可识别的角色和权限。
 */
public class SsoAuthoritiesMapper implements GrantedAuthoritiesMapper {
    private final SsoIdentityMapper identityMapper;

    public SsoAuthoritiesMapper(SsoIdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities
    ) {
        OidcUserAuthority oidcAuthority = authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .findFirst()
                .orElse(null);
        if (oidcAuthority == null) {
            return authorities;
        }

        boolean emailVerified = Boolean.TRUE.equals(
                oidcAuthority.getIdToken().getClaimAsBoolean("email_verified")
        );
        List<String> groups = oidcAuthority.getIdToken()
                .getClaimAsStringList("groups");
        SsoIdentityMapper.SsoIdentity identity = identityMapper.map(
                emailVerified,
                groups == null ? List.of() : groups
        );
        List<GrantedAuthority> mapped = new ArrayList<>();
        identity.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(mapped::add);
        identity.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(mapped::add);
        return mapped;
    }
}
