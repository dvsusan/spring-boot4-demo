package com.susan.training.web.error;

import org.springframework.http.HttpStatus;

/**
 * API 错误码的唯一来源，避免同一异常在不同 Controller 中返回不同状态。
 */
public enum ApiErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "订单不存在"),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "请求参数不合法"),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "资源状态冲突"),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "请求体不是合法 JSON"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请求媒体类型不受支持"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "无法生成调用方接受的响应格式"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不受支持"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
