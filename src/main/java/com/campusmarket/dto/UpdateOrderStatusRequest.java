package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "修改订单状态请求")
public class UpdateOrderStatusRequest {

    @NotBlank(message = "订单状态不能为空")
    @Pattern(regexp = "CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED",
            message = "订单状态只能是CONFIRMED、IN_PROGRESS、COMPLETED或CANCELLED")
    @Schema(description = "目标状态",
            allowableValues = {"CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED"},
            example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
