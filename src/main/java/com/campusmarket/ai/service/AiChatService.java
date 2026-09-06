package com.campusmarket.ai.service;

import com.campusmarket.ai.tool.ItemTools;
import com.campusmarket.ai.tool.OrderTools;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.ItemService;
import com.campusmarket.vo.ItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         ItemTools itemTools,
                         OrderTools orderTools,
                         ItemService itemService,
                         ObjectMapper objectMapper) {
        this.itemService = itemService;
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder
                .defaultSystem("你是校园二手交易助手。"
                        + "如果用户需要查询商品，你必须调用searchItems工具获取真实结果，不能自己编造商品、价格和库存。"
                        + "如果用户想看某个具体商品的详情，你必须调用getItemDetail工具。"
                        + "如果用户查询自己的订单，你必须调用getMyOrders工具，用户身份由系统提供，不能要求用户提供userId。"
                        + "如果用户让你推荐或帮他挑选商品，你必须调用recommendItems工具，不能根据你自己的知识凭空推荐商品。"
                        + "如果商品信息中缺少用户需要的参数，你必须明确说明没有该参数，不能自己编造配置、成色或使用情况。"
                        + "没有搜索到商品时，要诚实告知用户没有找到，并建议放宽条件，不能假装有结果。"
                        + "调用工具后，根据返回的JSON数据用中文向用户介绍。")
                .defaultTools(itemTools, orderTools)
                .build();
    }

    public String chat(String message, LoginUser loginUser, Long itemId) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息不能为空");
        }

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", loginUser.id());
        toolContext.put("username", loginUser.username());

        String userContent = buildItemContext(message, itemId);
        return chatClient.prompt()
                .user(userContent)
                .toolContext(toolContext)
                .call()
                .content();
    }

    private String buildItemContext(String message, Long itemId) {
        if (itemId == null) {
            return message;
        }

        ItemVO item = itemService.getDetail(itemId);
        try {
            String itemJson = objectMapper.writeValueAsString(item);
            return "用户当前正在查看的真实商品信息：\n" + itemJson + "\n\n用户问题：" + message;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("商品上下文生成失败");
        }
    }
}
