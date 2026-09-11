package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "修改订单状态请求")
public class UpdateOrderStatusRequest {

    @NotBlank(message = "订单状态不能为空")
    @Pattern(regexp = "CANCELLED|COMPLETED", message = "订单状态只能是CANCELLED或COMPLETED")
    @Schema(description = "目标状态", allowableValues = {"CANCELLED", "COMPLETED"},
            example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
