package com.snrt.knowledgebase.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.user.dto.*;
import com.snrt.knowledgebase.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理", description = "用户管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    @SaCheckPermission("user:list")
    public ApiResponse<PageResult<UserDTO>> getUserPage(UserQueryDTO query) {
        return ApiResponse.success(userService.getUserPage(query));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @SaCheckPermission("user:list")
    public ApiResponse<UserDTO> getUserById(@PathVariable String id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @SaCheckPermission("user:add")
    public ApiResponse<UserDTO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        return ApiResponse.success(userService.createUser(createDTO));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @SaCheckPermission("user:edit")
    public ApiResponse<UserDTO> updateUser(@PathVariable String id, @Valid @RequestBody UserUpdateDTO updateDTO) {
        updateDTO.setId(id);
        return ApiResponse.success(userService.updateUser(updateDTO));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @SaCheckPermission("user:delete")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/toggle")
    @SaCheckPermission("user:edit")
    public ApiResponse<Void> toggleUser(@PathVariable String id) {
        userService.toggleUser(id);
        return ApiResponse.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @SaCheckPermission("user:edit")
    public ApiResponse<Void> resetPassword(@PathVariable String id, @Valid @RequestBody ResetPasswordDTO resetDTO) {
        resetDTO.setUserId(id);
        userService.resetPassword(resetDTO.getUserId(), resetDTO.getNewPassword());
        return ApiResponse.success();
    }

    @Operation(summary = "分配角色给用户")
    @PutMapping("/{id}/roles")
    @SaCheckPermission("user:edit")
    public ApiResponse<Void> assignRoles(@PathVariable String id, @Valid @RequestBody AssignRoleDTO assignDTO) {
        assignDTO.setUserId(id);
        userService.assignRoles(assignDTO.getUserId(), assignDTO.getRoleIds());
        return ApiResponse.success();
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/{id}/roles")
    @SaCheckPermission("user:list")
    public ApiResponse<List<RoleDTO>> getUserRoles(@PathVariable String id) {
        return ApiResponse.success(userService.getRolesByUserId(id));
    }
}
