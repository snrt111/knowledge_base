package com.snrt.knowledgebase.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 异常上下文
 * 
 * 用于封装异常发生时的上下文信息：
 * - 追踪ID
 * - 时间戳
 * - 异常类型和消息
 * - 请求路径和方法
 * - 请求参数
 * - 执行耗时
 * 
 * @author SNRT
 * @since 1.0
 */
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

    /**
     * 创建新的异常上下文
     * 
     * @return ExceptionContext实例
     */
    public static ExceptionContext create() {
        return ExceptionContext.builder()
                .traceId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
