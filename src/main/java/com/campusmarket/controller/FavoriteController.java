package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.FavoriteRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.FavoriteService;
import com.campusmarket.vo.ItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "收藏商品与收藏列表")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    @Operation(summary = "添加收藏")
    public ApiResponse<Void> add(@Valid @RequestBody FavoriteRequest request,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        favoriteService.add(request.getItemId(), loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @GetMapping("/check/{itemId}")
    @Operation(summary = "检查是否已收藏")
    public ApiResponse<Boolean> check(@PathVariable @Positive(message = "商品id必须为正数") Long itemId,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(favoriteService.isFavorite(itemId, loginUser));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "取消收藏")
    public ApiResponse<Void> remove(@PathVariable @Positive(message = "商品id必须为正数") Long itemId,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        favoriteService.remove(itemId, loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @GetMapping
    @Operation(summary = "查询我的收藏")
    public ApiResponse<PageResult<ItemVO>> myFavorites(@AuthenticationPrincipal LoginUser loginUser,
                                                       @Parameter(description = "页码，从1开始")
                                                       @RequestParam(defaultValue = "1")
                                                       @Min(value = 1, message = "页码不能小于1") long page,
                                                       @Parameter(description = "每页数量，最大100")
                                                       @RequestParam(defaultValue = "10")
                                                       @Min(value = 1, message = "每页数量不能小于1")
                                                       @Max(value = 100, message = "每页数量不能超过100") long size) {
        return ApiResponse.ok(favoriteService.myFavorites(loginUser, page, size));
    }
}
