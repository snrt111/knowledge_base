package com.snrt.knowledgebase.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    @NotBlank(message = "用户ID不能为空")
    private String id;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private List<String> roleIds;
}
