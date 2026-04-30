package com.badminton.service;

import com.badminton.model.report.ExportReportResult;
import com.badminton.requestmodel.ExportReportRequest;
import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;

public interface ExportService {
    Result<PageResponse<ReportResponse>> reportList(ReportListRequest rptListRequest);

    Result<ExportReportResult> exportReport(String sessionId);

    void streamingExportReportList(ExportReportRequest exportedRptRequest, OutputStream outputStream) throws IOException;

    String buildListReportFileName(ExportReportRequest request);

    void normalExportReportList(ExportReportRequest exportedRptRequest, HttpServletResponse response);
}
