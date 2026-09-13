package com.campusmarket.ai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private boolean enabled = true;
    private String modelUri;
    private String tokenizerUri;
    private String cacheDirectory = "./.model-cache";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelUri() {
        return modelUri;
    }

    public void setModelUri(String modelUri) {
        this.modelUri = modelUri;
    }

    public String getTokenizerUri() {
        return tokenizerUri;
    }

    public void setTokenizerUri(String tokenizerUri) {
        this.tokenizerUri = tokenizerUri;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }
}
