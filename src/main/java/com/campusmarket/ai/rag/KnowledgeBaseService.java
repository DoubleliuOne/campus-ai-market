package com.campusmarket.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final List<String> RULE_KEYWORDS = List.of(
            "规则", "能卖", "不能卖", "禁", "发布", "退款", "售后", "交易注意", "注意", "违规", "安全", "平台");

    private final VectorStore vectorStore;

    public KnowledgeBaseService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String retrieveRules(String question) {
        if (!isRuleQuestion(question)) {
            return null;
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .similarityThresholdAll()
                    .build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            if (documents.isEmpty()) {
                return null;
            }
            return documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isRuleQuestion(String question) {
        if (question == null) {
            return false;
        }
        return RULE_KEYWORDS.stream().anyMatch(question::contains);
    }
}
