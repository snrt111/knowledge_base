package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 * 
 * 包含用户信息和访问令牌
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class LoginResponse {

    private UserDTO user;
    private String token;
    private String tokenType = "Bearer";

    public LoginResponse(UserDTO user, String token) {
        this.user = user;
        this.token = token;
    }
}
