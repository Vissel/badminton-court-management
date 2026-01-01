package com.badminton.controller;

import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import com.badminton.service.ExportService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
public class ManagerController {

    @Autowired
    ExportService exportService;

    @PostMapping("/reportList")
    public ResponseEntity<Result<PageResponse<ReportResponse>>> reportList(@RequestBody ReportListRequest rptListRequest) {
        return ResponseConvertor.convert(exportService.reportList(rptListRequest));
    }

    @PostMapping("/reportExport")
    public ResponseEntity<byte[]> exportReport(@RequestBody String sessionId) throws IOException {

        // Mock data (replace with DB query)
        List<ManagerReportDTO> data = List.of(
                new ManagerReportDTO(1L, LocalDate.now(), "08:00 - 10:00", 200_000),
                new ManagerReportDTO(2L, LocalDate.now(), "10:00 - 12:00", 350_000)
        );

        byte[] excelBytes = excelExportService.exportManagerReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(
                "attachment",
                "manager-report.xlsx"
        );

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
