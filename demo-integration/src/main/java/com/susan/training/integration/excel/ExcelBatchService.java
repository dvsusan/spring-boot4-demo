package com.susan.training.integration.excel;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Excel 分批导出和异步导入服务。
 */
public class ExcelBatchService implements AutoCloseable {
    private static final int BATCH_SIZE = 1_000;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, ExcelJobInfo> jobs = new ConcurrentHashMap<>();

    /**
     * 分批次写入百万行，避免构造完整 List。
     */
    public void exportOrders(OutputStream outputStream, int totalRows) {
        if (totalRows < 0) {
            throw new IllegalArgumentException("totalRows 不能为负数");
        }
        // SXSSF 只保留 100 行在内存，其余行分批写入临时文件。
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("orders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("订单号");
            header.createCell(1).setCellValue("客户编号");
            header.createCell(2).setCellValue("金额");
            header.createCell(3).setCellValue("状态");

            for (int index = 0; index < totalRows; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue("SO" + index);
                row.createCell(1).setCellValue("C" + (index % 10_000));
                row.createCell(2).setCellValue(index + 1D);
                row.createCell(3).setCellValue("CREATED");
            }
            workbook.write(outputStream);
            workbook.dispose();
        } catch (IOException exception) {
            throw new IllegalStateException("Excel 导出失败", exception);
        }
    }

    /**
     * 先落到临时文件，再把文件处理交给虚拟线程。
     */
    public String importAsync(InputStream inputStream) throws IOException {
        Path tempFile = Files.createTempFile("orders-", ".xlsx");
        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new ExcelJobInfo(
                jobId,
                ExcelJobStatus.PENDING,
                0,
                "等待处理"
        ));

        executor.submit(() -> {
            jobs.put(jobId, new ExcelJobInfo(
                    jobId,
                    ExcelJobStatus.RUNNING,
                    0,
                    "正在读取"
            ));
            try {
                ExcelSheetHandler handler = readFirstSheet(tempFile);
                jobs.put(jobId, new ExcelJobInfo(
                        jobId,
                        ExcelJobStatus.SUCCEEDED,
                        handler.getRowCount(),
                        "处理完成"
                ));
            } catch (Exception exception) {
                jobs.put(jobId, new ExcelJobInfo(
                        jobId,
                        ExcelJobStatus.FAILED,
                        readerCount(jobId),
                        exception.getMessage()
                ));
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 临时文件清理失败不应覆盖原始任务状态。
                }
            }
        });
        return jobId;
    }

    public ExcelJobInfo status(String jobId) {
        ExcelJobInfo info = jobs.get(jobId);
        if (info == null) {
            throw new IllegalArgumentException("Excel 任务不存在: " + jobId);
        }
        return info;
    }

    private ExcelSheetHandler readFirstSheet(Path path) throws Exception {
        try (OPCPackage packageFile = OPCPackage.open(path.toFile())) {
            XSSFReader reader = new XSSFReader(packageFile);
            ExcelSheetHandler handler = new ExcelSheetHandler(
                    BATCH_SIZE,
                    this::processBatch
            );
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(
                    reader.getStylesTable(),
                    reader.getSharedStringsTable(),
                    handler,
                    new DataFormatter(),
                    false
            ));
            XSSFReader.SheetIterator sheets = reader.getSheetIterator();
            if (!sheets.hasNext()) {
                throw new IllegalArgumentException("Excel 中没有工作表");
            }
            try (InputStream sheet = sheets.next()) {
                parser.parse(new InputSource(sheet));
            }
            handler.finish();
            return handler;
        }
    }

    private void processBatch(List<ExcelOrderRow> batch) {
        // 教学版在批次边界停止；生产项目在这里执行数据库批量写入或投递 MQ。
        // 业务消费者必须自行保证幂等、事务和失败重试。
    }

    private long readerCount(String jobId) {
        ExcelJobInfo info = jobs.get(jobId);
        return info == null ? 0 : info.processedRows();
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
