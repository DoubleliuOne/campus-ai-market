package com.campusmarket.ai.tool;

import com.campusmarket.common.PageResult;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.ItemService;
import com.campusmarket.vo.ItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ItemTools {

    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    public ItemTools(ItemService itemService, ObjectMapper objectMapper) {
        this.itemService = itemService;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "searchItems",
            description = "按关键词、分类、价格区间搜索当前正在出售的商品，返回分页商品列表")
    public String searchItems(
            @ToolParam(required = false, description = "搜索关键词，例如手机、机械键盘、教材") String keyword,
            @ToolParam(required = false, description = "商品分类id") Long categoryId,
            @ToolParam(required = false, description = "最低价格") Double minPrice,
            @ToolParam(required = false, description = "最高价格") Double maxPrice,
            @ToolParam(required = false, description = "页码，默认1") Integer page,
            @ToolParam(required = false, description = "每页数量，默认5") Integer size,
            ToolContext toolContext) {
        if (!hasBudget(toolContext)) {
            return "工具调用次数已达上限，请根据已有结果继续回答。";
        }
        try {
            int currentPage = page == null || page < 1 ? 1 : page;
            int pageSize = size == null || size < 1 ? 5 : Math.min(size, 20);

            PageResult<ItemVO> result = itemService.search(
                    keyword,
                    categoryId,
                    minPrice == null ? null : BigDecimal.valueOf(minPrice),
                    maxPrice == null ? null : BigDecimal.valueOf(maxPrice),
                    "latest",
                    currentPage,
                    pageSize);
            return objectMapper.writeValueAsString(result);
        } catch (BusinessException ex) {
            return "查询失败：" + ex.getMessage();
        } catch (JsonProcessingException ex) {
            return "查询结果序列化失败";
        } catch (RuntimeException ex) {
            return "商品搜索暂时不可用，请稍后重试。";
        }
    }

    @Tool(name = "getItemDetail",
            description = "按商品id查询当前正在出售的商品详情")
    public String getItemDetail(
            @ToolParam(required = true, description = "商品id") Long itemId,
            ToolContext toolContext) {
        if (!hasBudget(toolContext)) {
            return "工具调用次数已达上限，请根据已有结果继续回答。";
        }
        try {
            LoginUser loginUser = currentUser(toolContext);
            ItemVO detail = itemService.getDetailForUser(itemId, loginUser);
            return objectMapper.writeValueAsString(detail);
        } catch (BusinessException ex) {
            return "查询失败：" + ex.getMessage();
        } catch (JsonProcessingException ex) {
            return "商品详情序列化失败";
        } catch (RuntimeException ex) {
            return "商品详情暂时不可用，请稍后重试。";
        }
    }

    @Tool(name = "recommendItems",
            description = "根据用户想要的商品类型和预算推荐在售商品。会真实搜索平台商品，结果交给AI分析推荐")
    public String recommendItems(
            @ToolParam(required = false, description = "用户想要的商品类型，例如机械键盘、安卓手机、教材") String keyword,
            @ToolParam(required = false, description = "商品分类id") Long categoryId,
            @ToolParam(required = false, description = "用户能接受的最高价格") Double maxPrice,
            @ToolParam(required = false, description = "其他推荐说明") String note,
            ToolContext toolContext) {
        return searchItems(keyword, categoryId, null, maxPrice, 1, 10, toolContext);
    }

    private boolean hasBudget(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return true;
        }
        Object value = toolContext.getContext().get("toolCallBudget");
        return !(value instanceof ToolCallBudget budget) || budget.tryAcquire();
    }

    private LoginUser currentUser(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object userId = toolContext.getContext().get("userId");
        if (userId == null) {
            return null;
        }
        try {
            return new LoginUser(Long.valueOf(String.valueOf(userId)), null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
