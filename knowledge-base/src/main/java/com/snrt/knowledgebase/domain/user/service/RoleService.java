package com.snrt.knowledgebase.domain.user.service;

import com.snrt.knowledgebase.common.exception.BusinessException;
import com.snrt.knowledgebase.common.exception.ErrorCode;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.user.dto.*;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import com.snrt.knowledgebase.domain.user.entity.Role;
import com.snrt.knowledgebase.domain.user.entity.RolePermission;
import com.snrt.knowledgebase.domain.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public PageResult<RoleDTO> getRolePage(RoleQueryDTO query) {
        Pageable pageable = PageRequest.of(
            query.getPageNum() - 1,
            query.getPageSize(),
            Sort.by(Sort.Direction.ASC, "sort")
        );
        Page<Role> rolePage = roleRepository.findByConditions(query.getName(), query.getCode(), pageable);
        
        List<RoleDTO> roleDTOs = rolePage.getContent().stream()
            .map(role -> {
                RoleDTO dto = roleMapper.toDTO(role);
                dto.setPermissions(getPermissionsByRoleId(role.getId()));
                return dto;
            })
            .collect(Collectors.toList());
        
        return new PageResult<>(roleDTOs, rolePage.getTotalElements(), rolePage.getTotalPages());
    }

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findByIsActiveTrueAndIsDeletedFalseOrderBySortAsc().stream()
            .map(roleMapper::toDTO)
            .collect(Collectors.toList());
    }

    public RoleDTO getRoleById(String id) {
        Role role = findRoleById(id);
        RoleDTO dto = roleMapper.toDTO(role);
        dto.setPermissions(getPermissionsByRoleId(id));
        return dto;
    }

    @Transactional
    public RoleDTO createRole(RoleCreateDTO createDTO) {
        if (roleRepository.existsByName(createDTO.getName())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "角色名称", createDTO.getName());
        }
        if (roleRepository.existsByCode(createDTO.getCode())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "角色编码", createDTO.getCode());
        }

        Role role = new Role();
        role.setName(createDTO.getName());
        role.setCode(createDTO.getCode());
        role.setDescription(createDTO.getDescription());
        role.setSort(createDTO.getSort() != null ? createDTO.getSort() : 0);
        role.setIsActive(true);
        role.setIsDeleted(false);
        
        Role savedRole = roleRepository.save(role);
        
        if (createDTO.getPermissionIds() != null && !createDTO.getPermissionIds().isEmpty()) {
            assignPermissions(savedRole.getId(), createDTO.getPermissionIds());
        }
        
        return getRoleById(savedRole.getId());
    }

    @Transactional
    public RoleDTO updateRole(RoleUpdateDTO updateDTO) {
        Role role = findRoleById(updateDTO.getId());
        
        if (updateDTO.getName() != null && !updateDTO.getName().equals(role.getName())) {
            if (roleRepository.existsByName(updateDTO.getName())) {
                throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "角色名称", updateDTO.getName());
            }
            role.setName(updateDTO.getName());
        }
        
        if (updateDTO.getDescription() != null) {
            role.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getSort() != null) {
            role.setSort(updateDTO.getSort());
        }
        
        Role savedRole = roleRepository.save(role);
        
        if (updateDTO.getPermissionIds() != null) {
            assignPermissions(savedRole.getId(), updateDTO.getPermissionIds());
        }
        
        return getRoleById(savedRole.getId());
    }

    @Transactional
    public void deleteRole(String id) {
        Role role = findRoleById(id);
        
        if (!userRoleRepository.findByRoleId(id).isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该角色下还有用户，无法删除");
        }
        
        rolePermissionRepository.deleteByRoleId(id);
        role.setIsDeleted(true);
        roleRepository.save(role);
    }

    @Transactional
    public void toggleRole(String id) {
        Role role = findRoleById(id);
        role.setIsActive(!role.getIsActive());
        roleRepository.save(role);
    }

    @Transactional
    public void assignPermissions(String roleId, List<String> permissionIds) {
        Role role = findRoleById(roleId);
        
        rolePermissionRepository.deleteByRoleId(roleId);
        
        for (String permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限", permissionId));
            
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionRepository.save(rolePermission);
        }
    }

    public List<PermissionDTO> getPermissionsByRoleId(String roleId) {
        return permissionRepository.findByRoleId(roleId).stream()
            .map(permissionMapper::toDTO)
            .collect(Collectors.toList());
    }

    private Role findRoleById(String id) {
        Optional<Role> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty() || roleOptional.get().getIsDeleted()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色", id);
        }
        return roleOptional.get();
    }
}
