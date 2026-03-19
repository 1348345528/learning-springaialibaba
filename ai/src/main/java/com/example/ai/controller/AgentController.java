package com.example.ai.controller;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*") // 允许跨域
public class AgentController {

    private final ReactAgent minimaxAgent;
    private final ReactAgent openaiAgent;

    public AgentController(@Qualifier("miniMaxAgent")ReactAgent minimaxAgent, @Qualifier("openAiAgent")ReactAgent openaiAgent) {
        this.minimaxAgent = minimaxAgent;
        this.openaiAgent = openaiAgent;
    }

    /**
     * 流式对话接口
     */
    /**
     * 流式对话接口 - 支持区分思考过程和最终答案
     */
    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody Map<String, String> request) throws GraphRunnerException {
        String message = request.get("message");

        return minimaxAgent.stream(message)
                .flatMap(output -> {
                    if (output instanceof StreamingOutput streamingOutput) {
                        OutputType type = streamingOutput.getOutputType();

                        // 思考过程：工具调用/Hook执行
                        if (type == OutputType.AGENT_TOOL_FINISHED) {
                            return Flux.just("[THINKING]🔧 " + output.node() + "\n");
                        }
                        if (type == OutputType.AGENT_HOOK_FINISHED) {
                            return Flux.just("[THINKING]⚡ " + output.node() + "\n");
                        }

                        // 模型推理流式输出
                        if (type == OutputType.AGENT_MODEL_STREAMING) {
                            String text = streamingOutput.message().getText();
                            return Flux.just(text);
                        }

                        // 模型推理完成 - 开始答案
                        if (type == OutputType.AGENT_MODEL_FINISHED) {
                            return Flux.just("\n[ANSWER]💬 ");
                        }
                    }
                    return Flux.empty();
                })
                .concatWith(Flux.just("[DONE]"))
                .onErrorResume(e -> Flux.just("[ERROR]❌ " + e.getMessage() + "\n[DONE]"));
    }
    
}
