package com.snrt.knowledgebase.exception.impl;

import com.snrt.knowledgebase.exception.BusinessException;
import com.snrt.knowledgebase.exception.ExceptionContext;
import com.snrt.knowledgebase.exception.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultExceptionLogger implements ExceptionLogger {

    @Override
    public void logException(ExceptionContext context, Throwable throwable) {
        if (throwable instanceof BusinessException) {
            logBusinessException(context, (BusinessException) throwable);
        } else {
            logSystemException(context, throwable);
        }
    }

    @Override
    public void logBusinessException(ExceptionContext context, BusinessException exception) {
        log.warn("[业务异常] traceId={}, errorCode={}, message={}, path={}, method={}",
                context.getTraceId(),
                exception.getErrorCode().getCode(),
                exception.getMessage(),
                context.getPath(),
                context.getMethod());
    }

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
