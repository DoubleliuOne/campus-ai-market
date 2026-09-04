package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateOrderRequest;
import com.campusmarket.dto.UpdateOrderStatusRequest;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.OrderService;
import com.campusmarket.vo.OrderVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody CreateOrderRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(orderService.create(request, loginUser));
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<OrderVO>> myOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam(defaultValue = "buyer") String role,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.myOrders(loginUser, role, page, size));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody UpdateOrderStatusRequest request,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        orderService.updateStatus(id, request, loginUser);
        return ApiResponse.<Void>ok(null);
    }
}
