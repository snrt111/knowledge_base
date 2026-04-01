package com.snrt.knowledgebase.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 追踪ID过滤器
 * 
 * 在每个请求中生成和传递追踪ID（traceId）：
 * - 从请求头获取或生成新的traceId
 * - 将traceId设置到请求属性和响应头
 * - 记录请求开始和结束日志
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_ATTRIBUTE = "traceId";

    /**
     * 过滤器核心逻辑
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException 异常
     * @throws IOException 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        log.debug("[{}] 请求开始: {} {}", traceId, request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("[{}] 请求结束: {} {}, 状态: {}",
                    traceId, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
