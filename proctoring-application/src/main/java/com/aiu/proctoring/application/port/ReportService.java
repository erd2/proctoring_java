package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.ReportRequest;

import java.util.List;
import java.util.Map;

/**
 * Port for report generation service.
 */
public interface ReportService {
    String generateReport(ReportRequest request);
    byte[] getReport(String reportId);
    List<String> listAvailableReports();
    void deleteReport(String reportId);
}
