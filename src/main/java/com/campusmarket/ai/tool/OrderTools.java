package com.campusmarket.ai.tool;

import com.campusmarket.common.PageResult;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.OrderService;
import com.campusmarket.vo.OrderVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderTools(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "getMyOrders",
            description = "查询当前登录用户自己的订单。用户问自己买到的、下的订单时role传buyer；问自己卖出的订单时role传seller")
    public String getMyOrders(
            @ToolParam(required = false, description = "buyer或seller，默认buyer") String role,
            ToolContext toolContext) {
        if (!hasBudget(toolContext)) {
            return "工具调用次数已达上限，请根据已有结果继续回答。";
        }
        if (toolContext == null || toolContext.getContext() == null) {
            return "无法获取当前登录用户";
        }
        Object userIdObject = toolContext.getContext().get("userId");
        if (userIdObject == null) {
            return "无法获取当前登录用户";
        }

        try {
            Long userId = Long.valueOf(String.valueOf(userIdObject));
            PageResult<OrderVO> result = orderService.myOrders(new LoginUser(userId, null), role, 1, 20);
            return objectMapper.writeValueAsString(result);
        } catch (RuntimeException ex) {
            return "查询订单失败：" + ex.getMessage();
        } catch (JsonProcessingException ex) {
            return "订单结果序列化失败";
        }
    }

    private boolean hasBudget(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return true;
        }
        Object value = toolContext.getContext().get("toolCallBudget");
        return !(value instanceof ToolCallBudget budget) || budget.tryAcquire();
    }
}
