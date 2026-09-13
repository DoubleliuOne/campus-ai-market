package com.campusmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String uploadDir = "./uploads";
    private long maxImageSize = 5 * 1024 * 1024L;
    private long maxImageCount = 5L;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public long getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(long maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public long getMaxImageCount() {
        return maxImageCount;
    }

    public void setMaxImageCount(long maxImageCount) {
        this.maxImageCount = maxImageCount;
    }
}
