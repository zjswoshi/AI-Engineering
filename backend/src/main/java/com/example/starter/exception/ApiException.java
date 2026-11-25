package com.example.starter.exception;

public class ApiException extends RuntimeException {
    private final ApiError error;

    public ApiException(ApiError error) {
        super(error.name());
        this.error = error;
    }

    public ApiError error() {
        return error;
    }
}
