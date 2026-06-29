package com.example.chat.controller;

import com.example.chat.dto.ChatRequest;
import com.example.chat.service.GraphAgentChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/graph-agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GraphAgentController {

    private final GraphAgentChatService graphAgentChatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        return graphAgentChatService.chatStream(request);
    }
}
