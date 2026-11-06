package com.susan.training.integration.excel;

import java.math.BigDecimal;

/**
 * Excel 订单导入导出行。
 */
public class ExcelOrderRow {
    private String orderNo;

    private String customerId;

    private BigDecimal amount;

    private String status;

    public ExcelOrderRow() {
    }

    public ExcelOrderRow(
            String orderNo,
            String customerId,
            BigDecimal amount,
            String status
    ) {
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
