package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateItemRequest;
import com.campusmarket.dto.UpdateItemRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.ItemService;
import com.campusmarket.vo.ItemVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody CreateItemRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(itemService.publish(request, loginUser));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<ItemVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(itemService.myItems(loginUser, status, page, size));
    }

    @GetMapping
    public ApiResponse<PageResult<ItemVO>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(itemService.search(keyword, categoryId, minPrice, maxPrice, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(itemService.getDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody UpdateItemRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        itemService.update(id, request, loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> takeOffShelf(@PathVariable Long id,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        itemService.takeOffShelf(id, loginUser);
        return ApiResponse.<Void>ok(null);
    }
}
