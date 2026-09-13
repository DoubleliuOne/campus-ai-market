package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "创建 AI 会话请求")
public class CreateConversationRequest {

    @Size(max = 60, message = "会话标题不能超过60个字符")
    @Schema(description = "会话标题，可不填", example = "寻找机械键盘")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
