package com.snrt.knowledgebase.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, String id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceType, id);
    }

    public ResourceNotFoundException(String resourceType, String id, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, cause, resourceType, id);
    }
}
