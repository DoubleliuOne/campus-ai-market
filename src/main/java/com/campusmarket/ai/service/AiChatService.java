package com.campusmarket.ai.service;

import com.campusmarket.ai.rag.KnowledgeBaseService;
import com.campusmarket.ai.tool.ItemTools;
import com.campusmarket.ai.tool.OrderTools;
import com.campusmarket.ai.tool.ToolCallBudget;
import com.campusmarket.entity.AiMessage;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.exception.ServiceUnavailableException;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.ItemService;
import com.campusmarket.vo.ItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_MESSAGE_CHARS = 4000;
    private static final int MAX_HISTORY_TOTAL_CHARS = 12000;

    private final ChatClient chatClient;
    private final ItemService itemService;
    private final ObjectMapper objectMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final String apiKey;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         ItemTools itemTools,
                         OrderTools orderTools,
                         ItemService itemService,
                         ObjectMapper objectMapper,
                         KnowledgeBaseService knowledgeBaseService,
                         @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.itemService = itemService;
        this.objectMapper = objectMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.apiKey = apiKey;
        this.chatClient = chatClientBuilder
                .defaultSystem("你是校园二手交易助手。"
                        + "如果用户需要查询商品，你必须调用searchItems工具获取真实结果，不能自己编造商品、价格和库存。"
                        + "如果用户想看某个具体商品的详情，你必须调用getItemDetail工具。"
                        + "如果用户查询自己的订单，你必须调用getMyOrders工具，用户身份由系统提供，不能要求用户提供userId。"
                        + "如果用户让你推荐或帮他挑选商品，你必须调用recommendItems工具，不能根据你自己的知识凭空推荐商品。"
                        + "如果商品信息中缺少用户需要的参数，你必须明确说明没有该参数，不能自己编造配置、成色或使用情况。"
                        + "涉及平台禁售、发布、交易、退款规则的问题，必须依据用户消息中检索到的平台规则内容回答，不能编造平台规则。"
                        + "没有搜索到商品时，要诚实告知用户没有找到，并建议放宽条件，不能假装有结果。"
                        + "调用工具后，根据返回的JSON数据用中文向用户介绍。")
                .defaultTools(itemTools, orderTools)
                .build();
    }

    public String chat(String message, LoginUser loginUser, Long itemId) {
        return chat(message, loginUser, itemId, List.of());
    }

    public String chat(String message,
                       LoginUser loginUser,
                       Long itemId,
                       List<AiMessage> history) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceUnavailableException("AI 服务未配置，请联系管理员");
        }

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", loginUser.id());
        toolContext.put("username", loginUser.username());
        toolContext.put("toolCallBudget", new ToolCallBudget());

        List<Message> messages = buildMessages(message, itemId, loginUser, history);
        try {
            return chatClient.prompt()
                    .messages(messages)
                    .toolContext(toolContext)
                    .call()
                    .content();
        } catch (ServiceUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("AI chat request failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("AI 服务暂时不可用，请稍后重试");
        }
    }

    private List<Message> buildMessages(String message,
                                        Long itemId,
                                        LoginUser loginUser,
                                        List<AiMessage> history) {
        List<Message> messages = new ArrayList<>();
        int historyChars = 0;
        if (history != null) {
            for (AiMessage historyMessage : history) {
                if (historyMessage == null || historyMessage.getContent() == null) {
                    continue;
                }
                String content = truncate(historyMessage.getContent(), MAX_HISTORY_MESSAGE_CHARS);
                if (historyChars + content.length() > MAX_HISTORY_TOTAL_CHARS) {
                    continue;
                }
                if ("USER".equals(historyMessage.getRole())) {
                    messages.add(new UserMessage(content));
                    historyChars += content.length();
                } else if ("ASSISTANT".equals(historyMessage.getRole())) {
                    messages.add(new AssistantMessage(content));
                    historyChars += content.length();
                }
            }
        }
        messages.add(new UserMessage(buildItemContext(message, itemId, loginUser)));
        return messages;
    }

    private String buildItemContext(String message, Long itemId, LoginUser loginUser) {
        StringBuilder userContent = new StringBuilder();

        String rules = knowledgeBaseService.retrieveRules(message);
        if (rules != null) {
            userContent.append("以下是检索到的平台规则知识：\n")
                    .append(rules)
                    .append("\n\n");
        }

        if (itemId == null) {
            return userContent.length() == 0 ? message : userContent.append(message).toString();
        }

        ItemVO item = itemService.getDetailForUser(itemId, loginUser);
        try {
            String itemJson = objectMapper.writeValueAsString(item);
            userContent.append("用户当前正在查看的真实商品信息：\n")
                    .append(itemJson)
                    .append("\n\n");
        } catch (JsonProcessingException ex) {
            throw new BusinessException("商品上下文生成失败");
        }

        return userContent.append(message).toString();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
