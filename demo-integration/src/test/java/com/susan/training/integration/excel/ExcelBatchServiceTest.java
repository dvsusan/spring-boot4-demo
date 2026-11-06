package com.susan.training.integration.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelBatchServiceTest {

    @Test
    void exportedWorkbookCanBeImportedAsynchronously() throws Exception {
        ExcelBatchService service = new ExcelBatchService();
        ByteArrayOutputStream workbook = new ByteArrayOutputStream();

        // 2000 行会跨过一个 1000 行批次，验证分批写入路径。
        service.exportOrders(workbook, 2_000);
        assertThat(workbook.size()).isGreaterThan(1_000);

        String jobId = service.importAsync(
                new ByteArrayInputStream(workbook.toByteArray())
        );

        ExcelJobInfo result = waitForJob(service, jobId);
        assertThat(result.status()).isEqualTo(ExcelJobStatus.SUCCEEDED);
        assertThat(result.processedRows()).isEqualTo(2_000);
        service.close();
    }

    private ExcelJobInfo waitForJob(
            ExcelBatchService service,
            String jobId
    ) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            ExcelJobInfo info = service.status(jobId);
            if (info.status() == ExcelJobStatus.SUCCEEDED
                    || info.status() == ExcelJobStatus.FAILED) {
                return info;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Excel 异步任务在 5 秒内没有完成");
    }
}
