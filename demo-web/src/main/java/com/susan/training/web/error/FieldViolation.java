package com.susan.training.web.error;

/**
 * 单个字段的校验失败说明，客户端可以直接映射到表单项。
 */
public record FieldViolation(String field, String message) {
}
