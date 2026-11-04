package com.lab.gateway.exception;

/**
 * 下游服务调用异常。
 * <p>
 * 当 Node.js / Python 服务不可达、返回非 2xx 状态或响应格式异常时抛出,
 * 由 {@code GlobalExceptionHandler} 统一转换为 HTTP 502 Bad Gateway。
 */
public class DownstreamServiceException extends RuntimeException {

    private final String serviceName;

    public DownstreamServiceException(String message) {
        super(message);
        this.serviceName = "unknown";
    }

    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = cause == null ? "unknown" : cause.getClass().getSimpleName();
    }

    public DownstreamServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
