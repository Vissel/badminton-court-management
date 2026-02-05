package com.badminton.service;

import com.badminton.requestmodel.ExportReportRequest;
import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.OutputStream;

public interface ExportService {
    Result<PageResponse<ReportResponse>> reportList(ReportListRequest rptListRequest);

    Result<ByteArrayResource> exportReport(String sessionId);

    void streamingExportReportList(ExportReportRequest exportedRptRequest, OutputStream outputStream) throws IOException;

    void normalExportReportList(ExportReportRequest exportedRptRequest, HttpServletResponse response);
}
