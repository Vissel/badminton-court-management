package com.badminton.controller;

import com.badminton.requestmodel.debit.DebitReportExportRequest;
import com.badminton.service.DebitReportService;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitControllerExportTest {

    @Mock
    private DebitReportService debitReportService;

    @InjectMocks
    private DebitController debitController;

    private DebitReportExportRequest request;

    @BeforeEach
    void setUp() {
        request = new DebitReportExportRequest();
        request.setMode("CURRENT");
        request.setScope("ALL_PLAYERS");
        request.setPlayerNameFilter("An");
        request.setFrom("2000-01-01");
        request.setTo("9999-12-31");
        request.setSortField("PLAYER_NAME");
        request.setSortDirection("ASC");
        request.setTimeZone("Asia/Ho_Chi_Minh");
    }

    @Test
    void successfulExportReturnsXlsxContentTypeAndDisposition() throws IOException {
        when(debitReportService.buildContentDisposition(any()))
                .thenReturn("attachment; filename=\"debt-report.xlsx\"; filename*=UTF-8''cong-no-hien-tai_20260926_153000.xlsx");
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
                workbook.createSheet("Sheet1");
                workbook.write(out);
            }
            return null;
        }).when(debitReportService).exportToStream(any(), any(OutputStream.class));

        ResponseEntity<StreamingResponseBody> response = debitController.exportDebitReport(request);

        assertNotNull(response);
        assertEquals(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), response.getHeaders().getContentType());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"));
        assertTrue(contentDisposition.contains("debt-report.xlsx"));
        assertTrue(contentDisposition.contains("filename*=UTF-8''"));

        StreamingResponseBody body = response.getBody();
        assertNotNull(body);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        body.writeTo(baos);
        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length > 0);
        assertTrue(isZipStream(bytes));

        verify(debitReportService).exportToStream(eq(request), any(OutputStream.class));
    }

    @Test
    void historyScopeVariationsAccepted() {
        when(debitReportService.buildContentDisposition(any()))
                .thenReturn("attachment; filename=\"debt-report.xlsx\"; filename*=UTF-8''lich-su-no_20260926_153000.xlsx");

        request.setMode("HISTORY");
        request.setScope("PLAYER");
        request.setPlayerName("Nguyễn Văn An");
        request.setPlayerNameFilter(null);

        assertDoesNotThrow(() -> debitController.exportDebitReport(request));
    }

    private boolean isZipStream(byte[] bytes) {
        return bytes.length >= 4 &&
                bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 0x03 && bytes[3] == 0x04;
    }
}
