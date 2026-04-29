package com.example.mcpserver;

import com.example.mcpserver.service.CalculatorToolService;
import com.example.mcpserver.service.UserToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerExampleApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider userTools(UserToolService userToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(userToolService)
                .build();
    }

    @Bean
    public ToolCallbackProvider calculatorTools(CalculatorToolService calculatorToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculatorToolService)
                .build();
    }
}
