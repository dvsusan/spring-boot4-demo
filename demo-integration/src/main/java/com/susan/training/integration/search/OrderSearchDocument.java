package com.susan.training.integration.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Elasticsearch 中的订单搜索文档。
 */
@Document(indexName = "chapter4-orders")
public class OrderSearchDocument {
    @Id
    private String orderNo;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;

    protected OrderSearchDocument() {
    }

    public OrderSearchDocument(
            String orderNo,
            String customerId,
            BigDecimal amount,
            String status,
            Instant createdAt
    ) {
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
