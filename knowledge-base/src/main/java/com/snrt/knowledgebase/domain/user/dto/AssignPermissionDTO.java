package com.snrt.knowledgebase.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionDTO {

    @NotBlank(message = "角色ID不能为空")
    private String roleId;

    @NotEmpty(message = "权限ID列表不能为空")
    private List<String> permissionIds;
}
