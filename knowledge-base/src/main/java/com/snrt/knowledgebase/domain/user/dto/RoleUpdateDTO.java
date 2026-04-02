package com.snrt.knowledgebase.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleUpdateDTO {

    @NotBlank(message = "角色ID不能为空")
    private String id;

    private String name;

    private String description;

    private Integer sort;

    private List<String> permissionIds;
}
