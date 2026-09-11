package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 助手请求")
public class AgentChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户问题", example = "帮我找200元以内的机械键盘",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Positive(message = "商品id必须为正数")
    @Schema(description = "可选的商品id，用于商品上下文问答", example = "5")
    private Long itemId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
