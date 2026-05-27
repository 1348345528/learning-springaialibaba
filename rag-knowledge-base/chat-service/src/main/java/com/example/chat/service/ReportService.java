package com.example.chat.service;

import com.example.chat.entity.ReportEntity;
import com.example.chat.repository.jpa.ReportJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

@Service
@Slf4j
public class ReportService {

    private final ReportJpaRepository reportRepository;
    private final Random random = new Random();

    public ReportService(ReportJpaRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public ReportEntity generateReport(String insuranceType, LocalDate startDate, LocalDate endDate) {
        String reportName = String.format("%s报表-%s至%s",
                insuranceType,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        String htmlContent = buildHtml(insuranceType, startDate, endDate, reportName);

        ReportEntity entity = new ReportEntity();
        entity.setName(reportName);
        entity.setInsuranceType(insuranceType);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        entity.setHtmlContent(htmlContent);

        ReportEntity saved = reportRepository.save(entity);
        log.info("Generated report: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public ReportEntity getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found: " + id));
    }

    public String getReportHtml(Long id) {
        return getReportById(id).getHtmlContent();
    }

    private String buildHtml(String insuranceType, LocalDate startDate, LocalDate endDate, String reportName) {
        // 模拟数据
        double totalPremium = 100000 + random.nextDouble() * 900000;
        int totalPolicies = 500 + random.nextInt(2000);
        double totalClaims = totalPremium * (0.1 + random.nextDouble() * 0.3);
        double claimRate = totalClaims / totalPremium * 100;

        // 按月汇总数据
        StringBuilder monthRows = new StringBuilder();
        LocalDate current = startDate.withDayOfMonth(1);
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM", Locale.CHINA);

        while (!current.isAfter(endDate)) {
            double monthPremium = totalPremium / 3 * (0.8 + random.nextDouble() * 0.4);
            int monthPolicies = totalPolicies / 3 + random.nextInt(100) - 50;
            int monthClaims = (int) (monthPolicies * (0.05 + random.nextDouble() * 0.15));
            double monthClaimAmount = monthPremium * (0.08 + random.nextDouble() * 0.15);

            monthRows.append(String.format("""
                    <tr>
                        <td>%s</td>
                        <td>%.2f</td>
                        <td>%d</td>
                        <td>%d</td>
                        <td>%.2f</td>
                    </tr>
                    """,
                    current.format(monthFmt),
                    monthPremium,
                    monthPolicies,
                    monthClaims,
                    monthClaimAmount));

            current = current.plusMonths(1);
        }

        return String.format("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; padding: 24px; }
                        .container { max-width: 960px; margin: 0 auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 32px; }
                        h1 { font-size: 24px; color: #262626; margin-bottom: 8px; }
                        .subtitle { color: #8c8c8c; font-size: 14px; margin-bottom: 24px; }
                        .metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
                        .metric-card { background: #f0f5ff; border-radius: 8px; padding: 20px; text-align: center; }
                        .metric-card .value { font-size: 28px; font-weight: 600; color: #1890ff; margin-bottom: 4px; }
                        .metric-card .label { font-size: 14px; color: #8c8c8c; }
                        table { width: 100%%; border-collapse: collapse; }
                        th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #f0f0f0; }
                        th { background: #fafafa; font-weight: 500; color: #595959; }
                        td { color: #262626; }
                        tr:hover td { background: #f5f5f5; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>%s</h1>
                        <div class="subtitle">报表周期：%s 至 %s | 险种：%s</div>

                        <div class="metrics">
                            <div class="metric-card">
                                <div class="value">%.2f</div>
                                <div class="label">保费总额（元）</div>
                            </div>
                            <div class="metric-card">
                                <div class="value">%d</div>
                                <div class="label">保单数量</div>
                            </div>
                            <div class="metric-card">
                                <div class="value">%.2f</div>
                                <div class="label">理赔金额（元）</div>
                            </div>
                            <div class="metric-card">
                                <div class="value">%.1f%%</div>
                                <div class="label">理赔率</div>
                            </div>
                        </div>

                        <h2 style="font-size: 18px; margin-bottom: 16px;">按月汇总</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>月份</th>
                                    <th>保费收入（元）</th>
                                    <th>保单数</th>
                                    <th>理赔数</th>
                                    <th>理赔金额（元）</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>
                </body>
                </html>
                """,
                reportName,
                reportName,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                insuranceType,
                totalPremium,
                totalPolicies,
                totalClaims,
                claimRate,
                monthRows);
    }
}
