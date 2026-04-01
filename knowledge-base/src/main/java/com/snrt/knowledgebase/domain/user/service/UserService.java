package com.snrt.knowledgebase.domain.user.service;

import com.snrt.knowledgebase.common.exception.BusinessException;
import com.snrt.knowledgebase.common.exception.ErrorCode;
import com.snrt.knowledgebase.common.util.JwtUtil;
import com.snrt.knowledgebase.common.util.PasswordUtil;
import com.snrt.knowledgebase.domain.user.dto.LoginRequest;
import com.snrt.knowledgebase.domain.user.dto.LoginResponse;
import com.snrt.knowledgebase.domain.user.dto.RegisterRequest;
import com.snrt.knowledgebase.domain.user.dto.UserDTO;
import com.snrt.knowledgebase.domain.user.entity.User;
import com.snrt.knowledgebase.domain.user.repository.UserMapper;
import com.snrt.knowledgebase.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户服务
 * 
 * 提供用户相关的业务逻辑：登录、注册、获取用户信息等
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
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

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // 生成认证令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        String token = jwtUtil.generateToken(user.getId(), claims);

        // 转换为 DTO
        UserDTO userDTO = UserMapper.INSTANCE.toDTO(user);
        return new LoginResponse(userDTO, token);
    }

    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 用户信息
     */
    @Transactional
    public UserDTO register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "用户名", request.getUsername());
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "邮箱", request.getEmail());
        }

        // 检查手机号是否已存在
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "手机号", request.getPhone());
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsDeleted(false);

        User savedUser = userRepository.save(user);
        return UserMapper.INSTANCE.toDTO(savedUser);
    }

    /**
     * 根据用户ID获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserDTO getUserById(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户", userId);
        }
        return UserMapper.INSTANCE.toDTO(userOptional.get());
    }

    /**
     * 根据用户名获取用户信息
     * 
     * @param username 用户名
     * @return 用户信息
     */
    public UserDTO getUserByUsername(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户", username);
        }
        return UserMapper.INSTANCE.toDTO(userOptional.get());
    }
}
