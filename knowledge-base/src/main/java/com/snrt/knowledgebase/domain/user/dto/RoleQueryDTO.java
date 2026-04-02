package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

@Data
public class RoleQueryDTO {

    private String name;
    private String code;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
