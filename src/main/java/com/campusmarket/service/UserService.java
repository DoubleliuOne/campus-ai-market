package com.campusmarket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusmarket.dto.LoginRequest;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.entity.User;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.JwtUtil;
import com.campusmarket.vo.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public UserService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       TokenBlacklistService tokenBlacklistService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public Long register(RegisterRequest request) {
        String username = trimToNull(request.getUsername());
        String password = request.getPassword();

        if (username == null) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException("用户名长度必须在3到50个字符之间");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 72) {
            throw new BusinessException("密码长度必须在6到72个字符之间");
        }

        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCampus(trimToNull(request.getCampus()));

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("用户名已存在");
        }
        return user.getId();
    }

    public LoginResponse login(LoginRequest request) {
        String username = trimToNull(request.getUsername());

        if (username == null || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("用户名或密码不能为空");
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException("缺少token");
        }
        tokenBlacklistService.blacklist(authorizationHeader.substring(7));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
