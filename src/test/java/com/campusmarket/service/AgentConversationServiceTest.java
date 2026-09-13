package com.campusmarket.service;

import com.campusmarket.ai.service.AiChatService;
import com.campusmarket.dto.ConversationMessageRequest;
import com.campusmarket.entity.AiConversation;
import com.campusmarket.entity.AiMessage;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.AiConversationMapper;
import com.campusmarket.mapper.AiMessageMapper;
import com.campusmarket.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConversationServiceTest {

    @Mock
    private AiConversationMapper conversationMapper;

    @Mock
    private AiMessageMapper messageMapper;

    @Mock
    private AiChatService aiChatService;

    @Test
    void shouldRemovePendingUserMessageWhenAiReplyIsBlank() {
        LoginUser loginUser = new LoginUser(1L, "bob");
        AiConversation conversation = new AiConversation();
        conversation.setId(9L);
        conversation.setUserId(1L);
        conversation.setTitle("新对话");

        when(conversationMapper.selectById(9L)).thenReturn(conversation);
        when(messageMapper.selectList(any())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            message.setId(88L);
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));
        when(aiChatService.chat(eq("hello"), eq(loginUser), isNull(), anyList()))
                .thenReturn("  ");

        ConversationMessageRequest request = new ConversationMessageRequest();
        request.setMessage("hello");
        AgentConversationService service = new AgentConversationService(
                conversationMapper, messageMapper, aiChatService);

        assertThrows(BusinessException.class,
                () -> service.sendMessage(9L, request, loginUser));
        verify(messageMapper).deleteById(88L);
    }
}
