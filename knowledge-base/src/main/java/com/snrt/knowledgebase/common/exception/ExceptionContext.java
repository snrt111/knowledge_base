package com.snrt.knowledgebase.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExceptionContext {

    private String traceId;
    private LocalDateTime timestamp;
    private String exceptionType;
    private String message;
    private String path;
    private String method;
    private Object params;
    private Long duration;

    public static ExceptionContext create() {
        return ExceptionContext.builder()
                .traceId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
