package com.campusmarket.controller;

import com.campusmarket.ai.service.AiChatService;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.AgentChatRequest;
import com.campusmarket.dto.ConversationMessageRequest;
import com.campusmarket.dto.CreateConversationRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.AgentConversationService;
import com.campusmarket.vo.AgentChatResponse;
import com.campusmarket.vo.ConversationMessageVO;
import com.campusmarket.vo.ConversationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/agent")
@Tag(name = "AI 助手", description = "DeepSeek Tool Calling 与交易规则 RAG 问答")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AiChatService aiChatService;
    private final AgentConversationService agentConversationService;

    public AgentController(AiChatService aiChatService,
                           AgentConversationService agentConversationService) {
        this.aiChatService = aiChatService;
        this.agentConversationService = agentConversationService;
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 助手对话", description = "可携带 itemId 让 AI 结合指定商品上下文回答")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(new AgentChatResponse(
                aiChatService.chat(request.getMessage(), loginUser, request.getItemId())));
    }

    @GetMapping("/conversations")
    @Operation(summary = "查询我的 AI 会话")
    public ApiResponse<PageResult<ConversationVO>> conversations(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") long page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能超过100") long size) {
        return ApiResponse.ok(agentConversationService.list(loginUser, page, size));
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建 AI 会话")
    public ApiResponse<ConversationVO> createConversation(
            @Valid @RequestBody(required = false) CreateConversationRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(agentConversationService.create(request, loginUser));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "查询会话消息")
    public ApiResponse<List<ConversationMessageVO>> conversationMessages(
            @PathVariable @Positive(message = "会话id必须为正数") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(agentConversationService.messages(id, loginUser));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "发送会话消息", description = "保存用户消息与 AI 回答，并携带最近上下文")
    public ApiResponse<AgentChatResponse> sendConversationMessage(
            @PathVariable @Positive(message = "会话id必须为正数") Long id,
            @Valid @RequestBody ConversationMessageRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(agentConversationService.sendMessage(id, request, loginUser));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除 AI 会话")
    public ApiResponse<Void> deleteConversation(
            @PathVariable @Positive(message = "会话id必须为正数") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        agentConversationService.delete(id, loginUser);
        return ApiResponse.<Void>ok(null);
    }
}
