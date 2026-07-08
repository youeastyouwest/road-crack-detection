package com.roadcrack.common.model;

public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.code(), message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> failure(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.code(), message, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> failure(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.code(), resultCode.message(), null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
