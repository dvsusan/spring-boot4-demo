package com.susan.training.integration.file;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MinioFileStorageServiceTest {

    @Test
    void rejectsUnsupportedContentTypeBeforeCallingMinio() {
        MinioFileStorageService service = new MinioFileStorageService(
                mock(MinioClient.class),
                "training-files",
                1024
        );

        assertThatThrownBy(() -> service.upload(
                "danger.exe",
                "application/x-msdownload",
                10,
                new ByteArrayInputStream(new byte[]{1, 2, 3})
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedFileBeforeCallingMinio() {
        MinioFileStorageService service = new MinioFileStorageService(
                mock(MinioClient.class),
                "training-files",
                100
        );

        assertThatThrownBy(() -> service.upload(
                "manual.pdf",
                "application/pdf",
                101,
                new ByteArrayInputStream(new byte[]{1})
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
