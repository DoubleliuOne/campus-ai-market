package com.campusmarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "图片上传结果")
public class UploadFileVO {

    @Schema(description = "可直接保存到商品 images 字段的访问地址",
            example = "/api/files/images/7f0c1cba4d0f4f169834cb50a1f56a0d.jpg")
    private String url;

    @Schema(description = "原始文件名", example = "keyboard.jpg")
    private String originalName;

    @Schema(description = "文件字节数", example = "248120")
    private Long size;

    public UploadFileVO() {
    }

    public UploadFileVO(String url, String originalName, Long size) {
        this.url = url;
        this.originalName = originalName;
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
