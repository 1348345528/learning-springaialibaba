package com.example.chat.config;

import com.example.chat.service.CalculatorTool;
import com.example.chat.service.DateTimeTool;
import com.example.chat.service.DocStatsTool;
import com.example.chat.service.RagRetrievalTool;
import com.example.chat.service.ReportGenerationTool;
import com.example.chat.service.ReportService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ReactAgentConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public ToolCallback ragRetrievalCallback(WebClient webClient) {
        RagRetrievalTool tool = new RagRetrievalTool(webClient);
        return FunctionToolCallback
                .builder("search_knowledge_base", tool)
                .description("""
                        从知识库中检索与查询相关的文档块。
                        当你需要查找某个主题的相关信息、背景知识或文档内容时使用此工具。
                        参数 query: 要检索的问题或关键词。
                        参数 topK: 返回结果数量（默认5）。
                        返回: 相关知识块及其来源文档和相关性分数。
                        """)
                .inputType(RagRetrievalTool.Request.class)
                .build();
    }

    @Bean
    public ToolCallback dateTimeCallback() {
        DateTimeTool tool = new DateTimeTool();
        return FunctionToolCallback
                .builder("get_current_time", tool)
                .description("""
                        获取当前日期和时间。当用户问"现在几点"、"今天几号"等时间相关问题时使用。
                        参数 timezone: 时区（如 Asia/Shanghai、America/New_York、Europe/London、UTC）。
                        不传参数默认为 Asia/Shanghai。
                        返回: 格式化后的日期时间字符串。
                        """)
                .inputType(DateTimeTool.Request.class)
                .build();
    }

    @Bean
    public ToolCallback calculatorCallback() {
        CalculatorTool tool = new CalculatorTool();
        return FunctionToolCallback
                .builder("calculate", tool)
                .description("""
                        执行数学表达式计算。当用户需要进行数学运算时使用此工具。
                        参数 expression: 数学表达式（如 "2+3*4"、"100/7"、"sqrt(9)"）。
                        支持加减乘除、括号、百分比运算。
                        返回: 计算结果。
                        """)
                .inputType(CalculatorTool.Request.class)
                .build();
    }

    @Bean
    public ToolCallback docStatsCallback(WebClient webClient) {
        DocStatsTool tool = new DocStatsTool(webClient);
        return FunctionToolCallback
                .builder("get_document_stats", tool)
                .description("""
                        查询知识库中的文档统计信息。当用户问"知识库有多少文档"、"有哪些文档"时使用。
                        无需参数。
                        返回: 文档数量、文档名称列表、分块总数。
                        """)
                .inputType(DocStatsTool.Request.class)
                .build();
    }

    @Bean
    public ToolCallback reportGenerationCallback(ReportGenerationTool reportGenerationTool) {
        return FunctionToolCallback
                .builder("generate_report", reportGenerationTool)
                .description("""
                        生成保险报表。当用户要求生成报表、查看报表、统计保险数据时调用。
                        参数说明：
                        - insuranceType: 险种，可选值：车险/健康险/寿险/意外险/财产险
                        - startDate: 起始日期，格式 yyyy-MM-dd
                        - endDate: 截止日期，格式 yyyy-MM-dd
                        """)
                .inputType(ReportGenerationTool.Request.class)
                .build();
    }
}
