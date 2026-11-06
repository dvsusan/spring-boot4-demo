package com.susan.training.persistence.mapper;

import com.susan.training.persistence.query.OrderSearchRow;
import com.susan.training.persistence.query.OrderSummaryRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 复杂联表、分页和 PostgreSQL 高级查询的 MyBatis Mapper。
 */
public interface OrderQueryMapper {

    List<OrderSummaryRow> findPage(
            @Param("tenantId") String tenantId,
            @Param("customerNo") String customerNo,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long count(
            @Param("tenantId") String tenantId,
            @Param("customerNo") String customerNo
    );

    List<OrderSearchRow> search(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("channel") String channel,
            @Param("limit") int limit
    );
}
