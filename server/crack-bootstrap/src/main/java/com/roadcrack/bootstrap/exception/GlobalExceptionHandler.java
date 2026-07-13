package com.roadcrack.bootstrap.exception;

import com.roadcrack.common.model.AccessDeniedException;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<ResultCode> UNAUTHORIZED_CODES = Set.of(
            ResultCode.UNAUTHORIZED, ResultCode.TOKEN_EXPIRED, ResultCode.TOKEN_INVALID,
            ResultCode.USER_PASSWORD_ERROR, ResultCode.LOGIN_FAIL_LIMIT
    );
    private static final Set<ResultCode> FORBIDDEN_CODES = Set.of(
            ResultCode.FORBIDDEN, ResultCode.USER_DISABLED, ResultCode.USER_LOCKED
    );
    private static final Set<ResultCode> NOT_FOUND_CODES = Set.of(
            ResultCode.NOT_FOUND, ResultCode.USER_NOT_FOUND, ResultCode.ROLE_NOT_FOUND,
            ResultCode.DEPT_NOT_FOUND, ResultCode.FILE_NOT_FOUND
    );
    private static final Set<ResultCode> CONFLICT_CODES = Set.of(
            ResultCode.CONFLICT, ResultCode.USER_EXISTS, ResultCode.EMAIL_EXISTS,
            ResultCode.PHONE_EXISTS, ResultCode.ROLE_CODE_EXISTS, ResultCode.DEPT_CODE_EXISTS
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ResultCode resultCode = exception.getResultCode();
        ApiResponse<Void> body = resultCode != null
                ? ApiResponse.failure(resultCode, exception.getMessage())
                : ApiResponse.failure(exception.getCode(), exception.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST; // default 400
        if (resultCode != null) {
            if (UNAUTHORIZED_CODES.contains(resultCode)) status = HttpStatus.UNAUTHORIZED;
            else if (FORBIDDEN_CODES.contains(resultCode)) status = HttpStatus.FORBIDDEN;
            else if (NOT_FOUND_CODES.contains(resultCode)) status = HttpStatus.NOT_FOUND;
            else if (CONFLICT_CODES.contains(resultCode)) status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.message());
        return ApiResponse.failure(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.message());
        return ApiResponse.failure(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException exception) {
        String message = exception.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.message());
        return ApiResponse.failure(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParameterException(MissingServletRequestParameterException exception) {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "missing required parameter: " + exception.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "parameter type mismatch: " + exception.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException() {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "request body format is invalid");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException exception) {
        return ApiResponse.failure(ResultCode.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return ApiResponse.failure(HttpStatus.METHOD_NOT_ALLOWED.value(), "request method not supported: " + exception.getMethod());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceededException() {
        return ApiResponse.failure(ResultCode.FILE_SIZE_EXCEED);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ApiResponse<Void> handleDuplicateKeyException() {
        return ApiResponse.failure(ResultCode.CONFLICT, "duplicate data submission");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Void> handleDataIntegrityViolationException() {
        return ApiResponse.failure(ResultCode.CONFLICT, "data integrity violation");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpectedException(Exception exception) {
        return ApiResponse.failure(ResultCode.INTERNAL_ERROR, exception.getMessage());
    }
}
