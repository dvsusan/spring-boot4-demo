package com.susan.training.web;

import com.susan.training.common.ApiResponse;
import com.susan.training.order.OrderNotFoundException;
import com.susan.training.persistence.service.InsufficientStockException;
import com.susan.training.persistence.service.OrderNotFoundInDatabaseException;
import com.susan.training.persistence.tenant.UnknownTenantException;
import com.susan.training.web.error.ApiErrorCode;
import com.susan.training.web.error.FieldViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.List;

/**
 * REST 接口的统一异常出口。
 *
 * <p>业务异常、参数异常和兜底异常都在这里转换成稳定的 HTTP 状态码与错误码，
 * Controller 不需要重复编写 try/catch。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 订单不存在属于资源未找到，返回 HTTP 404。
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(OrderNotFoundException exception) {
        return response(ApiErrorCode.ORDER_NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundInDatabaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseNotFound(
            OrderNotFoundInDatabaseException exception
    ) {
        return response(ApiErrorCode.ORDER_NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(
            InsufficientStockException exception
    ) {
        return response(ApiErrorCode.RESOURCE_CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UnknownTenantException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownTenant(
            UnknownTenantException exception
    ) {
        return response(ApiErrorCode.INVALID_ARGUMENT, exception.getMessage());
    }

    /**
     * 请求体字段校验失败时返回全部字段错误，而不是只返回第一条。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleBodyValidation(
            MethodArgumentNotValidException exception
    ) {
        List<FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .toList();
        return errorResponse(
                ApiErrorCode.INVALID_ARGUMENT,
                ApiErrorCode.INVALID_ARGUMENT.defaultMessage(),
                violations
        );
    }

    /**
     * 路径变量、查询参数等方法参数校验失败时统一整理成字段错误。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleParameterValidation(
            HandlerMethodValidationException exception
    ) {
        List<FieldViolation> violations = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage()
                        )))
                .toList();
        return errorResponse(
                ApiErrorCode.INVALID_ARGUMENT,
                ApiErrorCode.INVALID_ARGUMENT.defaultMessage(),
                violations
        );
    }

    /**
     * 兼容 AOP 方法校验直接抛出的 ConstraintViolationException。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<FieldViolation> violations = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return errorResponse(
                ApiErrorCode.INVALID_ARGUMENT,
                ApiErrorCode.INVALID_ARGUMENT.defaultMessage(),
                violations
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException exception) {
        // 不把底层解析器异常原样返回，避免泄露包名、字段类型和实现细节。
        return response(ApiErrorCode.INVALID_JSON, ApiErrorCode.INVALID_JSON.defaultMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidArgument(Exception exception) {
        return response(
                ApiErrorCode.INVALID_ARGUMENT,
                ApiErrorCode.INVALID_ARGUMENT.defaultMessage()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception
    ) {
        return response(
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception
    ) {
        return response(ApiErrorCode.NOT_ACCEPTABLE, ApiErrorCode.NOT_ACCEPTABLE.defaultMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception
    ) {
        return response(ApiErrorCode.METHOD_NOT_ALLOWED, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        // 服务端保留完整堆栈，响应只返回稳定错误码，避免暴露内部实现。
        log.error("unhandled request failure", exception);
        return response(ApiErrorCode.INTERNAL_ERROR, ApiErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    /**
     * SSE 客户端断开后不能再写 JSON 响应，记录调试信息并结束异步分发。
     */
    @ExceptionHandler({AsyncRequestNotUsableException.class, AsyncRequestTimeoutException.class})
    public void handleDisconnectedStream(Exception exception) {
        log.debug("stream client disconnected before the response completed");
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> response(ApiErrorCode code, String message) {
        return ResponseEntity.status(code.status())
                .body(ApiResponse.failure(code.name(), message));
    }

    private <T> ResponseEntity<ApiResponse<T>> errorResponse(
            ApiErrorCode code,
            String message,
            T details
    ) {
        // 当前 ApiResponse 的 data 字段既承载成功数据，也承载结构化错误明细。
        return ResponseEntity.status(code.status())
                .body(new ApiResponse<>(code.name(), message, details, java.time.Instant.now()));
    }
}
