package com.badminton.service;

import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;

public interface ExportService {
    Result<PageResponse<ReportResponse>> reportList(ReportListRequest rptListRequest);

    byte[] exportReport(String sessionId);
}
