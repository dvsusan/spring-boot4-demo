package com.susan.training.integration.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * 只在搜索章节显式开启时注册 Elasticsearch 服务。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter4.elasticsearch",
        name = "enabled",
        havingValue = "true"
)
public class ElasticsearchIntegrationConfiguration {
    @Bean
    public OrderSearchService orderSearchService(ElasticsearchOperations operations) {
        return new OrderSearchService(operations);
    }
}
