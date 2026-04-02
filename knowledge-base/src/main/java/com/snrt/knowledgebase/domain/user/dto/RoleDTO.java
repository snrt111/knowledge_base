package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleDTO {

    private String id;
    private String name;
    private String code;
    private String description;
    private Integer sort;
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<PermissionDTO> permissions;
}
