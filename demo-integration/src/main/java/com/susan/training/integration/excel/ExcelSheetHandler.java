package com.susan.training.integration.excel;

import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * SAX 事件处理器按行读取 Excel，并按批次交给业务处理。
 */
public class ExcelSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
    private final int batchSize;
    private final Consumer<List<ExcelOrderRow>> batchConsumer;
    private final List<ExcelOrderRow> currentBatch = new ArrayList<>();
    private String orderNo;
    private String customerId;
    private String amount;
    private String status;
    private long rowCount;

    public ExcelSheetHandler(
            int batchSize,
            Consumer<List<ExcelOrderRow>> batchConsumer
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
        this.batchSize = batchSize;
        this.batchConsumer = Objects.requireNonNull(
                batchConsumer,
                "batchConsumer 不能为空"
        );
    }

    @Override
    public void startRow(int rowNum) {
        // 每次开始新行时清空上一行状态，避免单元格缺失导致串行。
        orderNo = null;
        customerId = null;
        amount = null;
        status = null;
    }

    @Override
    public void endRow(int rowNum) {
        // 第 0 行是表头，从第 1 行开始计为数据行。
        if (rowNum > 0) {
            rowCount++;
            currentBatch.add(new ExcelOrderRow(
                    orderNo,
                    customerId,
                    parseAmount(amount),
                    status
            ));
            if (currentBatch.size() >= batchSize) {
                flush();
            }
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        String column = cellReference.replaceAll("\\d", "");
        switch (column) {
            case "A" -> orderNo = formattedValue;
            case "B" -> customerId = formattedValue;
            case "C" -> amount = formattedValue;
            case "D" -> status = formattedValue;
            default -> {
                // 教学模板只读取前四列，额外列留给练习题扩展。
            }
        }
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
    }

    public long getRowCount() {
        return rowCount;
    }

    /**
     * 最后一个不足 batchSize 的批次也要提交。
     */
    public void finish() {
        flush();
    }

    private void flush() {
        if (currentBatch.isEmpty()) {
            return;
        }
        // 传入不可变快照，避免业务处理器在异步消费时看到后续修改。
        batchConsumer.accept(List.copyOf(currentBatch));
        currentBatch.clear();
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.replace(",", ""));
    }
}
