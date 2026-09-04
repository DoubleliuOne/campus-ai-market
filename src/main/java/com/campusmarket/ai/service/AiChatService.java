package com.campusmarket.ai.service;

import com.campusmarket.ai.tool.ItemTools;
import com.campusmarket.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder, ItemTools itemTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是校园二手交易助手。"
                        + "如果用户需要查询商品，你必须调用searchItems工具获取真实结果，不能自己编造商品、价格和库存。"
                        + "调用工具后，根据返回的JSON商品列表用中文向用户介绍。")
                .defaultTools(itemTools)
                .build();
    }

    public String chat(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息不能为空");
        }
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
