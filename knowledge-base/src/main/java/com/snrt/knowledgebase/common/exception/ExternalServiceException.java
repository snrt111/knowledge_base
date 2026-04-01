package com.snrt.knowledgebase.common.exception;

/**
 * 外部服务异常
 * 
 * 用于表示调用外部服务失败的异常
 * 
 * @author SNRT
 * @since 1.0
 */
public class ExternalServiceException extends BusinessException {

    /**
     * 构造外部服务异常
     * 
     * @param service 服务名称
     * @param message 错误消息
     */
    public ExternalServiceException(String service, String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, service, message);
    }

    /**
     * 构造外部服务异常（带异常原因）
     * 
     * @param service 服务名称
     * @param message 错误消息
     * @param cause 异常原因
     */
    public ExternalServiceException(String service, String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, cause, service, message);
    }
}
