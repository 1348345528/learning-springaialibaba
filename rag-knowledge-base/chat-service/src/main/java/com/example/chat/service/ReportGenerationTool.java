package com.example.chat.service;

import com.example.chat.entity.ReportEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class ReportGenerationTool
        implements Function<ReportGenerationTool.Request, ReportGenerationTool.Response> {

    private final ReportService reportService;

    // 存储每个会话最近一次生成的报表信息，供 RagChatService 提取
    private final Map<String, ReportInfo> pendingReports = new ConcurrentHashMap<>();

    // ThreadLocal 用于在 apply 方法中获取当前会话 ID
    private static final ThreadLocal<String> currentConversationId = new ThreadLocal<>();

    public ReportGenerationTool(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public Response apply(Request request) {
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = LocalDate.parse(request.endDate());

        ReportEntity report = reportService.generateReport(
                request.insuranceType(), startDate, endDate);

        String url = "/api/reports/" + report.getId() + "/html";

        // 存储报表信息到 pendingReports，供 RagChatService 提取
        String conversationId = currentConversationId.get();
        if (conversationId != null) {
            pendingReports.put(conversationId, new ReportInfo(report.getId(), report.getName(), url));
        }

        return new Response(report.getId(), report.getName(), url);
    }

    /** 设置当前会话 ID（在调用 agent.stream 之前调用） */
    public void setCurrentConversationId(String conversationId) {
        currentConversationId.set(conversationId);
    }

    /** 清除当前会话 ID */
    public void clearCurrentConversationId() {
        currentConversationId.remove();
    }

    /** 取出会话的报表信息并移除 */
    public ReportInfo pollReport(String conversationId) {
        return pendingReports.remove(conversationId);
    }

    public record Request(
            @JsonProperty(required = true) String insuranceType,
            @JsonProperty(required = true) String startDate,
            @JsonProperty(required = true) String endDate
    ) {}

    public record Response(
            Long reportId,
            String reportName,
            String url
    ) {}

    public record ReportInfo(Long reportId, String reportName, String url) {}
}
