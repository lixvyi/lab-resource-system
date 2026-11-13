package com.lab.gateway.exception;

/**
 * 下游服务返回了业务错误(4xx/5xx JSON)，网关按原状态码透传。
 */
public class DownstreamHttpException extends RuntimeException {

    private final int httpStatus;
    private final Object details;

    public DownstreamHttpException(int httpStatus, String message, Object details) {
        super(message);
        this.httpStatus = httpStatus;
        this.details = details;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Object getDetails() {
        return details;
    }
}
