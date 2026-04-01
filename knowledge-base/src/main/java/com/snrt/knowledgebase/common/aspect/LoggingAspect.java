package com.snrt.knowledgebase.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志切面
 * 
 * 统一记录Controller和Service层方法的执行日志：
 * - 记录方法执行耗时
 * - 性能告警（超过3秒）
 * - 异常日志记录
 * 
 * @author SNRT
 * @since 1.0
 */
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
     * 环绕通知：记录方法执行日志
     * 
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("controllerPointcut() || servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        HttpServletRequest request = getCurrentRequest();
        String traceId = request != null ? (String) request.getAttribute("traceId") : "";

        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());

            log.info("[{}] [方法执行] {}.{} 耗时: {}ms",
                    traceId, className, methodName, duration.toMillis());

            if (duration.toMillis() > 3000) {
                log.warn("[{}] [性能告警] {}.{} 执行时间过长: {}ms",
                        traceId, className, methodName, duration.toMillis());
            }

            return result;
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("[{}] [方法异常] {}.{} 耗时: {}ms, 异常: {}",
                    traceId, className, methodName, duration.toMillis(), e.getMessage());
            throw e;
        }
    }

    /**
     * 获取当前请求的HttpServletRequest
     * 
     * @return HttpServletRequest实例
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
