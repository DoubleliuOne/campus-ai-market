package com.campusmarket;

import com.campusmarket.mapper.UserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldQueryUserTable() {
        Long count = userMapper.selectCount(null);
        System.out.println("Current user count = " + count);
        Assertions.assertNotNull(count);
    }
}
