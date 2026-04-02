package com.snrt.knowledgebase.common.initializer;

import com.snrt.knowledgebase.common.util.PasswordUtil;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import com.snrt.knowledgebase.domain.user.entity.RolePermission;
import com.snrt.knowledgebase.domain.user.entity.User;
import com.snrt.knowledgebase.domain.user.entity.UserRole;
import com.snrt.knowledgebase.domain.user.repository.PermissionRepository;
import com.snrt.knowledgebase.domain.user.repository.RolePermissionRepository;
import com.snrt.knowledgebase.domain.user.repository.UserRepository;
import com.snrt.knowledgebase.domain.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 数据初始化器
 *
 * 应用启动时自动初始化默认数据，包括：
 * 1. 默认权限数据（菜单权限、操作权限）
 * 2. 默认管理员用户（admin/admin123）
 * 3. 用户-角色关联
 * 4. 角色-权限关联
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordUtil passwordUtil;

    /**
     * 默认管理员配置
     */
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_ADMIN_NICKNAME = "超级管理员";
    private static final String DEFAULT_ADMIN_ROLE_ID = "admin";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始执行数据初始化...");
        initializePermissions();
        initializeAdminUser();
        log.info("数据初始化完成");
    }

    /**
     * 初始化默认权限数据
     *
     * 创建系统所需的菜单权限，包括：
     * - user:menu（用户管理菜单）
     * - role:menu（角色管理菜单）
     */
    private void initializePermissions() {
        log.info("开始初始化权限数据...");

        List<PermissionDefinition> permissions = Arrays.asList(
            new PermissionDefinition("user:menu", "用户管理菜单", "MENU", 1),
            new PermissionDefinition("role:menu", "角色管理菜单", "MENU", 2),
            new PermissionDefinition("user:create", "用户创建", "BUTTON", 3),
            new PermissionDefinition("user:update", "用户修改", "BUTTON", 4),
            new PermissionDefinition("user:delete", "用户删除", "BUTTON", 5),
            new PermissionDefinition("user:view", "用户查看", "BUTTON", 6),
            new PermissionDefinition("role:create", "角色创建", "BUTTON", 7),
            new PermissionDefinition("role:update", "角色修改", "BUTTON", 8),
            new PermissionDefinition("role:delete", "角色删除", "BUTTON", 9),
            new PermissionDefinition("role:view", "角色查看", "BUTTON", 10),
            new PermissionDefinition("role:assign-permission", "分配权限", "BUTTON", 11)
        );

        for (PermissionDefinition def : permissions) {
            Optional<Permission> existingPermission = permissionRepository.findByCode(def.code);
            if (existingPermission.isEmpty()) {
                Permission permission = new Permission();
                permission.setCode(def.code);
                permission.setName(def.name);
                permission.setType(def.type);
                permission.setSort(def.sort);
                permission.setIsActive(true);
                permission.setIsDeleted(false);
                permission.setCreateTime(LocalDateTime.now());
                permission.setUpdateTime(LocalDateTime.now());

                Permission savedPermission = permissionRepository.save(permission);
                log.info("权限创建成功: {} - {}", def.code, def.name);

                // 将权限关联到 admin 角色
                associatePermissionWithAdminRole(savedPermission.getId());
            } else {
                // 确保已存在的权限也关联到 admin 角色
                associatePermissionWithAdminRole(existingPermission.get().getId());
            }
        }

        log.info("权限数据初始化完成");
    }

    /**
     * 将权限关联到 admin 角色
     */
    private void associatePermissionWithAdminRole(String permissionId) {
        List<RolePermission> existingAssociations = rolePermissionRepository.findByRoleId(DEFAULT_ADMIN_ROLE_ID);
        boolean alreadyAssociated = existingAssociations.stream()
            .anyMatch(rp -> rp.getPermissionId().equals(permissionId));

        if (!alreadyAssociated) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(DEFAULT_ADMIN_ROLE_ID);
            rolePermission.setPermissionId(permissionId);
            rolePermission.setCreateTime(LocalDateTime.now());
            rolePermissionRepository.save(rolePermission);
            log.info("权限 {} 已关联到 admin 角色", permissionId);
        }
    }

    /**
     * 初始化默认管理员用户
     *
     * 如果管理员用户不存在，则创建默认管理员：
     * - 用户名: admin
     * - 密码: admin123（使用BCrypt加密）
     * - 角色: 超级管理员
     */
    private void initializeAdminUser() {
        Optional<User> existingAdmin = userRepository.findByUsername(DEFAULT_ADMIN_USERNAME);

        if (existingAdmin.isPresent()) {
            log.info("管理员用户已存在，跳过初始化");
            return;
        }

        log.info("创建默认管理员用户...");

        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername(DEFAULT_ADMIN_USERNAME);
        adminUser.setPassword(passwordUtil.encode(DEFAULT_ADMIN_PASSWORD));
        adminUser.setNickname(DEFAULT_ADMIN_NICKNAME);
        adminUser.setIsActive(true);
        adminUser.setIsDeleted(false);
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());

        User savedUser = userRepository.save(adminUser);
        log.info("管理员用户创建成功，ID: {}", savedUser.getId());

        // 关联超级管理员角色
        UserRole userRole = new UserRole();
        userRole.setUserId(savedUser.getId());
        userRole.setRoleId(DEFAULT_ADMIN_ROLE_ID);
        userRole.setCreateTime(LocalDateTime.now());

        userRoleRepository.save(userRole);
        log.info("管理员角色关联成功");

        log.info("默认管理员初始化完成，用户名: {}，密码: {}", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    /**
     * 权限定义内部类
     */
    private static class PermissionDefinition {
        final String code;
        final String name;
        final String type;
        final int sort;

        PermissionDefinition(String code, String name, String type, int sort) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.sort = sort;
        }
    }
}
