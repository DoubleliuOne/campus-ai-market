package com.campusmarket.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CampusAI Market API",
                version = "v1",
                description = "校园二手交易、图片、收藏、订单状态机、AI 会话与 RAG 接口文档",
                contact = @Contact(name = "CampusAI Market")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "登录后填写 Bearer Token，不需要手动添加 Bearer 前缀"
)
public class OpenApiConfig {
}
