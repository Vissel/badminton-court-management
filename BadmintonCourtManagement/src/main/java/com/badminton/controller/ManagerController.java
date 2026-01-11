package com.badminton.controller;

import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.MonthYearResponse;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import com.badminton.service.DateTimeService;
import com.badminton.service.ExportService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
public class ManagerController {

    @Autowired
    ExportService exportService;
    @Autowired
    DateTimeService dateTimeService;

    @PostMapping("/reportList")
    public ResponseEntity<Result<PageResponse<ReportResponse>>> reportList(@RequestBody ReportListRequest rptListRequest) {
        return ResponseConvertor.convert(exportService.reportList(rptListRequest));
    }

    @GetMapping("/reportExport/{sessionId}")
    public ResponseEntity<Resource> exportReport(@PathVariable String sessionId) {
        byte[] excelBytes = exportService.exportReport(sessionId);
        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(
                "attachment",
                "manager-report.xlsx"
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report_" + sessionId + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/getMonthYear")
    public ResponseEntity<List<MonthYearResponse>> getMonthYear() {
        return new ResponseEntity<>(dateTimeService.getMonthYearList(), HttpStatus.OK);
    }
}
