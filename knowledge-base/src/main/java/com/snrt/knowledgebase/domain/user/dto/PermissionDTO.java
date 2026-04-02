package com.snrt.knowledgebase.domain.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PermissionDTO {

    private String id;
    private String name;
    private String code;
    private String type;
    private String parentId;
    private String path;
    private String icon;
    private String component;
    private Integer sort;
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<PermissionDTO> children;
}
