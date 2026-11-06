package com.susan.training.integration.excel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式开启文件批处理章节时才创建 Excel 服务。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter5.excel",
        name = "enabled",
        havingValue = "true"
)
public class ExcelIntegrationConfiguration {
    @Bean(destroyMethod = "close")
    public ExcelBatchService excelBatchService() {
        return new ExcelBatchService();
    }
}
