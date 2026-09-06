package com.campusmarket.controller;

import com.campusmarket.ai.service.AiChatService;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.dto.AgentChatRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.AgentChatResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AiChatService aiChatService;

    public AgentController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@RequestBody AgentChatRequest request,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(new AgentChatResponse(
                aiChatService.chat(request.getMessage(), loginUser, request.getItemId())));
    }
}
