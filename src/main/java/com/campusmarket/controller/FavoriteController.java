package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.FavoriteRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.FavoriteService;
import com.campusmarket.vo.ItemVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody FavoriteRequest request,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        favoriteService.add(request.getItemId(), loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> remove(@PathVariable Long itemId,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        favoriteService.remove(itemId, loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @GetMapping
    public ApiResponse<PageResult<ItemVO>> myFavorites(@AuthenticationPrincipal LoginUser loginUser,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(favoriteService.myFavorites(loginUser, page, size));
    }
}
