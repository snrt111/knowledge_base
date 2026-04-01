package com.snrt.knowledgebase.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 日志工具类
 * 
 * 提供统一的日志记录功能：
 * - 生成和管理追踪ID（traceId）
 * - 记录操作日志（成功/失败）
 * - 记录业务事件
 * - 记录API请求和响应
 * - 性能监控
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
public class LogUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TRACE_ID = "traceId";
    private static final String OPERATION = "operation";
    private static final String DURATION = "duration";
    private static final String STATUS = "status";
    private static final String ERROR_MSG = "errorMsg";

    /**
     * 生成追踪ID
     * 
     * @return 追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置追踪ID到MDC
     * 
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 清除MDC中的追踪ID
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    /**
     * 获取追踪ID，如果不存在则生成新的
     * 
     * @return 追踪ID
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * 记录操作日志
     * 
     * @param operation 操作名称
     * @param status 操作状态（SUCCESS/FAILED）
     * @param durationMs 执行耗时（毫秒）
     * @param errorMsg 错误消息（失败时）
     */
    public static void logOperation(String operation, String status, long durationMs, String errorMsg) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put(TRACE_ID, getTraceId());
        logMap.put(OPERATION, operation);
        logMap.put(STATUS, status);
        logMap.put(DURATION, durationMs);
        if (errorMsg != null) {
            logMap.put(ERROR_MSG, errorMsg);
        }

        try {
            String json = objectMapper.writeValueAsString(logMap);
            if ("SUCCESS".equals(status)) {
                log.info(json);
            } else {
                log.error(json);
            }
        } catch (JsonProcessingException e) {
            log.warn("日志序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 记录成功操作日志
     * 
     * @param operation 操作名称
     * @param durationMs 执行耗时（毫秒）
     */
    public static void logOperationSuccess(String operation, long durationMs) {
        logOperation(operation, "SUCCESS", durationMs, null);
    }

    /**
     * 记录失败操作日志
     * 
     * @param operation 操作名称
     * @param durationMs 执行耗时（毫秒）
     * @param errorMsg 错误消息
     */
    public static void logOperationFailed(String operation, long durationMs, String errorMsg) {
        logOperation(operation, "FAILED", durationMs, errorMsg);
    }

    /**
     * 记录业务事件
     * 
     * @param eventType 事件类型
     * @param context 事件上下文
     */
    public static void logBusinessEvent(String eventType, Map<String, Object> context) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put(TRACE_ID, getTraceId());
        logMap.put("eventType", eventType);
        logMap.put("timestamp", Instant.now().toString());
        if (context != null) {
            logMap.putAll(context);
        }

        try {
            String json = objectMapper.writeValueAsString(logMap);
            log.info(json);
        } catch (JsonProcessingException e) {
            log.warn("日志序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 记录API请求日志
     * 
     * @param method HTTP方法
     * @param path 请求路径
     * @param params 请求参数
     */
    public static void logApiRequest(String method, String path, Map<String, Object> params) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put(TRACE_ID, getTraceId());
        logMap.put("type", "API_REQUEST");
        logMap.put("httpMethod", method);
        logMap.put("path", path);
        if (params != null) {
            logMap.put("params", params);
        }

        try {
            String json = objectMapper.writeValueAsString(logMap);
            log.debug(json);
        } catch (JsonProcessingException e) {
            log.warn("日志序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 记录API响应日志
     * 
     * @param method HTTP方法
     * @param path 请求路径
     * @param statusCode 状态码
     * @param durationMs 执行耗时（毫秒）
     */
    public static void logApiResponse(String method, String path, int statusCode, long durationMs) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put(TRACE_ID, getTraceId());
        logMap.put("type", "API_RESPONSE");
        logMap.put("httpMethod", method);
        logMap.put("path", path);
        logMap.put("statusCode", statusCode);
        logMap.put(DURATION, durationMs);

        try {
            String json = objectMapper.writeValueAsString(logMap);
            if (statusCode >= 200 && statusCode < 300) {
                log.debug(json);
            } else {
                log.warn(json);
            }
        } catch (JsonProcessingException e) {
            log.warn("日志序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 记录性能日志
     * 
     * @param operation 操作名称
     * @param duration 执行耗时
     * @param thresholdMs 性能阈值（毫秒）
     */
    public static void logPerformance(String operation, Duration duration, long thresholdMs) {
        long durationMs = duration.toMillis();
        if (durationMs > thresholdMs) {
            log.warn("性能警告: operation={}, duration={}ms, threshold={}ms",
                    operation, durationMs, thresholdMs);
        }
    }
}
