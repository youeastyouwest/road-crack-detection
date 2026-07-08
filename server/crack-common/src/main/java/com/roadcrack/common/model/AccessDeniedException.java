package com.roadcrack.common.model;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super(ResultCode.FORBIDDEN.message());
    }

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
