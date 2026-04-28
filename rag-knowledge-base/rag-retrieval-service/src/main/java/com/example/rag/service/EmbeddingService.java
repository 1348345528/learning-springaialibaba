package com.example.rag.service;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class EmbeddingService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.embedding.model}")
    private String model;

    @PostConstruct
    public void init() {
        Constants.apiKey = apiKey;
        log.info("DashScope API Key configured successfully");
    }

    public float[] embed(String text) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model)
                    .texts(Collections.singletonList(text))
                    .build();

            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            List<TextEmbeddingResultItem> embeddings = result.getOutput().getEmbeddings();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("No embeddings returned from DashScope");
            }

            List<Double> embedding = embeddings.get(0).getEmbedding();
            float[] floatArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                floatArray[i] = embedding.get(i).floatValue();
            }

            log.debug("Generated embedding for text: {} (dimension: {})", text.substring(0, Math.min(50, text.length())), floatArray.length);
            return floatArray;

        } catch (NoApiKeyException e) {
            log.error("DashScope API Key not configured", e);
            throw new RuntimeException("DashScope API Key not configured", e);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public List<float[]> embed(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }
}
