package com.susan.training.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SsoIdentityMapperTest {
    private final SsoIdentityMapper mapper = new SsoIdentityMapper();

    @Test
    void unverifiedEmailGetsNoApplicationAuthorities() {
        SsoIdentityMapper.SsoIdentity identity =
                mapper.map(false, List.of("platform-admins"));

        assertThat(identity.roles()).isEmpty();
        assertThat(identity.permissions()).isEmpty();
    }

    @Test
    void enterpriseGroupsMapToApplicationRolesAndPermissions() {
        SsoIdentityMapper.SsoIdentity admin =
                mapper.map(true, List.of("platform-admins"));
        SsoIdentityMapper.SsoIdentity analyst =
                mapper.map(true, List.of("sales-analysts"));

        assertThat(admin.roles()).containsExactly("ADMIN");
        assertThat(admin.permissions()).contains("payment:write", "data:all");
        assertThat(analyst.roles()).containsExactly("ANALYST");
        assertThat(analyst.permissions()).contains("order:read", "data:department");
    }
}
