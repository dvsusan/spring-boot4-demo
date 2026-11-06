package com.susan.training.integration.file;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件上传与临时预览地址服务。
 */
public class MinioFileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final MinioClient client;
    private final String bucket;
    private final long maxFileBytes;
    private volatile boolean bucketReady;

    public MinioFileStorageService(
            MinioClient client,
            String bucket,
            long maxFileBytes
    ) {
        this.client = client;
        this.bucket = bucket;
        this.maxFileBytes = maxFileBytes;
    }

    public FileMetadata upload(
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream
    ) throws Exception {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
        if (size <= 0 || size > maxFileBytes) {
            throw new IllegalArgumentException("文件大小超出限制");
        }

        ensureBucket();
        String objectName = "uploads/"
                + UUID.randomUUID()
                + extension(originalFilename);

        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build());

        return new FileMetadata(bucket, objectName, presignedUrl(objectName));
    }

    public String presignedUrl(String objectName) throws Exception {
        if (objectName.contains("..") || objectName.startsWith("/")) {
            throw new IllegalArgumentException("非法对象路径");
        }
        // 预签名 URL 只用于短期预览，不能让客户端直接持有 MinIO 密钥。
        return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry(15, TimeUnit.MINUTES)
                .build());
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        String extension = filename.substring(filename.lastIndexOf('.'))
                .toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
