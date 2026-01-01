package com.badminton.service.impl;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Session;
import com.badminton.exception.BusinessException;
import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import com.badminton.service.ExportService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemple;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.TimeUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExcelExportService implements ExportService {
    @Autowired
    ServiceTemple serviceTemple;
    @Autowired
    SessionServiceImpl sessionService;

    @Override
    public Result<PageResponse<ReportResponse>> reportList(ReportListRequest rptListRequest) {
        return serviceTemple.execute(new ProcessCallback<ReportListRequest, PageResponse<ReportResponse>>() {
            @Override
            public ReportListRequest getRequest() {
                return rptListRequest;
            }

            @Override
            public void preProcess(ReportListRequest request) {
                Assert.notNull(request, "Request must not be null");
                validatePagination(request);
            }

            @Override
            public PageResponse<ReportResponse> process() throws BusinessException {
                PageResponse<ReportResponse> pageResponse = new PageResponse<>();
//                List<AvailablePlayer> availablePlayers = sessionService.getListAvailablePlayerInSessions(getRequest().getYearMonth());
                List<Session> listSession = sessionService.findListSessionBy(getRequest().getYearMonth(), getRequest().getPagination());
                pageResponse.setList(convertToListReportResponse(listSession));
                pageResponse.setPagination(getRequest().getPagination());
                pageResponse.setTotal(listSession.size());
                return pageResponse;
            }
        });
    }

    @Override
    public byte[] exportReport(String sessionId) {
        try (Workbook workbook = new XSSFWorkbook()) {


            ;
            Sheet sheet = workbook.createSheet("Manager Report");

            // ===== Header style =====
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // ===== Header row =====
            Row headerRow = sheet.createRow(0);
            String[] headers = {"No", "Date", "From - To", "Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // ===== Export to byte[] =====
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        } catch (IOException e) {
//            throw new RuntimeException(e);
        }
        return new byte[0];
    }

    private List<ReportResponse> convertToListReportResponse(List<Session> listSession) {
        return IntStream.range(0, listSession.size())
                .mapToObj(index -> convertToReportResponse(index, listSession.get(index))).collect(Collectors.toList());
    }

    private ReportResponse convertToReportResponse(int index, Session s) {
        ReportResponse rptResponse = new ReportResponse();
        rptResponse.setNo(index);
        rptResponse.setSessionId(s.getSessionId());
        rptResponse.setDate(s.getFromTime().toString());
        rptResponse.setDuring(TimeUtils.convertInstantsToString(s.getFromTime(), s.getToTime()));
        rptResponse.setGrossRevenue(getTotalGrossRevenue(s.getAvailablePlayers()));
        return rptResponse;
    }

    private float getTotalGrossRevenue(List<AvailablePlayer> availablePlayers) {
        return availablePlayers.stream()
                .filter(player -> player.getPayAmount() != null)
                .map(AvailablePlayer::getPayAmount)
                .reduce(0f, Float::sum);
    }

    private void validatePagination(ReportListRequest request) {
        Assert.notNull(request.getPagination(), "Pagination must be null");
        if (request.getPagination().getCurrent() < 1) {
            request.getPagination().setCurrent(1);
        }
        if (request.getPagination().getPageSize() < 1) {
            request.getPagination().setPageSize(10);
        }
    }
}
