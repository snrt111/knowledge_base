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

@Slf4j
public class LogUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TRACE_ID = "traceId";
    private static final String OPERATION = "operation";
    private static final String DURATION = "duration";
    private static final String STATUS = "status";
    private static final String ERROR_MSG = "errorMsg";

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

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

    public static void logOperationSuccess(String operation, long durationMs) {
        logOperation(operation, "SUCCESS", durationMs, null);
    }

    public static void logOperationFailed(String operation, long durationMs, String errorMsg) {
        logOperation(operation, "FAILED", durationMs, errorMsg);
    }

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

    public static void logPerformance(String operation, Duration duration, long thresholdMs) {
        long durationMs = duration.toMillis();
        if (durationMs > thresholdMs) {
            log.warn("性能警告: operation={}, duration={}ms, threshold={}ms",
                    operation, durationMs, thresholdMs);
        }
    }
}
