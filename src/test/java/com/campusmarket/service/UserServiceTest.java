package com.campusmarket.service;

import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRejectUsernameThatBecomesTooShortAfterTrimming() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  a  ");
        request.setPassword("123456");

        BusinessException exception = assertThrows(
                BusinessException.class, () -> userService.register(request));

        assertEquals("用户名长度必须在3到50个字符之间", exception.getMessage());
        verifyNoInteractions(userMapper, passwordEncoder);
    }
}
