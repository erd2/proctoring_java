package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.ReportRequest;
import com.aiu.proctoring.application.port.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Report generation service (supports JSON and CSV; PDF via JasperReports future).
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    // Simple in-memory storage for generated reports (replace with S3/DB in production)
    private final Map<String, byte[]> reportStore = new ConcurrentHashMap<>();

    @Override
    public String generateReport(ReportRequest request) {
        String reportId = UUID.randomUUID().toString();

        // Async generation (simplified: synchronous for now)
        try {
            byte[] report = generateReportInFormat(request);
            reportStore.put(reportId, report);
            log.info("Report generated: {} for proctor {}", reportId, request.getProctorId());
            return reportId;
        } catch (Exception e) {
            log.error("Report generation failed", e);
            throw new RuntimeException("Failed to generate report");
        }
    }

    @Override
    public byte[] getReport(String reportId) {
        byte[] report = reportStore.get(reportId);
        if (report == null) {
            throw new RuntimeException("Report not found");
        }
        return report;
    }

    @Override
    public List<String> listAvailableReports() {
        return reportStore.keySet().stream().toList();
    }

    @Override
    public void deleteReport(String reportId) {
        reportStore.remove(reportId);
    }

    private byte[] generateReportInFormat(ReportRequest request) throws Exception {
        String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "json";

        if ("json".equals(format)) {
            return generateJsonReport(request);
        } else if ("pdf".equals(format)) {
            return generatePdfReport(request);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private byte[] generateJsonReport(ReportRequest request) throws Exception {
        Map<String, Object> report = new HashMap<>();
        report.put("reportId", UUID.randomUUID());
        report.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("proctorId", request.getProctorId());
        report.put("period", Map.of(
            "start", request.getStartDate(),
            "end", request.getEndDate()
        ));
        report.put("violations", List.of()); // would fetch from DB

        String json = new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(report);

        return json.getBytes();
    }

    private byte[] generatePdfReport(ReportRequest request) throws Exception {
        // Simplified: In production, use iText or JasperReports
        String text = "Proctoring Report\n" +
            "==================\n" +
            "Proctor: " + request.getProctorId() + "\n" +
            "Period: " + request.getStartDate() + " - " + request.getEndDate() + "\n" +
            "Total violations: 0\n";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(text.getBytes());
        return baos.toByteArray();
    }
}
