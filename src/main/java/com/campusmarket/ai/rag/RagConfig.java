package com.campusmarket.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class RagConfig {

    @Bean
    public SimpleVectorStore ruleVectorStore(EmbeddingModel embeddingModel) {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge/trading-rules.txt");
            byte[] bytes;
            try (InputStream inputStream = resource.getInputStream()) {
                bytes = inputStream.readAllBytes();
            }

            String content = new String(bytes, StandardCharsets.UTF_8);
            String[] sections = content.split("\\n(?=## )");
            List<Document> documents = new ArrayList<>();
            for (String section : sections) {
                if (section.isBlank()) {
                    continue;
                }
                documents.add(new Document(section.trim(), Map.of("source", "campus-trading-rules")));
            }

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            vectorStore.add(documents);
            return vectorStore;
        } catch (IOException ex) {
            throw new IllegalStateException("校园交易规则文档加载失败", ex);
        }
    }
}
