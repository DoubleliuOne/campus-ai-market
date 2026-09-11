package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateOrderRequest;
import com.campusmarket.dto.UpdateOrderStatusRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.OrderService;
import com.campusmarket.vo.OrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/orders")
@Tag(name = "订单", description = "创建订单、订单查询与状态流转")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "通过条件更新防止并发超卖")
    public ApiResponse<Long> create(@Valid @RequestBody CreateOrderRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(orderService.create(request, loginUser));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的订单")
    public ApiResponse<PageResult<OrderVO>> myOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Parameter(description = "查询视角：buyer 或 seller")
                                                     @RequestParam(defaultValue = "buyer")
                                                     @Pattern(regexp = "buyer|seller",
                                                             message = "role参数只能是buyer或seller") String role,
                                                     @Parameter(description = "页码，从1开始")
                                                     @RequestParam(defaultValue = "1")
                                                     @Min(value = 1, message = "页码不能小于1") long page,
                                                     @Parameter(description = "每页数量，最大100")
                                                     @RequestParam(defaultValue = "10")
                                                     @Min(value = 1, message = "每页数量不能小于1")
                                                     @Max(value = 100, message = "每页数量不能超过100") long size) {
        return ApiResponse.ok(orderService.myOrders(loginUser, role, page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改订单状态", description = "买家可取消，卖家可完成")
    public ApiResponse<Void> updateStatus(
            @PathVariable @Positive(message = "订单id必须为正数") Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        orderService.updateStatus(id, request, loginUser);
        return ApiResponse.<Void>ok(null);
    }
}
