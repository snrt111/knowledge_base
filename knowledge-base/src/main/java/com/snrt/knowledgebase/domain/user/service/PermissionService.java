package com.snrt.knowledgebase.domain.user.service;

import com.snrt.knowledgebase.common.exception.BusinessException;
import com.snrt.knowledgebase.common.exception.ErrorCode;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.user.dto.PermissionDTO;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import com.snrt.knowledgebase.domain.user.entity.RolePermission;
import com.snrt.knowledgebase.domain.user.repository.PermissionMapper;
import com.snrt.knowledgebase.domain.user.repository.PermissionRepository;
import com.snrt.knowledgebase.domain.user.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionMapper permissionMapper;

    public List<PermissionDTO> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findByIsActiveTrueAndIsDeletedFalseOrderBySortAsc();
        List<PermissionDTO> allDTOs = allPermissions.stream()
            .map(permissionMapper::toDTO)
            .collect(Collectors.toList());
        
        return buildTree(allDTOs, null);
    }

    public PageResult<PermissionDTO> getPermissionPage(String name, String code, String type, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(
            pageNum - 1,
            pageSize,
            Sort.by(Sort.Direction.ASC, "sort")
        );
        Page<Permission> permissionPage = permissionRepository.findByConditions(name, code, type, pageable);
        
        List<PermissionDTO> permissionDTOs = permissionPage.getContent().stream()
            .map(permissionMapper::toDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(permissionDTOs, permissionPage.getTotalElements(), permissionPage.getTotalPages());
    }

    public PermissionDTO getPermissionById(String id) {
        Permission permission = findPermissionById(id);
        return permissionMapper.toDTO(permission);
    }

    @Transactional
    public PermissionDTO createPermission(Permission permission) {
        if (permissionRepository.existsByCode(permission.getCode())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "权限编码", permission.getCode());
        }
        permission.setIsActive(true);
        permission.setIsDeleted(false);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDTO(savedPermission);
    }

    @Transactional
    public PermissionDTO updatePermission(String id, Permission permission) {
        Permission existingPermission = findPermissionById(id);
        
        if (permission.getCode() != null && !permission.getCode().equals(existingPermission.getCode())) {
            if (permissionRepository.existsByCode(permission.getCode())) {
                throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "权限编码", permission.getCode());
            }
            existingPermission.setCode(permission.getCode());
        }
        
        if (permission.getName() != null) {
            existingPermission.setName(permission.getName());
        }
        if (permission.getType() != null) {
            existingPermission.setType(permission.getType());
        }
        if (permission.getParentId() != null) {
            existingPermission.setParentId(permission.getParentId());
        }
        if (permission.getPath() != null) {
            existingPermission.setPath(permission.getPath());
        }
        if (permission.getIcon() != null) {
            existingPermission.setIcon(permission.getIcon());
        }
        if (permission.getComponent() != null) {
            existingPermission.setComponent(permission.getComponent());
        }
        if (permission.getSort() != null) {
            existingPermission.setSort(permission.getSort());
        }
        
        Permission savedPermission = permissionRepository.save(existingPermission);
        return permissionMapper.toDTO(savedPermission);
    }

    @Transactional
    public void deletePermission(String id) {
        Permission permission = findPermissionById(id);
        
        List<Permission> children = permissionRepository.findByParentIdAndIsActiveTrueAndIsDeletedFalseOrderBySortAsc(id);
        if (!children.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该权限下还有子权限，无法删除");
        }
        
        rolePermissionRepository.deleteByPermissionId(id);
        permission.setIsDeleted(true);
        permissionRepository.save(permission);
    }

    @Transactional
    public void togglePermission(String id) {
        Permission permission = findPermissionById(id);
        permission.setIsActive(!permission.getIsActive());
        permissionRepository.save(permission);
    }

    private List<PermissionDTO> buildTree(List<PermissionDTO> allPermissions, String parentId) {
        List<PermissionDTO> tree = new ArrayList<>();
        for (PermissionDTO permission : allPermissions) {
            if ((parentId == null && permission.getParentId() == null) || 
                (parentId != null && parentId.equals(permission.getParentId()))) {
                permission.setChildren(buildTree(allPermissions, permission.getId()));
                tree.add(permission);
            }
        }
        return tree;
    }

    private Permission findPermissionById(String id) {
        Optional<Permission> permissionOptional = permissionRepository.findById(id);
        if (permissionOptional.isEmpty() || permissionOptional.get().getIsDeleted()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限", id);
        }
        return permissionOptional.get();
    }
}
