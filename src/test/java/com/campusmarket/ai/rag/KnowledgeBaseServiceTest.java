package com.campusmarket.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeBaseServiceTest {

    @Test
    void shouldDegradeWhenEmbeddingResourcesAreUnavailable() {
        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        properties.setModelUri("file:/missing/model.onnx");
        properties.setTokenizerUri("file:/missing/tokenizer.json");

        KnowledgeBaseService service = new KnowledgeBaseService(properties);

        assertNull(service.retrieveRules("平台发布规则是什么？"));
    }

    @Test
    void shouldSkipRetrievalForNonRuleQuestion() {
        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        properties.setModelUri("file:/missing/model.onnx");
        properties.setTokenizerUri("file:/missing/tokenizer.json");

        KnowledgeBaseService service = new KnowledgeBaseService(properties);

        assertNull(service.retrieveRules("帮我找机械键盘"));
    }
}
