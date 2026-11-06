package com.susan.training.integration.file;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式开启 MinIO 章节时才创建对象存储客户端。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter5.minio",
        name = "enabled",
        havingValue = "true"
)
public class MinioIntegrationConfiguration {

    @Bean
    public MinioClient minioClient(
            @Value("${demo.chapter5.minio.endpoint:http://127.0.0.1:9000}")
            String endpoint,
            @Value("${demo.chapter5.minio.access-key:minioadmin}")
            String accessKey,
            @Value("${demo.chapter5.minio.secret-key:minioadmin}")
            String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public MinioFileStorageService fileStorageService(
            MinioClient client,
            @Value("${demo.chapter5.minio.bucket:training-files}")
            String bucket,
            @Value("${demo.chapter5.minio.max-file-bytes:20971520}")
            long maxFileBytes
    ) {
        return new MinioFileStorageService(client, bucket, maxFileBytes);
    }
}
