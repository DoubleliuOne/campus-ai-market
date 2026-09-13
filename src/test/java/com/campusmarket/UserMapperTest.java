package com.campusmarket;

import com.campusmarket.mapper.UserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.ai.openai.api-key=test-key",
        "spring.ai.model.embedding=none",
        "jwt.secret=test-jwt-secret-at-least-32-characters-long"
})
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void applicationContextLoadsWithoutExternalServices() {
        Assertions.assertNotNull(userMapper);
    }
}
