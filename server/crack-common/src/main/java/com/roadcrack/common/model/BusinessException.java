package com.roadcrack.common.model;

public class BusinessException extends RuntimeException {

    private final int code;
    private final ResultCode resultCode;

    public BusinessException(String message) {
        this(ResultCode.INTERNAL_ERROR, message);
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.code();
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.message());
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.resultCode = null;
    }

    public int getCode() {
        return code;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
