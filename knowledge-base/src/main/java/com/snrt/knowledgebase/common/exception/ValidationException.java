package com.snrt.knowledgebase.common.exception;

public class ValidationException extends BusinessException {

    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, field + " - " + message);
    }

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
