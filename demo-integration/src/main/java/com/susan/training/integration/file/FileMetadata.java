package com.susan.training.integration.file;

/**
 * 文件上传后返回给业务方的对象存储元数据。
 */
public record FileMetadata(
        String bucket,
        String objectName,
        String presignedUrl
) {
}
