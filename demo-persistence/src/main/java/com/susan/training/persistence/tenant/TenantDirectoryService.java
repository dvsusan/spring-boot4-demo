package com.susan.training.persistence.tenant;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通过动态数据源读写当前租户 Schema 中的演示记录。
 */
@Service
@Profile("db")
public class TenantDirectoryService {
    private final TenantDataSourceRegistry dataSources;

    public TenantDirectoryService(TenantDataSourceRegistry dataSources) {
        this.dataSources = dataSources;
    }

    public TenantNote upsert(String key, String value) {
        requireTenant();
        dataSources.jdbcTemplate().update("""
                INSERT INTO tenant_notes (note_key, note_value, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (note_key)
                DO UPDATE SET note_value = EXCLUDED.note_value,
                              updated_at = CURRENT_TIMESTAMP
                """, key, value);
        return find(key);
    }

    public TenantNote find(String key) {
        requireTenant();
        return dataSources.jdbcTemplate().queryForObject("""
                SELECT note_key, note_value
                FROM tenant_notes
                WHERE note_key = ?
                """, (resultSet, rowNum) -> new TenantNote(
                resultSet.getString("note_key"),
                resultSet.getString("note_value"),
                TenantContext.get()
        ), key);
    }

    public List<TenantNote> findAll() {
        requireTenant();
        return dataSources.jdbcTemplate().query("""
                SELECT note_key, note_value
                FROM tenant_notes
                ORDER BY note_key
                """, (resultSet, rowNum) -> new TenantNote(
                resultSet.getString("note_key"),
                resultSet.getString("note_value"),
                TenantContext.get()
        ));
    }

    private void requireTenant() {
        String tenantId = TenantContext.get();
        if (!TenantDataSourceRegistry.ACME.equals(tenantId)
                && !TenantDataSourceRegistry.GLOBEX.equals(tenantId)) {
            throw new UnknownTenantException(tenantId);
        }
    }

    /**
     * 租户内记录的响应结构。
     */
    public record TenantNote(String key, String value, String tenantId) {
    }
}
