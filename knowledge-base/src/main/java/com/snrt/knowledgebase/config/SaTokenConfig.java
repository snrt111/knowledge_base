package com.snrt.knowledgebase.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 
 * 配置 Sa-Token 相关设置，替代 Spring Security
 * 
 * @author SNRT
 * @since 1.0
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，只拦截需要登录的路径
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 检查是否登录
            StpUtil.checkLogin();
        }))
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/**",
            "/api/health",
            "/api/v3/api-docs/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html"
        );
    }
}

