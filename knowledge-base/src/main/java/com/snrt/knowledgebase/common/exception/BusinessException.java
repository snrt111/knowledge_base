package com.snrt.knowledgebase.common.exception;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * 业务异常
 * 
 * 用于表示业务逻辑错误
 * 包含错误码和错误参数，支持消息格式化
 * 
 * @author SNRT
 * @since 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    /**
     * 构造业务异常
     * 
     * @param errorCode 错误码
     * @param args 错误参数
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(MessageFormat.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 构造业务异常（带异常原因）
     * 
     * @param errorCode 错误码
     * @param cause 异常原因
     * @param args 错误参数
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(MessageFormat.format(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
