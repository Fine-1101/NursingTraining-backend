package org.example.nursingtrainingbackend.common.result;

import java.time.Instant;

public record Result<T>(int code, String message, T data, Instant timestamp) {
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, Instant.now());
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static Result<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    public static Result<Void> failure(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null, Instant.now());
    }
}
