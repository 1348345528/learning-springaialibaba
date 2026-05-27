package com.example.chat.service;

import com.example.chat.entity.ReportEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Component
public class ReportGenerationTool
        implements Function<ReportGenerationTool.Request, ReportGenerationTool.Response> {

    private final ReportService reportService;

    // 存储最近一次生成的报表信息，供 RagChatService 提取
    private final AtomicReference<ReportInfo> latestReport = new AtomicReference<>();

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

        // 存储报表信息，供 RagChatService 提取
        ReportInfo reportInfo = new ReportInfo(report.getId(), report.getName(), url);
        latestReport.set(reportInfo);
        log.info("Report generated and stored: id={}, name={}", report.getId(), report.getName());

        return new Response(report.getId(), report.getName(), url);
    }

    /** 取出最近生成的报表信息并移除 */
    public ReportInfo pollReport() {
        ReportInfo info = latestReport.getAndSet(null);
        if (info != null) {
            log.info("Polling report: id={}, name={}", info.reportId(), info.reportName());
        }
        return info;
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
