package com.campusmarket.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void shouldRejectWeakSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 60));
    }

    @Test
    void shouldGenerateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil("01234567890123456789012345678901", 60);

        LoginUser loginUser = jwtUtil.parseToken(jwtUtil.generateToken(7L, "alice"));

        assertEquals(7L, loginUser.id());
        assertEquals("alice", loginUser.username());
    }
}
