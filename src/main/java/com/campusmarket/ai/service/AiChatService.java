package com.campusmarket.ai.service;

import com.campusmarket.ai.tool.ItemTools;
import com.campusmarket.ai.tool.OrderTools;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.security.LoginUser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         ItemTools itemTools,
                         OrderTools orderTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是校园二手交易助手。"
                        + "如果用户需要查询商品，你必须调用searchItems工具获取真实结果，不能自己编造商品、价格和库存。"
                        + "如果用户想看某个具体商品的详情，你必须调用getItemDetail工具。"
                        + "如果用户查询自己的订单，你必须调用getMyOrders工具，用户身份由系统提供，不能要求用户提供userId。"
                        + "调用工具后，根据返回的JSON商品列表用中文向用户介绍。")
                .defaultTools(itemTools, orderTools)
                .build();
    }

    public String chat(String message, LoginUser loginUser) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息不能为空");
        }

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", loginUser.id());
        toolContext.put("username", loginUser.username());

        return chatClient.prompt()
                .user(message)
                .toolContext(toolContext)
                .call()
                .content();
    }
}
