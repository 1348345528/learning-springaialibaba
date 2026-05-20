package com.example.rag.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class HydeService {

    private final RestClient restClient;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${hyde.model:qwen-plus}")
    private String hydeModel;

    private static final String HYDE_PROMPT = """
            Write a short passage that answers the following question.
            Include relevant details, terminology, and context that would appear in a professional document.
            Output ONLY the passage content, no prefixes or explanations.

            Question: %s

            Passage:""";

    public HydeService() {
        this.restClient = RestClient.create();
    }

    public String generateHypotheticalDocument(String query) {
        String prompt = String.format(HYDE_PROMPT, query);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", hydeModel);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 512);

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        log.debug("Generating HyDE document for query: {}", query);

        String response = restClient.post()
                .uri("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody.toJSONString())
                .retrieve()
                .body(String.class);

        JSONObject resp = JSON.parseObject(response);
        List<Object> choices = resp.getJSONArray("choices").toJavaList(Object.class);
        if (choices.isEmpty()) {
            throw new RuntimeException("HyDE returned no choices");
        }
        JSONObject choice = (JSONObject) choices.get(0);
        return choice.getJSONObject("message").getString("content");
    }
}
