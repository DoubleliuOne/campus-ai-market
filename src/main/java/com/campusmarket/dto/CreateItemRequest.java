package com.campusmarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "发布商品请求")
public class CreateItemRequest {

    @NotNull(message = "请选择商品分类")
    @Positive(message = "商品分类id必须为正数")
    @Schema(description = "商品分类id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题不能超过100个字符")
    @Schema(description = "商品标题", example = "九成新机械键盘",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 2000, message = "商品描述不能超过2000个字符")
    @Schema(description = "商品描述", example = "白色青轴，宿舍自提")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品价格最多8位整数和2位小数")
    @Schema(description = "商品价格，最多两位小数", example = "99.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Size(max = 5, message = "最多只能上传5张商品图片")
    @Schema(description = "商品图片URL列表，最多5张")
    private List<@Size(max = 500, message = "图片链接不能超过500个字符") String> images;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
