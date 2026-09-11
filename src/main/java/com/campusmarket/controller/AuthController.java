package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.dto.LoginRequest;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.UserService;
import com.campusmarket.vo.LoginResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "用户注册、登录、当前用户与登出")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<Long> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功后返回 JWT")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前用户")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<LoginUser> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(loginUser);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "将当前 JWT 加入 Redis 黑名单")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> logout(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        userService.logout(authorizationHeader);
        return ApiResponse.<Void>ok(null);
    }
}
