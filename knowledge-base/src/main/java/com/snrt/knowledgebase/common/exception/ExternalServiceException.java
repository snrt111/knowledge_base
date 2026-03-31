package com.snrt.knowledgebase.common.exception;

public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String service, String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, service, message);
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, cause, service, message);
    }
}
