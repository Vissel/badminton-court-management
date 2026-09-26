package com.badminton.service;

import com.badminton.core.debit.CoreDebitReportService;
import com.badminton.enums.DebitReportMode;
import com.badminton.enums.DebitReportScope;
import com.badminton.model.report.DebitReportData;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import com.badminton.service.impl.DebitReportServiceImpl;
import com.badminton.service.report.DebitExcelReportWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitReportServiceImplTest {

    @Mock
    private CoreDebitReportService coreDebitReportService;

    @Mock
    private DebitExcelReportWriter debitExcelReportWriter;

    @InjectMocks
    private DebitReportServiceImpl debitReportService;

    private DebitReportExportRequest request;

    @BeforeEach
    void setUp() {
        request = new DebitReportExportRequest();
        request.setMode("CURRENT");
        request.setScope("ALL_PLAYERS");
        request.setPlayerName(null);
        request.setPlayerNameFilter("An");
        request.setFrom("2000-01-01");
        request.setTo("9999-12-31");
        request.setSortField("PLAYER_NAME");
        request.setSortDirection("ASC");
        request.setTimeZone("Asia/Ho_Chi_Minh");
    }

    @Test
    void playerScopeRequiresPlayerName() {
        request.setScope("PLAYER");
        request.setPlayerName(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> debitReportService.exportToStream(request, new ByteArrayOutputStream()));
        assertTrue(ex.getMessage().contains("playerName"));
        verifyNoInteractions(coreDebitReportService);
    }

    @Test
    void allPlayersSupportsOptionalFilter() {
        request.setPlayerNameFilter("  ");
        DebitReportData data = DebitReportData.builder()
                .mode(DebitReportMode.CURRENT)
                .summaries(List.of())
                .currentDetails(List.of())
                .build();
        when(coreDebitReportService.loadReportData(any(), any(), any(), any(), any(), any())).thenReturn(data);
        doNothing().when(debitExcelReportWriter).write(any(), any(), any(), any());

        assertDoesNotThrow(() -> debitReportService.exportToStream(request, new ByteArrayOutputStream()));
    }

    @Test
    void emptyResultsProduceValidWorkbook() {
        DebitReportData data = DebitReportData.builder()
                .mode(DebitReportMode.CURRENT)
                .summaries(List.of())
                .currentDetails(List.of())
                .build();
        when(coreDebitReportService.loadReportData(any(), any(), any(), any(), any(), any())).thenReturn(data);
        doNothing().when(debitExcelReportWriter).write(any(), any(), any(), any());

        assertDoesNotThrow(() -> debitReportService.exportToStream(request, new ByteArrayOutputStream()));
    }

    @Test
    void rejectsUnknownEnumValues() {
        request.setMode("UNKNOWN");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> debitReportService.exportToStream(request, new ByteArrayOutputStream()));
        assertTrue(ex.getMessage().contains("mode"));
    }

    @Test
    void rejectsUnsupportedTimeZone() {
        request.setTimeZone("Mars/Phobos");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> debitReportService.exportToStream(request, new ByteArrayOutputStream()));
        assertTrue(ex.getMessage().contains("timeZone"));
    }

    @Test
    void writerReceivesBigDecimalPrecision() {
        DebtCurrentReportRow detail = new DebtCurrentReportRow(1, "An", null,
                new BigDecimal("123456.78"), "VND", "=cmd");
        DebtPlayerSummaryRow summary = new DebtPlayerSummaryRow(1, "An",
                new BigDecimal("123456.78"), 1L, null, null, null, null, null);
        DebitReportData data = DebitReportData.builder()
                .mode(DebitReportMode.CURRENT)
                .summaries(List.of(summary))
                .currentDetails(List.of(detail))
                .build();
        when(coreDebitReportService.loadReportData(any(), any(), any(), any(), any(), any())).thenReturn(data);
        doNothing().when(debitExcelReportWriter).write(any(), any(), any(), any());

        ArgumentCaptor<DebitReportData> dataCaptor = ArgumentCaptor.forClass(DebitReportData.class);
        debitReportService.exportToStream(request, new ByteArrayOutputStream());
        verify(debitExcelReportWriter).write(dataCaptor.capture(), any(), any(), any(OutputStream.class));

        DebtCurrentReportRow captured = dataCaptor.getValue().getCurrentDetails().get(0);
        assertEquals(new BigDecimal("123456.78"), captured.getRemainingAmount());
    }
}
