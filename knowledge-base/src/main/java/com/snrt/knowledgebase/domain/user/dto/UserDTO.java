package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO
 * 
 * 包含用户的基本信息，用于返回给前端
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class UserDTO {

    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}
