package com.snrt.knowledgebase.common.util;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 认证工具类
 * 
 * 基于 Sa-Token 实现认证相关功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Component
public class JwtUtil {

    /**
     * 生成认证令牌
     * 
     * @param userId 用户ID
     * @param claims 额外的声明信息
     * @return 认证令牌
     */
    public String generateToken(String userId, Map<String, Object> claims) {
        // 登录认证，生成 token
        StpUtil.login(userId);
        
        // 设置额外信息
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            StpUtil.getSession().set(entry.getKey(), entry.getValue());
        }
        
        // 返回 token
        return StpUtil.getTokenValue();
    }

    /**
     * 从令牌中获取用户ID
     * 
     * @param token 认证令牌
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        // 解析 token 获取用户ID
        return StpUtil.getLoginIdByToken(token).toString();
    }

}

