package com.campusmarket.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final List<String> RULE_KEYWORDS = List.of(
            "规则", "能卖", "不能卖", "禁", "发布", "退款", "售后", "交易注意", "注意", "违规", "安全", "平台");

    private final RagProperties properties;
    private volatile VectorStore vectorStore;
    private volatile boolean initialized;

    public KnowledgeBaseService(RagProperties properties) {
        this.properties = properties;
    }

    public String retrieveRules(String question) {
        if (!properties.isEnabled() || !isRuleQuestion(question)) {
            return null;
        }

        try {
            VectorStore store = getOrCreateVectorStore();
            if (store == null) {
                return null;
            }
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .similarityThresholdAll()
                    .build();
            List<Document> documents = store.similaritySearch(searchRequest);
            if (documents.isEmpty()) {
                return null;
            }
            return documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
        } catch (RuntimeException ex) {
            log.warn("RAG rule retrieval unavailable: {}", ex.getMessage());
            return null;
        }
    }

    private VectorStore getOrCreateVectorStore() {
        if (initialized) {
            return vectorStore;
        }
        synchronized (this) {
            if (initialized) {
                return vectorStore;
            }
            try {
                if (!StringUtils.hasText(properties.getModelUri())
                        || !StringUtils.hasText(properties.getTokenizerUri())) {
                    log.warn("RAG is enabled but model URIs are not configured; skipping rule retrieval");
                    return null;
                }

                TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
                embeddingModel.setDisableCaching(false);
                embeddingModel.setResourceCacheDirectory(properties.getCacheDirectory());
                embeddingModel.setModelResource(properties.getModelUri());
                embeddingModel.setTokenizerResource(properties.getTokenizerUri());
                embeddingModel.afterPropertiesSet();

                SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
                store.add(loadRuleDocuments());
                vectorStore = store;
                log.info("RAG rule vector store initialized");
            } catch (Exception ex) {
                log.warn("RAG initialization failed; AI will continue without rule retrieval: {}",
                        ex.getMessage());
            } finally {
                initialized = true;
            }
            return vectorStore;
        }
    }

    private List<Document> loadRuleDocuments() throws IOException {
        ClassPathResource resource = new ClassPathResource("knowledge/trading-rules.txt");
        byte[] bytes;
        try (InputStream inputStream = resource.getInputStream()) {
            bytes = inputStream.readAllBytes();
        }

        String content = new String(bytes, StandardCharsets.UTF_8);
        String[] sections = content.split("\\n(?=## )");
        List<Document> documents = new ArrayList<>();
        for (String section : sections) {
            if (!section.isBlank()) {
                documents.add(new Document(section.trim(), Map.of("source", "campus-trading-rules")));
            }
        }
        return documents;
    }

    private boolean isRuleQuestion(String question) {
        if (question == null) {
            return false;
        }
        return RULE_KEYWORDS.stream().anyMatch(question::contains);
    }
}
