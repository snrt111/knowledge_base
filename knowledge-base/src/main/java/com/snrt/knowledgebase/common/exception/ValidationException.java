package com.snrt.knowledgebase.common.exception;

/**
 * 参数校验异常
 * 
 * 用于表示参数校验失败的异常
 * 
 * @author SNRT
 * @since 1.0
 */
public class ValidationException extends BusinessException {

    /**
     * 构造参数校验异常
     * 
     * @param field 字段名
     * @param message 错误消息
     */
    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, field + " - " + message);
    }

    /**
     * 构造参数校验异常
     * 
     * @param message 错误消息
     */
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
