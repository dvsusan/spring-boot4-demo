package com.susan.training.integration.excel;

/**
 * 可查询的异步任务快照。
 */
public record ExcelJobInfo(
        String jobId,
        ExcelJobStatus status,
        long processedRows,
        String message
) {
}
