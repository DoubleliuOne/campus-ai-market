package com.campusmarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 助手响应")
public class AgentChatResponse {

    @Schema(description = "Markdown 格式的 AI 回答",
            example = "找到一件符合条件的机械键盘，售价99元。")
    private String reply;

    @Schema(description = "持久化会话id；旧版直接聊天接口可能为空", example = "12")
    private Long conversationId;

    public AgentChatResponse() {
    }

    public AgentChatResponse(String reply) {
        this.reply = reply;
    }

    public AgentChatResponse(String reply, Long conversationId) {
        this.reply = reply;
        this.conversationId = conversationId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}
