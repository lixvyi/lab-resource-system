package com.lab.gateway.exception;

import com.lab.gateway.entity.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 * <p>
 * 把各类异常统一转换为与其它两个服务一致的 {@code ApiResponse} 信封格式,
 * 保证客户端在跨语言场景下只需处理一种响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 下游服务(跨语言调用)失败 —— 返回 502 Bad Gateway
     */
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDownstream(DownstreamServiceException ex) {
        log.error("[网关] 跨语言调用失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(502, "下游服务调用失败", ex.getMessage()));
    }

    /**
     * 参数校验失败 —— 返回 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[网关] 参数校验失败: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数校验失败", fieldErrors));
    }

    /**
     * 唯一约束冲突(如手机号重复)—— 返回 409
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("[网关] 唯一约束冲突: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, "数据重复:手机号或唯一字段已存在", ex.getMessage()));
    }

    /**
     * 非法参数(业务级)—— 返回 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[网关] 非法参数: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数错误", ex.getMessage()));
    }

    /**
     * 兜底异常 —— 返回 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("[网关] 未捕获异常:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误", ex.getMessage()));
    }
}
