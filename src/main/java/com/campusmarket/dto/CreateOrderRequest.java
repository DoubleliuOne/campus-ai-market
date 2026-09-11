package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotNull(message = "商品id不能为空")
    @Positive(message = "商品id必须为正数")
    @Schema(description = "要购买的商品id", example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long itemId;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
