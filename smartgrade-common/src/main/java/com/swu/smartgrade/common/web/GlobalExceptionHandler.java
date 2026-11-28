package com.swu.smartgrade.common.web;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.api.TraceId;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * B1 统一异常出口：把 BusinessException、参数校验异常和未知异常都转换成 ApiResponse。
 * 业务服务不需要在每个 Controller 里重复 try/catch 和拼接错误 JSON。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        // 按业务异常携带的 HTTP 状态输出统一失败结构。
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.fail(ex.getCode(), ex.getMessage(), TraceId.current()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // A4 证据：DTO 上的 @NotBlank/@Pattern 校验失败时统一返回 40000 和具体字段名。
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(ApiResponse.fail(40000, message, TraceId.current()));
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<Void>> handleConstraint(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(40000, ex.getMessage(), TraceId.current()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        // 未预期异常只向前端暴露通用信息，完整堆栈写入服务日志并通过 traceId 排查。
        log.error("未处理异常 traceId={}", TraceId.current(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(50000, "服务端内部错误", TraceId.current()));
    }
}
