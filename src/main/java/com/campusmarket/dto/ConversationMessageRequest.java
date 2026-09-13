package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 会话消息请求")
public class ConversationMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户消息", example = "有没有便宜一点的？",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Positive(message = "商品id必须为正数")
    @Schema(description = "可选商品id，用于结合商品上下文", example = "5")
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
