package com.snrt.knowledgebase.config;

import cn.dev33.satoken.stp.StpInterface;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import com.snrt.knowledgebase.domain.user.entity.Role;
import com.snrt.knowledgebase.domain.user.entity.UserRole;
import com.snrt.knowledgebase.domain.user.repository.PermissionRepository;
import com.snrt.knowledgebase.domain.user.repository.RoleRepository;
import com.snrt.knowledgebase.domain.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String userId = (String) loginId;
        List<Permission> permissions = permissionRepository.findByUserId(userId);
        return permissions.stream()
            .map(Permission::getCode)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String userId = (String) loginId;
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<String> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Role> roles = roleRepository.findAllById(roleIds);
        return roles.stream()
            .filter(role -> role.getIsActive() && !role.getIsDeleted())
            .map(Role::getCode)
            .collect(Collectors.toList());
    }
}
