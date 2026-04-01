package com.snrt.knowledgebase.common.exception;

/**
 * 资源未找到异常
 * 
 * 用于表示请求的资源不存在
 * 继承自BusinessException，使用RESOURCE_NOT_FOUND错误码
 * 
 * @author SNRT
 * @since 1.0
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 构造资源未找到异常
     * 
     * @param resourceType 资源类型
     * @param id 资源ID
     */
    public ResourceNotFoundException(String resourceType, String id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceType, id);
    }

    /**
     * 构造资源未找到异常（带异常原因）
     * 
     * @param resourceType 资源类型
     * @param id 资源ID
     * @param cause 异常原因
     */
    public ResourceNotFoundException(String resourceType, String id, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, cause, resourceType, id);
    }
}
