package com.campusmarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "商品信息")
public class ItemVO {

    @Schema(description = "商品id", example = "5")
    private Long id;

    @Schema(description = "卖家用户id", example = "1")
    private Long sellerId;

    @Schema(description = "卖家用户名", example = "bob")
    private String sellerUsername;

    @Schema(description = "分类id", example = "1")
    private Long categoryId;

    @Schema(description = "分类名称", example = "数码产品")
    private String categoryName;

    @Schema(description = "标题", example = "九成新机械键盘")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "价格", example = "99.00")
    private BigDecimal price;

    @Schema(description = "图片URL列表")
    private List<String> images;

    @Schema(description = "商品状态", allowableValues = {"ON_SALE", "SOLD", "OFF_SHELF"},
            example = "ON_SALE")
    private String status;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
