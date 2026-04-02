package com.snrt.knowledgebase.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.user.dto.*;
import com.snrt.knowledgebase.domain.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理", description = "角色管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色列表")
    @GetMapping
    @SaCheckPermission("role:list")
    public ApiResponse<PageResult<RoleDTO>> getRolePage(RoleQueryDTO query) {
        return ApiResponse.success(roleService.getRolePage(query));
    }

    @Operation(summary = "获取所有角色")
    @GetMapping("/all")
    @SaCheckPermission("role:list")
    public ApiResponse<List<RoleDTO>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @SaCheckPermission("role:list")
    public ApiResponse<RoleDTO> getRoleById(@PathVariable String id) {
        return ApiResponse.success(roleService.getRoleById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    @SaCheckPermission("role:add")
    public ApiResponse<RoleDTO> createRole(@Valid @RequestBody RoleCreateDTO createDTO) {
        return ApiResponse.success(roleService.createRole(createDTO));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @SaCheckPermission("role:edit")
    public ApiResponse<RoleDTO> updateRole(@PathVariable String id, @Valid @RequestBody RoleUpdateDTO updateDTO) {
        updateDTO.setId(id);
        return ApiResponse.success(roleService.updateRole(updateDTO));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @SaCheckPermission("role:delete")
    public ApiResponse<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/禁用角色")
    @PutMapping("/{id}/toggle")
    @SaCheckPermission("role:edit")
    public ApiResponse<Void> toggleRole(@PathVariable String id) {
        roleService.toggleRole(id);
        return ApiResponse.success();
    }

    @Operation(summary = "分配权限给角色")
    @PutMapping("/{id}/permissions")
    @SaCheckPermission("role:edit")
    public ApiResponse<Void> assignPermissions(@PathVariable String id, @Valid @RequestBody AssignPermissionDTO assignDTO) {
        assignDTO.setRoleId(id);
        roleService.assignPermissions(assignDTO.getRoleId(), assignDTO.getPermissionIds());
        return ApiResponse.success();
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    @SaCheckPermission("role:list")
    public ApiResponse<List<PermissionDTO>> getRolePermissions(@PathVariable String id) {
        return ApiResponse.success(roleService.getPermissionsByRoleId(id));
    }
}
