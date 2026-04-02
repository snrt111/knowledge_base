package com.snrt.knowledgebase.domain.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.snrt.knowledgebase.common.exception.BusinessException;
import com.snrt.knowledgebase.common.exception.ErrorCode;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.common.util.JwtUtil;
import com.snrt.knowledgebase.common.util.PasswordUtil;
import com.snrt.knowledgebase.domain.user.dto.*;
import com.snrt.knowledgebase.domain.user.entity.*;
import com.snrt.knowledgebase.domain.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public LoginResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        if (userOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }

        User user = userOptional.get();
        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }

        if (!user.getIsActive()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        UserDTO userDTO = getUserWithDetails(user.getId());
        return new LoginResponse(userDTO, token);
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "用户名", request.getUsername());
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "邮箱", request.getEmail());
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "手机号", request.getPhone());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsDeleted(false);

        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    public PageResult<UserDTO> getUserPage(UserQueryDTO query) {
        Pageable pageable = PageRequest.of(
            query.getPageNum() - 1,
            query.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );
        
        Page<User> userPage = userRepository.findAll(pageable);
        
        List<UserDTO> userDTOs = userPage.getContent().stream()
            .map(user -> getUserWithDetails(user.getId()))
            .collect(Collectors.toList());
        
        return new PageResult<>(userDTOs, userPage.getTotalElements(), userPage.getTotalPages());
    }

    public UserDTO getUserById(String userId) {
        return getUserWithDetails(userId);
    }

    @Transactional
    public UserDTO createUser(UserCreateDTO createDTO) {
        if (userRepository.existsByUsername(createDTO.getUsername())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "用户名", createDTO.getUsername());
        }

        User user = new User();
        user.setUsername(createDTO.getUsername());
        user.setPassword(passwordUtil.encode(createDTO.getPassword()));
        user.setNickname(createDTO.getNickname() != null ? createDTO.getNickname() : createDTO.getUsername());
        user.setEmail(createDTO.getEmail());
        user.setPhone(createDTO.getPhone());
        user.setAvatar(createDTO.getAvatar());
        user.setIsActive(true);
        user.setIsDeleted(false);

        User savedUser = userRepository.save(user);

        if (createDTO.getRoleIds() != null && !createDTO.getRoleIds().isEmpty()) {
            assignRoles(savedUser.getId(), createDTO.getRoleIds());
        }

        return getUserWithDetails(savedUser.getId());
    }

    @Transactional
    public UserDTO updateUser(UserUpdateDTO updateDTO) {
        User user = findUserById(updateDTO.getId());

        if (updateDTO.getNickname() != null) {
            user.setNickname(updateDTO.getNickname());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
        }

        User savedUser = userRepository.save(user);

        if (updateDTO.getRoleIds() != null) {
            assignRoles(savedUser.getId(), updateDTO.getRoleIds());
        }

        return getUserWithDetails(savedUser.getId());
    }

    @Transactional
    public void deleteUser(String id) {
        User user = findUserById(id);
        String currentUserId = StpUtil.getLoginIdAsString();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能删除当前登录用户");
        }
        userRoleRepository.deleteByUserId(id);
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Transactional
    public void toggleUser(String id) {
        User user = findUserById(id);
        String currentUserId = StpUtil.getLoginIdAsString();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能禁用当前登录用户");
        }
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String userId, String newPassword) {
        User user = findUserById(userId);
        user.setPassword(passwordUtil.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void assignRoles(String userId, List<String> roleIds) {
        User user = findUserById(userId);
        
        userRoleRepository.deleteByUserId(userId);
        
        for (String roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色", roleId));
            
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }

    public List<RoleDTO> getRolesByUserId(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<String> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return roleRepository.findAllById(roleIds).stream()
            .filter(role -> role.getIsActive() && !role.getIsDeleted())
            .map(roleMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<String> getPermissionCodesByUserId(String userId) {
        List<Permission> permissions = permissionRepository.findByUserId(userId);
        return permissions.stream()
            .map(Permission::getCode)
            .collect(Collectors.toList());
    }

    private UserDTO getUserWithDetails(String userId) {
        User user = findUserById(userId);
        UserDTO userDTO = userMapper.toDTO(user);
        userDTO.setRoles(getRolesByUserId(userId));
        userDTO.setPermissions(getPermissionCodesByUserId(userId));
        return userDTO;
    }

    private User findUserById(String id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty() || userOptional.get().getIsDeleted()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户", id);
        }
        return userOptional.get();
    }
}
