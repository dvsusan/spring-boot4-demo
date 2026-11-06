package com.susan.training.database;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * 所有 PostgreSQL 集成测试共享一个嵌入式实例。
 */
abstract class PostgresIntegrationTestSupport {
    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        EmbeddedPostgres instance = startPostgres();
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://localhost:" + instance.getPort() + "/postgres"
        );
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    protected static int postgresPort() {
        return startPostgres().getPort();
    }

    private static synchronized EmbeddedPostgres startPostgres() {
        if (postgres != null) {
            return postgres;
        }
        try {
            postgres = EmbeddedPostgres.builder()
                    .setPort(0)
                    .setRegisterShutdownHook(true)
                    // macOS 的 C.UTF-8 不一定能被 PostgreSQL initdb 识别。
                    .setLocaleConfig("locale", "C")
                    // 沙箱可能禁止 System V 共享内存，改用 mmap 完成测试启动。
                    .setServerConfig("shared_memory_type", "mmap")
                    .start();
            return postgres;
        } catch (IOException exception) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", exception);
        }
    }
}
