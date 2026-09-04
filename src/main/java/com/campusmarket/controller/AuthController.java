package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.dto.LoginRequest;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.UserService;
import com.campusmarket.vo.LoginResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<LoginUser> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(loginUser);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        userService.logout(authorizationHeader);
        return ApiResponse.<Void>ok(null);
    }
}
