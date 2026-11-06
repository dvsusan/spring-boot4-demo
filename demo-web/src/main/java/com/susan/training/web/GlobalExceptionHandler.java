package com.susan.training.web;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 接口的统一异常出口。
 *
 * <p>把业务异常转换成稳定的 HTTP 状态码和业务错误码。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 订单不存在属于资源未找到，返回 HTTP 404。
     */
    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(OrderNotFoundException exception) {
        return ApiResponse.failure("ORDER_NOT_FOUND", exception.getMessage());
    }

    /**
     * Bean Validation 校验失败时返回 HTTP 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        // 本示例只返回第一个错误，生产项目可以扩展为完整错误列表。
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ApiResponse.failure("INVALID_ARGUMENT", message);
    }
}
