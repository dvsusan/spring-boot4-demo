package com.susan.training.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

/**
 * 教学账号仓库。
 *
 * <p>生产环境应替换为数据库或企业身份源，不能把账号和密码保存在源码中。</p>
 */
public class Chapter6UserDetailsService implements UserDetailsService {
    private final Map<String, Chapter6UserDetails> users;

    public Chapter6UserDetailsService(PasswordEncoder passwordEncoder) {
        this.users = Map.of(
                "admin", new Chapter6UserDetails(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "tenant_acme",
                        "HQ",
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
                ),
                "analyst", new Chapter6UserDetails(
                        "analyst",
                        passwordEncoder.encode("analyst123"),
                        "tenant_acme",
                        "SALES",
                        List.of("ANALYST"),
                        List.of(
                                "order:read",
                                "data:department",
                                "data:pii:read"
                        )
                ),
                "clerk", new Chapter6UserDetails(
                        "clerk",
                        passwordEncoder.encode("clerk123"),
                        "tenant_acme",
                        "SALES",
                        List.of("CLERK"),
                        List.of("order:read")
                )
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Chapter6UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return user;
    }
}
