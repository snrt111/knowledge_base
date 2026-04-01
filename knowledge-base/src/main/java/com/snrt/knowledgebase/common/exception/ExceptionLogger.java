package com.snrt.knowledgebase.common.exception;

/**
 * 异常日志记录器接口
 * 
 * 定义异常日志记录的统一接口
 * 支持业务异常和系统异常的日志记录
 * 
 * @author SNRT
 * @since 1.0
 */
public interface ExceptionLogger {

    /**
     * 记录异常
     * 
     * @param context 异常上下文
     * @param throwable 异常对象
     */
    void logException(ExceptionContext context, Throwable throwable);

    /**
     * 记录业务异常
     * 
     * @param context 异常上下文
     * @param exception 业务异常
     */
    void logBusinessException(ExceptionContext context, BusinessException exception);

    /**
     * 记录系统异常
     * 
     * @param context 异常上下文
     * @param exception 系统异常
     */
    void logSystemException(ExceptionContext context, Throwable exception);
}
