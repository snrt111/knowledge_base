package com.snrt.knowledgebase.common.exception.impl;

import com.snrt.knowledgebase.common.exception.BusinessException;
import com.snrt.knowledgebase.common.exception.ExceptionContext;
import com.snrt.knowledgebase.common.exception.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认异常日志记录器
 * 
 * 实现ExceptionLogger接口，提供默认的异常日志记录逻辑：
 * - 业务异常：记录警告级别日志
 * - 系统异常：记录错误级别日志（包含堆栈信息）
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class DefaultExceptionLogger implements ExceptionLogger {

    /**
     * 记录异常（根据异常类型自动分发）
     * 
     * @param context 异常上下文
     * @param throwable 异常对象
     */
    @Override
    public void logException(ExceptionContext context, Throwable throwable) {
        if (throwable instanceof BusinessException) {
            logBusinessException(context, (BusinessException) throwable);
        } else {
            logSystemException(context, throwable);
        }
    }

    /**
     * 记录业务异常
     * 
     * @param context 异常上下文
     * @param exception 业务异常
     */
    @Override
    public void logBusinessException(ExceptionContext context, BusinessException exception) {
        log.warn("[业务异常] traceId={}, errorCode={}, message={}, path={}, method={}",
                context.getTraceId(),
                exception.getErrorCode().getCode(),
                exception.getMessage(),
                context.getPath(),
                context.getMethod());
    }

    /**
     * 记录系统异常
     * 
     * @param context 异常上下文
     * @param exception 系统异常
     */
    @Override
    public void logSystemException(ExceptionContext context, Throwable exception) {
        log.error("[系统异常] traceId={}, exceptionType={}, message={}, path={}, method={}, duration={}ms",
                context.getTraceId(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                context.getPath(),
                context.getMethod(),
                context.getDuration(),
                exception);
    }
}
