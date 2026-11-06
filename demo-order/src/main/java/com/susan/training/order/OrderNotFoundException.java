package com.susan.training.order;

/**
 * 查询订单时找不到目标订单。
 *
 * <p>这是业务异常，不应该让业务模块直接依赖 HTTP 404。</p>
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderNo) {
        super("订单不存在: " + orderNo);
    }
}
