package com.roadcrack.common.model;

public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    INTERNAL_ERROR(500, "internal server error"),

    USER_NOT_FOUND(1001, "user not found"),
    USER_PASSWORD_ERROR(1002, "invalid username or password"),
    USER_DISABLED(1003, "user is disabled"),
    USER_LOCKED(1004, "user is locked"),
    USER_EXISTS(1005, "username already exists"),
    EMAIL_EXISTS(1006, "email already exists"),
    PHONE_EXISTS(1007, "phone already exists"),
    OLD_PASSWORD_ERROR(1008, "old password is incorrect"),
    VERIFICATION_CODE_ERROR(1009, "verification code is invalid or expired"),
    VERIFICATION_CODE_SEND_FAIL(1010, "failed to send verification code"),
    LOGIN_FAIL_LIMIT(1011, "too many failed login attempts"),
    TOKEN_EXPIRED(1012, "token expired"),
    TOKEN_INVALID(1013, "token invalid"),

    ROLE_NOT_FOUND(2001, "role not found"),
    ROLE_CODE_EXISTS(2002, "role code already exists"),
    ROLE_HAS_USERS(2003, "role is still assigned to users"),

    DEPT_NOT_FOUND(3001, "department not found"),
    DEPT_CODE_EXISTS(3002, "department code already exists"),
    DEPT_HAS_CHILDREN(3003, "department still has child departments"),
    DEPT_HAS_USERS(3004, "department still has users"),

    ROAD_NOT_FOUND(5001, "road not found"),
    ROAD_CODE_EXISTS(5002, "road code already exists"),
    ROAD_HAS_DETECTIONS(5003, "road still has detection tasks"),

    FILE_UPLOAD_FAIL(4001, "file upload failed"),
    FILE_TYPE_ERROR(4002, "unsupported file type"),
    FILE_SIZE_EXCEED(4003, "file size exceeds limit"),
    FILE_NOT_FOUND(4004, "file not found");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
