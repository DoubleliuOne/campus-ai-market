package com.campusmarket.controller;

import com.campusmarket.ai.service.AiChatService;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.dto.AgentChatRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.AgentChatResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/agent")
@Tag(name = "AI 助手", description = "DeepSeek Tool Calling 与交易规则 RAG 问答")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AiChatService aiChatService;

    public AgentController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 助手对话", description = "可携带 itemId 让 AI 结合指定商品上下文回答")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(new AgentChatResponse(
                aiChatService.chat(request.getMessage(), loginUser, request.getItemId())));
    }
}
