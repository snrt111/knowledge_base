package com.snrt.knowledgebase.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.domain.user.dto.LoginRequest;
import com.snrt.knowledgebase.domain.user.dto.LoginResponse;
import com.snrt.knowledgebase.domain.user.dto.RegisterRequest;
import com.snrt.knowledgebase.domain.user.dto.UserDTO;
import com.snrt.knowledgebase.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * 
 * 提供用户登录、注册等认证相关的 REST API 接口
 * 
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "用户认证", description = "用户登录、注册等认证操作")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    /**
     * 获取当前用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("/user")
    public ApiResponse<UserDTO> getCurrentUser() {
        // 从 Sa-Token 获取当前用户ID
        String userId = StpUtil.getLoginIdAsString();
        return ApiResponse.success(userService.getUserById(userId));
    }
}

