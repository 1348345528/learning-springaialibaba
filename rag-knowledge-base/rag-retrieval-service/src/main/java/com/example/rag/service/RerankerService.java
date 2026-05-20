package com.example.rag.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RerankerService {

    private final RestClient restClient;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${reranker.model:gte-rerank}")
    private String rerankModel;

    public RerankerService() {
        this.restClient = RestClient.builder()
                .defaultStatusHandler(status -> status.value() >= 400, (request, response) -> {
                    String body = readBody(response);
                    log.error("Reranker API error: {}", body);
                    throw new RuntimeException("Reranker API error: " + body);
                })
                .build();
    }

    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", rerankModel);
        JSONObject input = new JSONObject();
        input.put("query", query);
        JSONArray docs = new JSONArray();
        docs.addAll(documents);
        input.put("documents", docs);
        requestBody.put("input", input);
        JSONObject params = new JSONObject();
        params.put("top_n", topN);
        params.put("return_documents", true);
        requestBody.put("parameters", params);

        log.debug("Calling DashScope reranker, model={}, documents={}, topN={}",
                rerankModel, documents.size(), topN);

        String response = restClient.post()
                .uri("https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody.toJSONString())
                .retrieve()
                .body(String.class);

        return parseResponse(response);
    }

    private List<RerankResult> parseResponse(String responseBody) {
        JSONObject resp = JSON.parseObject(responseBody);
        JSONArray results = resp.getJSONObject("output").getJSONArray("results");
        List<RerankResult> reranked = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject r = results.getJSONObject(i);
            reranked.add(new RerankResult(
                    r.getInteger("index"),
                    r.getString("document"),
                    r.getDouble("relevance_score").floatValue()
            ));
        }
        return reranked;
    }

    private static String readBody(ClientHttpResponse response) {
        try {
            return new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    public record RerankResult(int index, String document, float score) {}
}
