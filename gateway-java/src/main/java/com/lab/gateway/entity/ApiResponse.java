package com.lab.gateway.entity;

/**
 * 统一 API 响应包装。
 * <p>
 * 三个服务(Node/Python/Java)采用完全一致的响应信封格式:
 * <pre>
 * { "code": 0, "message": "success", "data": { ... } }
 * </pre>
 * 这样客户端只需实现一套解析逻辑,也便于网关聚合下游响应。
 */
public class ApiResponse<T> {

    /** 0 表示成功,非 0 为错误码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 错误详情(成功时为 null) */
    private Object details;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this(code, message, data, null);
    }

    public ApiResponse(int code, String message, T data, Object details) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.details = details;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, Object details) {
        return new ApiResponse<>(code, message, null, details);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
