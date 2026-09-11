package com.campusmarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 助手响应")
public class AgentChatResponse {

    @Schema(description = "Markdown 格式的 AI 回答",
            example = "找到一件符合条件的机械键盘，售价99元。")
    private String reply;

    public AgentChatResponse() {
    }

    public AgentChatResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
