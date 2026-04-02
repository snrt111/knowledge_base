package com.snrt.knowledgebase.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.user.dto.PermissionDTO;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import com.snrt.knowledgebase.domain.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "权限管理", description = "权限管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    @SaCheckPermission("role:list")
    public ApiResponse<List<PermissionDTO>> getPermissionTree() {
        return ApiResponse.success(permissionService.getPermissionTree());
    }

    @Operation(summary = "分页查询权限列表")
    @GetMapping
    @SaCheckPermission("role:list")
    public ApiResponse<PageResult<PermissionDTO>> getPermissionPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(permissionService.getPermissionPage(name, code, type, pageNum, pageSize));
    }

    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    @SaCheckPermission("role:list")
    public ApiResponse<PermissionDTO> getPermissionById(@PathVariable String id) {
        return ApiResponse.success(permissionService.getPermissionById(id));
    }

    @Operation(summary = "创建权限")
    @PostMapping
    @SaCheckPermission("role:add")
    public ApiResponse<PermissionDTO> createPermission(@RequestBody Permission permission) {
        return ApiResponse.success(permissionService.createPermission(permission));
    }

    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    @SaCheckPermission("role:edit")
    public ApiResponse<PermissionDTO> updatePermission(@PathVariable String id, @RequestBody Permission permission) {
        return ApiResponse.success(permissionService.updatePermission(id, permission));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @SaCheckPermission("role:delete")
    public ApiResponse<Void> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/禁用权限")
    @PutMapping("/{id}/toggle")
    @SaCheckPermission("role:edit")
    public ApiResponse<Void> togglePermission(@PathVariable String id) {
        permissionService.togglePermission(id);
        return ApiResponse.success();
    }
}
