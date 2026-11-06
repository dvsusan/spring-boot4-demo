package com.susan.training.persistence.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 为两个租户 Schema 创建独立连接池，并统一挂到路由数据源。
 */
@Component
@Profile("db")
public class TenantDataSourceRegistry {
    public static final String ACME = "tenant_acme";
    public static final String GLOBEX = "tenant_globex";

    private final Map<String, HikariDataSource> dataSources = new LinkedHashMap<>();
    private final TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();
    private final JdbcTemplate jdbcTemplate;

    public TenantDataSourceRegistry(
            @Value("${spring.datasource.url}") String baseUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driver
    ) {
        dataSources.put(ACME, createDataSource(baseUrl, username, password, driver, ACME));
        dataSources.put(
                GLOBEX,
                createDataSource(baseUrl, username, password, driver, GLOBEX)
        );
        routingDataSource.setTargetDataSources(new LinkedHashMap<>(dataSources));
        routingDataSource.setDefaultTargetDataSource(dataSources.get(ACME));
        routingDataSource.afterPropertiesSet();
        jdbcTemplate = new JdbcTemplate(routingDataSource);
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    private HikariDataSource createDataSource(
            String baseUrl,
            String username,
            String password,
            String driver,
            String schema
    ) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(appendSchema(baseUrl, schema));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(4);
        config.setPoolName("tenant-" + schema);
        return new HikariDataSource(config);
    }

    private String appendSchema(String url, String schema) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "currentSchema=" + schema;
    }

    @PreDestroy
    public void close() {
        dataSources.values().forEach(HikariDataSource::close);
    }
}
