package com.susan.training.integration.search;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.Optional;

/**
 * 订单搜索文档读写服务。
 */
public class OrderSearchService {
    private final ElasticsearchOperations operations;

    public OrderSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    public OrderSearchDocument save(OrderSearchDocument document) {
        return operations.save(document);
    }

    public Optional<OrderSearchDocument> find(String orderNo) {
        return Optional.ofNullable(
                operations.get(orderNo, OrderSearchDocument.class)
        );
    }
}
