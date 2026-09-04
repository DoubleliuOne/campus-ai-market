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
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Long register(RegisterRequest request) {
        String username = trimToNull(request.getUsername());
        String password = request.getPassword();

        if (username == null) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
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

        userMapper.insert(user);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
