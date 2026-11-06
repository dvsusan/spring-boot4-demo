package com.susan.training.persistence.shard;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 应用层分片示例，分片键为订单号哈希。
 */
@Service
@Profile("db")
public class ShardedOrderService {
    private final JdbcTemplate jdbcTemplate;

    public ShardedOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ShardOrderView create(
            String tenantId,
            String customerNo,
            BigDecimal amount
    ) {
        String orderNo = "SH" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 14)
                .toUpperCase(Locale.ROOT);
        int shard = route(orderNo);
        String table = tableName(shard);
        // 表名只来自内部枚举结果，不能接受客户端传入。
        jdbcTemplate.update("""
                INSERT INTO %s
                    (order_no, tenant_id, customer_no, amount, status, created_at)
                VALUES (?, ?, ?, ?, 'CREATED', CURRENT_TIMESTAMP)
                """.formatted(table), orderNo, tenantId, customerNo, amount);
        return findByOrderNo(tenantId, orderNo);
    }

    public ShardOrderView findByOrderNo(String tenantId, String orderNo) {
        int shard = route(orderNo);
        return jdbcTemplate.queryForObject("""
                SELECT order_no, customer_no, amount, status, created_at
                FROM %s
                WHERE tenant_id = ? AND order_no = ?
                """.formatted(tableName(shard)),
                (resultSet, rowNum) -> map(resultSet, shard),
                tenantId,
                orderNo);
    }

    /**
     * 跨分片查询必须分别访问每个分片，再在应用层归并。
     */
    public List<ShardOrderView> findByCustomer(String tenantId, String customerNo) {
        return jdbcTemplate.query("""
                SELECT *
                FROM (
                    SELECT order_no, customer_no, amount, status, created_at
                    FROM shard_order_0
                    WHERE tenant_id = ? AND customer_no = ?
                    UNION ALL
                    SELECT order_no, customer_no, amount, status, created_at
                    FROM shard_order_1
                    WHERE tenant_id = ? AND customer_no = ?
                ) shard_result
                ORDER BY created_at DESC, order_no DESC
                """, (resultSet, rowNum) -> {
                    String orderNo = resultSet.getString("order_no");
                    return map(resultSet, route(orderNo));
                }, tenantId, customerNo, tenantId, customerNo);
    }

    public int route(String orderNo) {
        return Math.floorMod(orderNo.hashCode(), 2);
    }

    private String tableName(int shard) {
        return switch (shard) {
            case 0 -> "shard_order_0";
            case 1 -> "shard_order_1";
            default -> throw new IllegalArgumentException("非法分片: " + shard);
        };
    }

    private ShardOrderView map(java.sql.ResultSet resultSet, int shard)
            throws java.sql.SQLException {
        return new ShardOrderView(
                resultSet.getString("order_no"),
                resultSet.getString("customer_no"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("status"),
                shard,
                resultSet.getObject("created_at", Instant.class)
        );
    }
}
