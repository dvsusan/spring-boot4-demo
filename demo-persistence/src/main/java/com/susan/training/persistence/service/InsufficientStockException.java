package com.susan.training.persistence.service;

/**
 * 库存不足时终止订单事务。
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId) {
        super("库存不足: " + productId);
    }
}
