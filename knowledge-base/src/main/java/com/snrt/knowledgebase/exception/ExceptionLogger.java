package com.snrt.knowledgebase.exception;

public interface ExceptionLogger {

    void logException(ExceptionContext context, Throwable throwable);

    void logBusinessException(ExceptionContext context, BusinessException exception);

    void logSystemException(ExceptionContext context, Throwable exception);
}
