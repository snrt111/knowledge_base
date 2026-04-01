package com.snrt.knowledgebase.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志切面类
 * 
 * <p>统一记录Controller和Service层方法的执行日志，提供以下功能：</p>
 * <ul>
 *   <li>记录方法执行耗时</li>
 *   <li>性能告警（执行时间超过3秒）</li>
 *   <li>异常日志记录</li>
 *   <li>支持traceId追踪</li>
 * </ul>
 * 
 * <p>切点范围：</p>
 * <ul>
 *   <li>Controller层：带有 {@link org.springframework.web.bind.annotation.RestController} 注解的类</li>
 *   <li>Service层：带有 {@link org.springframework.stereotype.Service} 注解的类</li>
 * </ul>
 * 
 * @author SNRT
 * @since 1.0
 */
@Order(1)
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Controller层切点
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    /**
     * Service层切点
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
    }

    /**
     * 环绕通知，记录方法执行日志
     * 
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("controllerPointcut() || servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodContext context = buildContext(joinPoint);
        Instant start = context.getStartTime();

        try {
            Object result = joinPoint.proceed();
            context.setEndTime(Instant.now());
            logSuccess(context);
            return result;
        } catch (Exception e) {
            context.setEndTime(Instant.now());
            logException(context, e);
            throw e;
        }
    }

    /**
     * 构建方法执行上下文
     * 
     * @param joinPoint 切入点
     * @return 方法执行上下文
     */
    private MethodContext buildContext(ProceedingJoinPoint joinPoint) {
        MethodContext context = new MethodContext();
        context.setStartTime(Instant.now());
        context.setClassName(joinPoint.getSignature().getDeclaringTypeName());
        context.setMethodName(joinPoint.getSignature().getName());
        context.setTraceId(getTraceId());
        return context;
    }

    /**
     * 记录方法执行成功日志
     * 
     * @param context 方法执行上下文
     */
    private void logSuccess(MethodContext context) {
        long duration = Duration.between(context.getStartTime(), context.getEndTime()).toMillis();
        log.info("[{}] [方法执行] {}.{} 耗时: {}ms", context.getTraceId(), context.getClassName(), context.getMethodName(), duration);
        if (duration > 3000) {
            logWarn(context, duration);
        }
    }

    /**
     * 记录性能告警日志
     * 
     * @param context 方法执行上下文
     * @param duration 执行耗时（毫秒）
     */
    private void logWarn(MethodContext context, long duration) {
        log.warn("[{}] [性能告警] {}.{} 执行时间过长: {}ms", context.getTraceId(), context.getClassName(), context.getMethodName(), duration);
    }

    /**
     * 记录方法执行异常日志
     * 
     * @param context 方法执行上下文
     * @param e 异常对象
     */
    private void logException(MethodContext context, Exception e) {
        long duration = Duration.between(context.getStartTime(), context.getEndTime()).toMillis();
        log.error("[{}] [方法异常] {}.{} 耗时: {}ms, 异常: {}", context.getTraceId(), context.getClassName(), context.getMethodName(), duration, e.getMessage());
    }

    /**
     * 获取当前请求的traceId
     * 
     * @return traceId字符串
     */
    private String getTraceId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        HttpServletRequest request = attributes.getRequest();
        return request != null ? (String) request.getAttribute("traceId") : "";
    }

    /**
     * 方法执行上下文
     * 
     * 封装方法执行过程中的关键信息
     */
    private static class MethodContext {
        private Instant startTime;
        private Instant endTime;
        private String className;
        private String methodName;
        private String traceId;

        /**
         * 获取开始时间
         * 
         * @return 开始时间
         */
        public Instant getStartTime() {
            return startTime;
        }

        /**
         * 设置开始时间
         * 
         * @param startTime 开始时间
         */
        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        /**
         * 获取结束时间
         * 
         * @return 结束时间
         */
        public Instant getEndTime() {
            return endTime;
        }

        /**
         * 设置结束时间
         * 
         * @param endTime 结束时间
         */
        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        /**
         * 获取类名
         * 
         * @return 类名
         */
        public String getClassName() {
            return className;
        }

        /**
         * 设置类名
         * 
         * @param className 类名
         */
        public void setClassName(String className) {
            this.className = className;
        }

        /**
         * 获取方法名
         * 
         * @return 方法名
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * 设置方法名
         * 
         * @param methodName 方法名
         */
        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        /**
         * 获取traceId
         * 
         * @return traceId
         */
        public String getTraceId() {
            return traceId;
        }

        /**
         * 设置traceId
         * 
         * @param traceId traceId
         */
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }
}
