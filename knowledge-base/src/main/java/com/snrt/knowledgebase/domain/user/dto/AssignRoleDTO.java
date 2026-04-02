package com.snrt.knowledgebase.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRoleDTO {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotEmpty(message = "角色ID列表不能为空")
    private List<String> roleIds;
}
