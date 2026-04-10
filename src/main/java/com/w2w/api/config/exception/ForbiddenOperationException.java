package com.w2w.api.config.exception;

public class ForbiddenOperationException extends ApiRuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
