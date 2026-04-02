package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {

    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
    private List<RoleDTO> roles;
    private List<String> permissions;
}
