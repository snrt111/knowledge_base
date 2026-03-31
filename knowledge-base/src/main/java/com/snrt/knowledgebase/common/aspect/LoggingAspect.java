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

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
    }

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

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
