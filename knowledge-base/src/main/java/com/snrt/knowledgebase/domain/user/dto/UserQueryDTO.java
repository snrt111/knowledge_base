package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

@Data
public class UserQueryDTO {

    private String username;
    private String nickname;
    private String email;
    private Boolean isActive;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
