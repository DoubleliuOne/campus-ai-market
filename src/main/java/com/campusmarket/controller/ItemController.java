package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateItemRequest;
import com.campusmarket.dto.UpdateItemRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.ItemService;
import com.campusmarket.vo.ItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/items")
@Tag(name = "商品", description = "商品发布、搜索、详情、修改与下架")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @Operation(summary = "发布商品")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Long> create(@Valid @RequestBody CreateItemRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(itemService.publish(request, loginUser));
    }

    @GetMapping("/mine")
    @Operation(summary = "查询我发布的商品")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResult<ItemVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                @Parameter(description = "商品状态")
                                                @RequestParam(required = false)
                                                @Pattern(regexp = "ON_SALE|SOLD|OFF_SHELF",
                                                        message = "商品状态只能是ON_SALE、SOLD或OFF_SHELF")
                                                String status,
                                                @Parameter(description = "页码，从1开始")
                                                @RequestParam(defaultValue = "1")
                                                @Min(value = 1, message = "页码不能小于1") long page,
                                                @Parameter(description = "每页数量，最大100")
                                                @RequestParam(defaultValue = "10")
                                                @Min(value = 1, message = "每页数量不能小于1")
                                                @Max(value = 100, message = "每页数量不能超过100") long size) {
        return ApiResponse.ok(itemService.myItems(loginUser, status, page, size));
    }

    @GetMapping
    @Operation(summary = "搜索在售商品")
    public ApiResponse<PageResult<ItemVO>> list(
            @Parameter(description = "标题或描述关键词")
            @RequestParam(required = false)
            @Size(max = 100, message = "搜索关键词不能超过100个字符") String keyword,
            @Parameter(description = "商品分类id")
            @RequestParam(required = false)
            @Positive(message = "商品分类id必须为正数") Long categoryId,
            @Parameter(description = "最低价格")
            @RequestParam(required = false)
            @DecimalMin(value = "0", message = "最低价格不能小于0") BigDecimal minPrice,
            @Parameter(description = "最高价格")
            @RequestParam(required = false)
            @DecimalMin(value = "0", message = "最高价格不能小于0") BigDecimal maxPrice,
            @Parameter(description = "排序：latest、priceAsc、priceDesc")
            @RequestParam(defaultValue = "latest")
            @Pattern(regexp = "latest|priceAsc|priceDesc",
                    message = "排序方式只能是latest、priceAsc或priceDesc") String sort,
            @Parameter(description = "页码，从1开始")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") long page,
            @Parameter(description = "每页数量，最大100")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能超过100") long size) {
        return ApiResponse.ok(itemService.search(
                keyword, categoryId, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/hot")
    @Operation(summary = "查询热门商品", description = "最多返回10件在售商品，优先读取 Redis 缓存")
    public ApiResponse<List<ItemVO>> hot() {
        return ApiResponse.ok(itemService.getHotItems());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询商品详情")
    public ApiResponse<ItemVO> detail(
            @PathVariable @Positive(message = "商品id必须为正数") Long id) {
        return ApiResponse.ok(itemService.getDetail(id));
    }

    @GetMapping("/{id}/manage")
    @Operation(summary = "查询自己发布的商品详情", description = "卖家可查看在售、已售出和已下架商品")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ItemVO> managedDetail(
            @PathVariable @Positive(message = "商品id必须为正数") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(itemService.getOwnedDetail(id, loginUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> update(@PathVariable @Positive(message = "商品id必须为正数") Long id,
                                    @Valid @RequestBody UpdateItemRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        itemService.update(id, request, loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "下架商品", description = "逻辑下架，将状态改为 OFF_SHELF")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> takeOffShelf(
            @PathVariable @Positive(message = "商品id必须为正数") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        itemService.takeOffShelf(id, loginUser);
        return ApiResponse.<Void>ok(null);
    }

    @PatchMapping("/{id}/relist")
    @Operation(summary = "重新上架商品", description = "仅允许卖家将 OFF_SHELF 商品恢复为 ON_SALE")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> relist(
            @PathVariable @Positive(message = "商品id必须为正数") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        itemService.relist(id, loginUser);
        return ApiResponse.<Void>ok(null);
    }
}
