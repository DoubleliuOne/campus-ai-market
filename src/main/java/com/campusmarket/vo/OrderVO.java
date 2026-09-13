package com.campusmarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单id", example = "10")
    private Long id;

    @Schema(description = "商品id", example = "5")
    private Long itemId;

    @Schema(description = "商品标题", example = "九成新机械键盘")
    private String itemTitle;

    @Schema(description = "商品图片URL列表")
    private List<String> images;

    @Schema(description = "订单价格", example = "99.00")
    private BigDecimal price;

    @Schema(description = "订单状态",
            allowableValues = {"CREATED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED"},
            example = "CREATED")
    private String status;

    @Schema(description = "买家id", example = "2")
    private Long buyerId;

    @Schema(description = "买家用户名", example = "buyer1")
    private String buyerUsername;

    @Schema(description = "卖家id", example = "1")
    private Long sellerId;

    @Schema(description = "卖家用户名", example = "bob")
    private String sellerUsername;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public void setBuyerUsername(String buyerUsername) {
        this.buyerUsername = buyerUsername;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
