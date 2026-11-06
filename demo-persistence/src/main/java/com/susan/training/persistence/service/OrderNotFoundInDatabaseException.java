package com.susan.training.persistence.service;

/**
 * 数据库订单不存在。
 */
public class OrderNotFoundInDatabaseException extends RuntimeException {
    public OrderNotFoundInDatabaseException(String orderNo) {
        super("数据库订单不存在: " + orderNo);
    }
}
