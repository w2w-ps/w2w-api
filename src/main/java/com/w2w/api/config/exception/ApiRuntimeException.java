package com.w2w.api.config.exception;

public abstract class ApiRuntimeException extends RuntimeException {

    protected ApiRuntimeException(String message) {
        super(message);
    }
}
