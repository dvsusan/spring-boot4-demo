package com.susan.training.common;

import java.time.Instant;

/**
 * 所有 HTTP 接口共用的响应结构。
 *
 * @param code      业务状态码，0 表示成功
 * @param message   给调用方阅读的结果说明
 * @param data      成功时返回的数据，失败时为 null
 * @param timestamp 响应生成时间，便于日志排查和链路定位
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Instant timestamp
) {
    /**
     * 创建成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "success", data, Instant.now());
    }

    /**
     * 创建失败响应，并保留稳定的业务错误码。
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}
