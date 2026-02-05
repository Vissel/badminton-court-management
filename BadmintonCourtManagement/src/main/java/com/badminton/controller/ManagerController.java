package com.badminton.controller;

import com.badminton.requestmodel.ExportReportRequest;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
public class ManagerController {

    @Autowired
    ExportService exportService;
    @Autowired
    DateTimeService dateTimeService;
    // Temporary storage for session ID lists (In production, use Redis with TTL)
    private final Map<String, ExportReportRequest> exportCache = new ConcurrentHashMap<>();

    @PostMapping("/reportList")
    public ResponseEntity<Result<PageResponse<ReportResponse>>> reportList(@RequestBody ReportListRequest rptListRequest) {
        return ResponseConvertor.convert(exportService.reportList(rptListRequest));
    }

    @GetMapping("/reportExport/{sessionId}")
    public ResponseEntity<? extends Resource> exportReport(@PathVariable String sessionId) {
        Result<ByteArrayResource> resourceResult = exportService.exportReport(sessionId);
        return ResponseConvertor.convertToResource(resourceResult);
    }

    @GetMapping("/stream/reportExportList/{token}")
    public ResponseEntity<StreamingResponseBody> exportReportList(@PathVariable String token
    ) {
        com.badminton.requestmodel.ExportReportRequest exportedRptRequest = exportCache.remove(token); // Get and clear
        if (exportedRptRequest == null) return ResponseEntity.notFound().build();

        StreamingResponseBody responseBody = outputStream -> {
            try {
                exportService.streamingExportReportList(exportedRptRequest, outputStream);
            } catch (IOException e) {
//                exportService.normalExportReportList(exportedRptRequest, outputStream);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @GetMapping("/getMonthYear")
    public ResponseEntity<List<MonthYearResponse>> getMonthYear() {
        return new ResponseEntity<>(dateTimeService.getMonthYearList(), HttpStatus.OK);
    }


    // STEP 1: Receive the long list of IDs
    @PostMapping("/reportToken")
    public ResponseEntity<Map<String, String>> prepareExport(@RequestBody ExportReportRequest exportedRptRequest) {
        String downloadToken = UUID.randomUUID().toString();
        exportCache.put(downloadToken, exportedRptRequest);

        // Return the token to the frontend
        return ResponseEntity.ok(Map.of("reportToken", downloadToken));
    }

    // STEP 2: Trigger the Native Streaming Download
    @GetMapping("/download/{token}")
    public ResponseEntity<StreamingResponseBody> downloadZip(@PathVariable String token) {
        List<Integer> sessionIds = exportCache.remove(token).getSessionIds(); // Get and clear
        if (sessionIds == null) return ResponseEntity.notFound().build();

        StreamingResponseBody responseBody = outputStream -> {
            File file = new File("/Users/user/Downloads/Image of Ngoc mother from iphone 6s-20251214T094836Z-3-001.zip");
            if (!file.exists()) return;
            try (FileInputStream fis = new FileInputStream(file)
            ) {
                ZipEntry zipEntry;
                byte[] buffer = new byte[8192];
                int bytesRead;
                // Read the source file in chunks and write to the ZipOutputStream
                while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Close the current entry (important before adding a new one or closing the stream)
                outputStream.flush();
            }

        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }
}
