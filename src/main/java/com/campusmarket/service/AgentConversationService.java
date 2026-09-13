package com.campusmarket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmarket.ai.service.AiChatService;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.ConversationMessageRequest;
import com.campusmarket.dto.CreateConversationRequest;
import com.campusmarket.entity.AiConversation;
import com.campusmarket.entity.AiMessage;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.AiConversationMapper;
import com.campusmarket.mapper.AiMessageMapper;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.AgentChatResponse;
import com.campusmarket.vo.ConversationMessageVO;
import com.campusmarket.vo.ConversationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AgentConversationService {

    private static final int HISTORY_LIMIT = 10;
    private static final String DEFAULT_TITLE = "新对话";

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiChatService aiChatService;

    public AgentConversationService(AiConversationMapper conversationMapper,
                                    AiMessageMapper messageMapper,
                                    AiChatService aiChatService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.aiChatService = aiChatService;
    }

    public PageResult<ConversationVO> list(LoginUser loginUser, long page, long size) {
        validatePage(page, size);
        IPage<ConversationVO> conversationPage = conversationMapper.selectConversationPage(
                new Page<>(page, size), loginUser.id());
        List<ConversationVO> records = conversationPage.getRecords().stream()
                .peek(record -> {
                    String lastMessage = record.getLastMessage();
                    record.setLastMessage(lastMessage == null
                            ? null
                            : truncate(lastMessage.replaceAll("\\s+", " "), 60));
                })
                .toList();
        return new PageResult<>(records, conversationPage.getTotal(),
                conversationPage.getCurrent(), conversationPage.getSize());
    }

    public ConversationVO create(CreateConversationRequest request, LoginUser loginUser) {
        String title = request == null ? null : trimToNull(request.getTitle());
        AiConversation conversation = new AiConversation();
        conversation.setUserId(loginUser.id());
        conversation.setTitle(title == null ? DEFAULT_TITLE : truncate(title, 60));
        conversationMapper.insert(conversation);
        return toConversationVO(conversationMapper.selectById(conversation.getId()));
    }

    public List<ConversationMessageVO> messages(Long conversationId, LoginUser loginUser) {
        requireOwnedConversation(conversationId, loginUser);
        return messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime)
                        .orderByAsc(AiMessage::getId))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Transactional
    public void delete(Long conversationId, LoginUser loginUser) {
        requireOwnedConversation(conversationId, loginUser);
        messageMapper.delete(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    public AgentChatResponse sendMessage(Long conversationId,
                                         ConversationMessageRequest request,
                                         LoginUser loginUser) {
        AiConversation conversation = requireOwnedConversation(conversationId, loginUser);
        String message = trimToNull(request == null ? null : request.getMessage());
        if (message == null) {
            throw new BusinessException("消息不能为空");
        }
        if (message.length() > 2000) {
            throw new BusinessException("消息内容不能超过2000个字符");
        }

        List<AiMessage> history = loadHistory(conversationId);
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("USER");
        userMessage.setContent(message);
        messageMapper.insert(userMessage);

        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(buildTitle(message));
        }

        String reply;
        try {
            reply = aiChatService.chat(message, loginUser,
                    request == null ? null : request.getItemId(), history);
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException("AI 暂时没有返回内容，请稍后重试");
            }
        } catch (RuntimeException ex) {
            messageMapper.deleteById(userMessage.getId());
            throw ex;
        }

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(reply);
        messageMapper.insert(assistantMessage);

        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return new AgentChatResponse(reply, conversationId);
    }

    private List<AiMessage> loadHistory(Long conversationId) {
        List<AiMessage> latest = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getCreateTime)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + HISTORY_LIMIT));
        List<AiMessage> history = new ArrayList<>(latest);
        Collections.reverse(history);
        return history;
    }

    private AiConversation requireOwnedConversation(Long conversationId, LoginUser loginUser) {
        if (conversationId == null) {
            throw new BusinessException("会话id不能为空");
        }
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(loginUser.id())) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return conversation;
    }

    private ConversationVO toConversationVO(AiConversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        vo.setMessageCount(messageMapper.selectCount(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversation.getId())));

        AiMessage latest = messageMapper.selectOne(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversation.getId())
                .orderByDesc(AiMessage::getCreateTime)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1"));
        if (latest != null) {
            vo.setLastMessage(truncate(latest.getContent().replaceAll("\\s+", " "), 60));
        }
        return vo;
    }

    private ConversationMessageVO toMessageVO(AiMessage message) {
        ConversationMessageVO vo = new ConversationMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String buildTitle(String firstMessage) {
        return truncate(firstMessage.replaceAll("\\s+", " "), 30);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validatePage(long page, long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }
    }
}
