package com.badminton.service.impl;

import com.badminton.constant.CommonConstant;
import com.badminton.constant.ErrorConstant;
import com.badminton.constant.GameConstant;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import com.badminton.entity.Team;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ReportCost;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.TeamDTO;
import com.badminton.model.report.*;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.GameRepository;
import com.badminton.requestmodel.ExportReportRequest;
import com.badminton.requestmodel.Pagination;
import com.badminton.requestmodel.ReportListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.ReportResponse;
import com.badminton.response.result.Result;
import com.badminton.service.ExportService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemple;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceUtil;
import com.badminton.util.TimeUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ExcelExportService implements ExportService {
    @Autowired
    ServiceTemple serviceTemple;
    @Autowired
    SessionServiceImpl sessionService;
    @Autowired
    AvailablePlayerRepository availablePlayerRepo;
    @Autowired
    GameRepository gameRepo;

    private static final int HEADER_ROW_INDEX = 0;
    private static final int HEADER_CELL_INDEX = HEADER_ROW_INDEX;
    private static final int START_HEADER_ROW_INDEX = 2;
    private static final int START_HEADER_DATA_ROW_INDEX = 3;

    private static final int START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX = 5;
    private static final int START_BODY_TABLE_DATA_ROW_RPT_INDEX = 6;
    private static final String DATE_VN = "Ngày";
    private static final String FORMATED_DATE_VN = "Ngày đinh dạng";
    private static final String DURING_VN = "Thời gian";
    private static final String TOTAL_VN = "Tổng tiền";
    private static final String TOTAL_SHUTTLE = "Tổng cầu ";
    private static final String TOTAL = "Tổng ";
    private static final String PLAYER_NO_VN = "Stt";
    private static final String PLAYER_NAME_VN = "Người chơi";
    private static final String PAY_AMOUNT_VN = "Số tiền thanh toán";
    private static final String LEAVE_TIME_VN = "Thời gian rời sân";
    private static final String COURT_FEE_VN = "Tiền sân";
    private static final String TITLE_VN = "Bảng thống kê" + CommonConstant.COLON;

    private static final int DATE_COL = 0;
    private static final int DATE_FORMAT_COL = 1;
    private static final int DURING_COL = 2;
    private static final int TOTAL_COL = 3;
    private static final int TOTAL_SHUTTLE_COL = 4;
    private static final int PLAYER_NO_COL = 4;
    private static final int PLAYER_NAME_COL = 5;
    private static final int PAY_AMT_COL = 6;
    private static final int LEAVE_TIME_COL = 7;
    private static final int COURT_FEE_COL = 8;
    private static final List<Integer> columns;

    static {
        columns = new ArrayList<>();
        columns.add(DATE_COL);
        columns.add(DATE_FORMAT_COL);
        columns.add(DURING_COL);
        columns.add(TOTAL_COL);
        columns.add(PLAYER_NO_COL);
        columns.add(PLAYER_NAME_COL);
        columns.add(PAY_AMT_COL);
        columns.add(LEAVE_TIME_COL);
        columns.add(COURT_FEE_COL);
    }

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

                long count = sessionService.countSessionBy(getRequest().getYearMonth(), getRequest().getPagination());
                pageResponse.setTotal(count);
                if (count == 0) {
                    return pageResponse;
                }
                List<Session> listSession = sessionService.findListSessionBy(getRequest().getYearMonth(), getRequest().getPagination());
                pageResponse.setList(convertToListReportResponse(listSession));
                Pagination paginationResponse = getRequest().getPagination();
                final int totalPage = ((int) count / getRequest().getPagination().getPageSize()) + 1;
                paginationResponse.setTotalPage(totalPage);
                pageResponse.setPagination(paginationResponse);
                return pageResponse;
            }
        });
    }

    @Transactional
    @Override
    public Result<ExportReportResult> exportReport(String sessionId) {

        return serviceTemple.execute(new ProcessCallback<String, ExportReportResult>() {
            @Override
            public String getRequest() {
                return sessionId;
            }

            @Override
            public void preProcess(String request) {
                Assert.isTrue(StringUtils.isNotBlank(request), "Session id must not be blank");
            }

            @Override
            public ExportReportResult process() throws BusinessException {
                ReportDTO reportDTO = retrieveReport(sessionId);
                String fileName = buildFileName(reportDTO.getSession());
                ByteArrayResource resource = new ByteArrayResource(generateSingleXlsxReport(reportDTO));
                return new ExportReportResult(resource, fileName);
            }
        });
    }

    private String buildFileName(Session session) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
                .format(session.getFromTime());
        String during = TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime())
                .replace(":", "h")
                .replace(" - ", "-");
        return "report_" + date + "_" + during + ".xlsx";
    }

    @Transactional
    @Override
    public void streamingExportReportList(ExportReportRequest exportedRptRequest, OutputStream outputStream) throws IOException {
        String title = TITLE_VN + exportedRptRequest.getSessionIds().size() + " phiên làm việc.";
        Map<String, RptShuttle> shuttleAcc = new LinkedHashMap<>();
        Map<String, RptService> serviceAcc = new LinkedHashMap<>();
        double totalPayAcc = 0;
        Instant firstDate = null;
        Instant lastDate = null;

        // SXSSFWorkbook(-1) disables automatic row flushing so pre-created rows 2-3 remain
        // accessible after writing data rows, allowing us to back-fill the summary header.
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(-1)) {
            SXSSFSheet sheet = workbook.createSheet("Report");
            writeTitle(title, workbook, sheet);
            // Pre-create summary rows in correct ascending order before data rows (6+)
            sheet.createRow(START_HEADER_ROW_INDEX);
            sheet.createRow(START_HEADER_DATA_ROW_INDEX);
            writeTableHeader(sheet, workbook);

            int rowIndex = START_BODY_TABLE_DATA_ROW_RPT_INDEX;
            int playerNo = 1;

            for (Integer sessionId : exportedRptRequest.getSessionIds()) {
                ReportDTO report = retrieveReport(String.valueOf(sessionId));
                Instant sessionDate = report.getSession().getFromTime();

                if (firstDate == null || sessionDate.isBefore(firstDate)) firstDate = sessionDate;
                if (lastDate == null || sessionDate.isAfter(lastDate)) lastDate = sessionDate;
                totalPayAcc += totalPayOf(report.getAvailablePlayers());
                accumulate(shuttleAcc, report.getListTotalShuttle(), RptShuttle::getShuttleName);
                accumulate(serviceAcc, report.getListTotalService(), RptService::getServiceName);

                int[] playerNoRef = {playerNo};
                rowIndex = writeReportData(report, rowIndex, playerNoRef, sheet);
                playerNo = playerNoRef[0];
            }

            writeMultiSessionHeader(sheet, workbook, firstDate, lastDate, totalPayAcc);
            writeTotalHeader(sheet, workbook, new ArrayList<>(shuttleAcc.values()), new ArrayList<>(serviceAcc.values()));
            columns.forEach(sheet::autoSizeColumn);
            sheet.setAutoFilter(new CellRangeAddress(START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX, START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX, DATE_COL, COURT_FEE_COL));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.dispose();
        }
    }

    private <T extends RptModel> void accumulate(Map<String, T> acc, List<T> items, Function<T, String> keyFn) {
        items.forEach(item -> acc.merge(keyFn.apply(item), item, (existing, next) -> {
            existing.setQuantity(existing.getQuantity() + next.getQuantity());
            return existing;
        }));
    }

    private double totalPayOf(List<AvailablePlayer> players) {
        return players.stream().mapToDouble(p -> p.getPayAmount() != null ? p.getPayAmount() : 0).sum();
    }

    private void writeMultiSessionHeader(SXSSFSheet sheet, Workbook workbook, Instant firstDate, Instant lastDate, double totalPay) {
        Row r1 = sheet.getRow(START_HEADER_ROW_INDEX);
        r1.createCell(DATE_COL).setCellValue(DATE_VN);
        r1.createCell(DATE_FORMAT_COL).setCellValue(FORMATED_DATE_VN);
        r1.createCell(TOTAL_COL).setCellValue(TOTAL_VN);
        r1.setRowStyle(createTblStyle(workbook));

        Row r2 = sheet.getRow(START_HEADER_DATA_ROW_INDEX);
        r2.createCell(DATE_COL).setCellValue(firstDate != null ? TimeUtils.toDateDisplay(firstDate, TimeUtils.newVNLocal()) : "");
        r2.createCell(DATE_FORMAT_COL).setCellValue(lastDate != null ? TimeUtils.toVNDateFormat(lastDate) : "");
        r2.createCell(TOTAL_COL).setCellValue(totalPay);
    }

    @Override
    public String buildListReportFileName(ExportReportRequest request) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
        Instant first = null, last = null;
        for (Integer id : request.getSessionIds()) {
            Session s = sessionService.findSessionById(id.toString());
            if (s == null) continue;
            if (first == null || s.getFromTime().isBefore(first)) first = s.getFromTime();
            if (last == null || s.getFromTime().isAfter(last)) last = s.getFromTime();
        }
        String fromStr = first != null ? fmt.format(first) : "unknown";
        String toStr = last != null ? fmt.format(last) : "unknown";
        return "report_" + fromStr + "-" + toStr + ".xlsx";
    }

    private void writeTitle(String title, Workbook workbook, Sheet sheet) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleStyle.setFont(titleFont);
        Cell titleCell = sheet.createRow(HEADER_ROW_INDEX).createCell(HEADER_CELL_INDEX);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
    }

    /**
     * return next row index
     *
     * @param reportData
     * @param rowIndex
     * @param playerNoArray array containing player number (passed by reference)
     * @param sheet
     * @return
     */
    private int writeReportData(ReportDTO reportData, int rowIndex, int[] playerNoArray, SXSSFSheet sheet) {
        Session session = reportData.getSession();
        List<AvailablePlayer> players = reportData.getAvailablePlayers();
        List<Game> games = reportData.getGames();

        Map<Long, String> partnerMap = players.stream().collect(Collectors.toMap(AvailablePlayer::getAvaId, p -> p.getPlayer().getPlayerName(), (a, b) -> a));

        String date = TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal());
        String dateFormat = TimeUtils.toVNDateFormat(session.getFromTime());
        String during = TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime());
        SXSSFCell totalAmtCell = null;
        Float totalAmt = MoneyUtils.DEFAULT;

        int currentRowI = rowIndex;
        SXSSFRow row;
        SXSSFRow rowNumber;
        for (AvailablePlayer avaPlayer : players) {

            // --- ROW N: Basic Info + Game Headers ---
            row = sheet.createRow(currentRowI++);

            row.createCell(DATE_COL).setCellValue(date);
            row.createCell(DATE_FORMAT_COL).setCellValue(dateFormat);
            row.createCell(DURING_COL).setCellValue(during);
            if (totalAmtCell == null) {
                totalAmtCell = row.createCell(TOTAL_COL); // set value later
            }
            row.createCell(PLAYER_NO_COL).setCellValue(playerNoArray[0]++);
            row.createCell(PLAYER_NAME_COL).setCellValue(avaPlayer.getPlayer().getPlayerName());
            row.createCell(PAY_AMT_COL).setCellValue(getPayAmount(avaPlayer));
            row.createCell(LEAVE_TIME_COL).setCellValue(TimeUtils.convertInstantToTimeStr(avaPlayer.getLeaveTime()));
            row.createCell(COURT_FEE_COL);

            // --- ROW N+1: set duplicated value for filter.
            rowNumber = sheet.createRow(currentRowI++);
            rowNumber.createCell(DATE_COL).setCellValue(date);
            rowNumber.createCell(DATE_FORMAT_COL).setCellValue(dateFormat);
            rowNumber.createCell(COURT_FEE_COL).setCellValue(extractCourtFee(avaPlayer.getServices()));
            rowNumber.createCell(PLAYER_NO_COL).setCellValue(playerNoArray[0] - 1);
            rowNumber.createCell(PLAYER_NAME_COL).setCellValue(avaPlayer.getPlayer().getPlayerName());

            int colIndex = row.getLastCellNum();
            colIndex = writeGameInfoColumns(games, avaPlayer, partnerMap, colIndex, row, rowNumber);
            setRemainingServices(avaPlayer.getServices(), colIndex, row, rowNumber);
            // sum player payAmount
            totalAmt = Float.sum(totalAmt, getPayAmount(avaPlayer));
        }
        if (totalAmtCell != null) {
            totalAmtCell.setCellValue(totalAmt);
        }
        // currentRow is now next row index
        return currentRowI;
    }

    @Override
    public void normalExportReportList(ExportReportRequest exportedRptRequest, HttpServletResponse response) {

    }

    public byte[] generateSingleXlsxReport(ReportDTO reportDTO) throws BusinessException {
        Session session = reportDTO.getSession();
        List<AvailablePlayer> players = reportDTO.getAvailablePlayers();
        List<Game> games = reportDTO.getGames();
//        List<ReportCost> listCost = reportDTO.getListCost();
        String title = TITLE_VN + TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal());
        // If you have a template, use: Workbook workbook = WorkbookFactory.create(new File("template.xlsx"));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");
        writeTitle(title, workbook, sheet);
        writeHeader(sheet, workbook, session, players);
        writeTotalHeader(sheet, workbook, reportDTO.getListTotalShuttle(), reportDTO.getListTotalService());

        Row tableHeader = sheet.createRow(START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX);
        String[] tblHeaders = {PLAYER_NO_VN, PLAYER_NAME_VN, PAY_AMOUNT_VN, LEAVE_TIME_VN, COURT_FEE_VN};
        for (int i = 0; i < tblHeaders.length; i++) tableHeader.createCell(i).setCellValue(tblHeaders[i]);

        // 3. Map for Partner Names (AvaId -> Name)
        Map<Long, String> partnerMap = players.stream().collect(
                Collectors.toMap(AvailablePlayer::getAvaId,
                        p -> p.getPlayer().getPlayerName(), (a, b) -> a));

        int currentRow = START_BODY_TABLE_DATA_ROW_RPT_INDEX;
        int no = 1;

        for (AvailablePlayer avaPlayer : players) {
            // --- ROW N: Basic Info + Game Headers ---
            Row rowHeader = sheet.createRow(currentRow++);
            rowHeader.createCell(0).setCellValue(no++);
            rowHeader.createCell(1).setCellValue(avaPlayer.getPlayer().getPlayerName());
            rowHeader.createCell(2).setCellValue(getPayAmount(avaPlayer));
            rowHeader.createCell(3).setCellValue(TimeUtils.convertInstantToTimeStr(avaPlayer.getLeaveTime()));
            rowHeader.createCell(4);

            // --- ROW N+1: Empty A-E + Game Values ---
            Row rowNumber = sheet.createRow(currentRow++);
            rowNumber.createCell(0).setCellValue(no - 1);
            rowNumber.createCell(1).setCellValue(avaPlayer.getPlayer().getPlayerName());
            rowNumber.createCell(4).setCellValue(extractCourtFee(avaPlayer.getServices()));
            int colIndex = 5;
            colIndex = writeGameInfoColumns(games, avaPlayer, partnerMap, colIndex, rowHeader, rowNumber);
            setRemainingServices(avaPlayer.getServices(), colIndex, rowHeader, rowNumber);
//            for (ReportCost rptCost : listCost) {
//                setServiceValueIntoNextTwoColumn(rptCost, avaPlayer, partnerMap, colIndex, rowHeader, rowNumber);
//                colIndex += 2; // Move to next game pair (F, H, J...)
//            }
        }
        // Auto-size columns for basic info
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setAutoFilter(new CellRangeAddress(START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX, START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX, 0, tblHeaders.length - 1));
        // Write to file
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.EXPORTING_ERROR, "");
        }
    }

    private void writeTotalHeader(Sheet sheet, Workbook workbook, List<RptShuttle> listTotalShuttle, List<RptService> listTotalService) {
        Row r1 = sheet.getRow(START_HEADER_ROW_INDEX);
        Row r2 = sheet.getRow(START_HEADER_DATA_ROW_INDEX);

        int colIndex = TOTAL_SHUTTLE_COL;
        for (RptShuttle rptShuttle : listTotalShuttle) {
            r1.createCell(colIndex).setCellValue(TOTAL_SHUTTLE + rptShuttle.rptModelDisplay());
            r2.createCell(colIndex).setCellValue(rptShuttle.getQuantity());
            colIndex++;
        }
        for (RptService rptService : listTotalService) {
            r1.createCell(colIndex).setCellValue(TOTAL + rptService.rptModelDisplay());
            r2.createCell(colIndex).setCellValue(rptService.getQuantity());
            colIndex++;
        }
    }

    private int writeGameInfoColumns(List<Game> games, AvailablePlayer avaPlayer, Map<Long, String> partnerMap,
                                     int colIndex, Row row, Row rowNumber) {
        for (Game game : games) {
            try {
                setServiceValueIntoNextTwoColumn(game, avaPlayer, partnerMap, colIndex, row, rowNumber);
                colIndex += 2;
            } catch (NullPointerException e) {
                log.warn("{} There is no matching data.", ErrorConstant.ERROR_EXPORT_RPT_WRITE_DATA);
            }
        }
        return colIndex;
    }

    private void setRemainingServices(String services, int colIndex, Row row, Row rowNumber) {
        List<ServiceDTO> remainServices = ServiceUtil.convertStringToListService(services).stream()
                .filter(s -> !s.getServiceName().toLowerCase().contains(GameConstant.COST_IN_PERSON_VN.toLowerCase()))
                .collect(Collectors.toList());
        for (ServiceDTO serviceDTO : remainServices) {
            row.createCell(colIndex).setCellValue(serviceDTO.getServiceName());
            rowNumber.createCell(colIndex).setCellValue(serviceDTO.getCost());
            colIndex += 2;
        }
    }

    private void setServiceValueIntoNextTwoColumn(Game game, AvailablePlayer avaPlayer, Map<Long, String> partnerMap,
                                                  int colIndex, Row row, Row rowNumber) throws NullPointerException {
        Team matchingTeam = getPlayerTeamInGame(avaPlayer, game);

        String gameTitle = String.format("%s (%s - %s) %s:", game.getCourt().getCourtName(), TimeUtils.convertInstantToTimeStr(game.getCreatedDate()), TimeUtils.convertInstantToTimeStr(game.getEndedDate()), matchingTeam.isWin() ? GameConstant.WIN_VN : GameConstant.LOSE_VN);
        row.createCell(colIndex).setCellValue(gameTitle);

        // Row N: "Court (Time):" and "partner: Name"
        String partnerName = getPartnerName(avaPlayer, matchingTeam, partnerMap);
        row.createCell(colIndex + 1).setCellValue(GameConstant.PARTNER_VN.concat(CommonConstant.COLON).concat(partnerName));
        rowNumber.createCell(colIndex).setCellValue(getExpenseValue(avaPlayer, matchingTeam));

    }

    private void setServiceValueIntoNextTwoColumn(ReportCost rptCost, AvailablePlayer avaPlayer, Map<Long, String> partnerMap,
                                                  int colIndex, Row row, Row rowNumber) {
        TeamDTO matchingTeam = getPlayerTeamInGame(avaPlayer, rptCost);

        String gameTitle = String.format("%s (%s - %s) %s:",
                rptCost.getCourtName(), rptCost.getStart(),
                rptCost.getEnd(),
                matchingTeam.isWin() ? GameConstant.WIN_VN : GameConstant.LOSE_VN);

        row.createCell(colIndex).setCellValue(gameTitle);

        // Row N: "Court (Time):" and "partner: Name"
        String partnerName = getPartnerName(avaPlayer, matchingTeam);

        row.createCell(colIndex + 1).setCellValue(GameConstant.PARTNER_VN.concat(CommonConstant.COLON).concat(partnerName));
        rowNumber.createCell(colIndex).setCellValue(getExpenseValue(avaPlayer, null));
    }

    private String getPartnerName(AvailablePlayer currAvaPlayer, Team matchingTeam, Map<Long, String> partnerMap) {
        AvailablePlayer anotherPlayer = getAnotherPlayer(currAvaPlayer, matchingTeam);
        return anotherPlayer != null ? partnerMap.getOrDefault(anotherPlayer.getAvaId(), GameConstant.NO_PARTNER) : GameConstant.NO_PARTNER;
    }

    private String getPartnerName(AvailablePlayer currAvaPlayer, TeamDTO matchingTeam) {
        final long availableId = currAvaPlayer.getAvaId();
        String partnerName;
        if (matchingTeam.getPlayer1().getId() == availableId) {
            partnerName = matchingTeam.getPlayer2().getName();
        } else if (matchingTeam.getPlayer2().getId() == availableId) {
            partnerName = matchingTeam.getPlayer1().getName();
        } else {
            partnerName = GameConstant.NO_PARTNER;
        }
        return partnerName;
    }

    private float getExpenseValue(AvailablePlayer currAvaPlayer, Team matchingTeam) {
        float expense = MoneyUtils.DEFAULT;
        try {
            if (matchingTeam.getPlayerOne() != null && matchingTeam.getPlayerOne().equals(currAvaPlayer)) {
                expense = matchingTeam.getExpenseOne();
            } else if (matchingTeam.getPlayerTwo() != null && matchingTeam.getPlayerTwo().equals(currAvaPlayer)) {
                expense = matchingTeam.getExpenseTwo();
            }
        } catch (NullPointerException e) {
            log.warn("{} Failed to get expense value for availablePlayerId={}, teamId={}",
                    ErrorConstant.ERROR_EXPORT_RPT_WRITE_DATA, currAvaPlayer.getAvaId(), matchingTeam.getTeamId());
        }
        return expense;
    }

    private String getAnotherPlayer(AvailablePlayer currAvaPlayer, Team matchingTeam, Map<Long, String> partnerMap) {
        AvailablePlayer anotherPlayer = matchingTeam.getPlayerOne();
        if (matchingTeam.getPlayerOne().equals(currAvaPlayer)) {
            anotherPlayer = matchingTeam.getPlayerTwo();
        }
        return partnerMap.getOrDefault(anotherPlayer.getAvaId(), GameConstant.NO_PARTNER);
    }

    private AvailablePlayer getAnotherPlayer(AvailablePlayer currAvaPlayer, Team matchingTeam) {
        AvailablePlayer anotherPlayer = matchingTeam.getPlayerOne();
        if (anotherPlayer != null && anotherPlayer.equals(currAvaPlayer)) {
            return matchingTeam.getPlayerTwo();
        }
        anotherPlayer = matchingTeam.getPlayerTwo();
        if (anotherPlayer != null && anotherPlayer.equals(currAvaPlayer)) {
            return matchingTeam.getPlayerOne();
        }
        return anotherPlayer;
    }

    private void writeHeader(Sheet sheet, Workbook workbook, Session session, List<AvailablePlayer> players) {
        double totalPay = players.stream().mapToDouble(p -> p.getPayAmount() != null ? p.getPayAmount() : 0).sum();

        Row r1 = sheet.createRow(START_HEADER_ROW_INDEX); // Row 3
        r1.createCell(DATE_COL).setCellValue(DATE_VN);
        r1.createCell(DATE_FORMAT_COL).setCellValue(FORMATED_DATE_VN);
        r1.createCell(DURING_COL).setCellValue(DURING_VN);
        r1.createCell(TOTAL_COL).setCellValue(TOTAL_VN);
        r1.setRowStyle(createTblStyle(workbook));

        Row r2 = sheet.createRow(START_HEADER_DATA_ROW_INDEX); // Row 4
        r2.createCell(DATE_COL).setCellValue(TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal()));
        r2.createCell(DATE_FORMAT_COL).setCellValue(TimeUtils.toVNDateFormat(session.getFromTime())); // Format as needed
        r2.createCell(DURING_COL).setCellValue(TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime()));
        r2.createCell(TOTAL_COL).setCellValue(totalPay);
    }

    private void writeTableHeader(SXSSFSheet sheet, Workbook workbook) {
        Row r1 = sheet.createRow(START_BODY_TABLE_HEADER_ROW_SINGLE_RPT_INDEX); // Row 6
        r1.createCell(DATE_COL).setCellValue(DATE_VN);
        r1.createCell(DATE_FORMAT_COL).setCellValue(FORMATED_DATE_VN);
        r1.createCell(DURING_COL).setCellValue(DURING_VN);
        r1.createCell(TOTAL_COL).setCellValue(TOTAL_VN);
        r1.createCell(PLAYER_NO_COL).setCellValue(PLAYER_NO_VN);
        r1.createCell(PLAYER_NAME_COL).setCellValue(PLAYER_NAME_VN);
        r1.createCell(PAY_AMT_COL).setCellValue(PAY_AMOUNT_VN);
        r1.createCell(LEAVE_TIME_COL).setCellValue(LEAVE_TIME_VN);
        r1.createCell(COURT_FEE_COL).setCellValue(COURT_FEE_VN);
        sheet.trackColumnsForAutoSizing(columns);
        r1.setRowStyle(createTblStyle(workbook));
    }

    private CellStyle createTblStyle(Workbook workbook) {
        CellStyle tblHeaderStyle = workbook.createCellStyle();
        Font tblFont = workbook.createFont();
        tblFont.setFontHeightInPoints((short) (tblFont.getFontHeightInPoints() + 3));
        tblFont.setBold(true);
        return tblHeaderStyle;
    }

    private float extractCourtFee(String services) {
        List<ServiceDTO> listService = ServiceUtil.convertStringToListService(services);
        Optional<ServiceDTO> costInPerService = listService.stream().filter(s -> GameConstant.COST_IN_PERSON_VN.equals(s.getServiceName())).findFirst();
        return costInPerService.isPresent() ? costInPerService.get().getCost() : MoneyUtils.DEFAULT;
    }

    private Team getPlayerTeamInGame(AvailablePlayer p, Game g) throws NullPointerException {
        if (g.getTeamOne() != null && (p.equals(g.getTeamOne().getPlayerOne()) || p.equals(g.getTeamOne().getPlayerTwo())))
            return g.getTeamOne();
        if (g.getTeamTwo() != null && (p.equals(g.getTeamTwo().getPlayerOne()) || p.equals(g.getTeamTwo().getPlayerTwo())))
            return g.getTeamTwo();
        throw new NullPointerException();
    }

    private TeamDTO getPlayerTeamInGame(AvailablePlayer p, ReportCost rptCost) throws NullPointerException {
        final long availableId = p.getAvaId();

        if (rptCost.getTeam1() != null && (availableId == rptCost.getTeam1().getPlayer1().getId() || availableId == rptCost.getTeam1().getPlayer2().getId()))
            return rptCost.getTeam1();
        if (rptCost.getTeam2() != null && (availableId == rptCost.getTeam2().getPlayer1().getId() || availableId == rptCost.getTeam2().getPlayer2().getId()))
            return rptCost.getTeam2();
        throw new NullPointerException();
    }

    private List<ReportResponse> convertToListReportResponse(List<Session> listSession) {
        return IntStream.range(0, listSession.size()).mapToObj(index -> convertToReportResponse(index, listSession.get(index))).collect(Collectors.toList());
    }

    private ReportResponse convertToReportResponse(int index, Session s) {
        ReportResponse rptResponse = new ReportResponse();
        rptResponse.setNo(index);
        rptResponse.setSessionId(s.getSessionId());
        rptResponse.setDate(s.getFromTime());
        rptResponse.setDuring(TimeUtils.convertInstantsToString(s.getFromTime(), s.getToTime()));
        float revenue = getTotalGrossRevenue(s.getAvailablePlayers());
        rptResponse.setGrossRevenue(revenue);
        rptResponse.setGrossRevenueFormat(MoneyUtils.formatToVND(revenue));
        return rptResponse;
    }

    private float getTotalGrossRevenue(List<AvailablePlayer> availablePlayers) {
        return availablePlayers.stream().filter(player -> player.getPayAmount() != null).map(AvailablePlayer::getPayAmount).reduce(0f, Float::sum);
    }

    private void validatePagination(ReportListRequest request) {
        Assert.notNull(request.getPagination(), "Pagination must be null");
        if (request.getPagination().getCurrent() < 0) {
            request.getPagination().setCurrent(0);
        }
        if (request.getPagination().getPageSize() < 1) {
            request.getPagination().setPageSize(10);
        }

    }

    private Float getPlayerExpenseForGame(AvailablePlayer player, Game game) {
        // Match player to Team 1
        if (game.getTeamOne() != null && isPlayerInTeam(player, game.getTeamOne())) {
            return game.getTeamOne().getExpenseOne();
        }
        // Match player to Team 2
        if (game.getTeamTwo() != null && isPlayerInTeam(player, game.getTeamTwo())) {
            return game.getTeamTwo().getExpenseTwo();
        }
        return null;
    }

    private boolean isPlayerInTeam(AvailablePlayer avaPlayer, Team team) {
        Long pid = avaPlayer.getAvaId();
        return pid.equals(team.getPlayerOne()) || pid.equals(team.getPlayerTwo());
    }

    public ReportDTO retrieveReport(String sessionId) {
        Session session = sessionService.findSessionById(sessionId);
        Assert.notNull(session, "Current session is not found");

        List<AvailablePlayer> availablePlayers = availablePlayerRepo.findAllBySession(session);
        List<Long> avaIds = availablePlayers.stream().map(AvailablePlayer::getAvaId).toList();
        List<Game> games = gameRepo.findGamesByPlayerIds(avaIds);
        return buildReportModel(session, availablePlayers, games);
    }

    private ReportDTO buildReportModel(Session session, List<AvailablePlayer> availablePlayers, List<Game> games) {
        List<RptShuttle> listTotalShuttle = buildListTotalShuttle(games);
        List<RptService> listTotalService = buildListTotalService(availablePlayers);
        return new ReportDTO(session, availablePlayers, games, null, listTotalShuttle, listTotalService);
    }

    private List<RptShuttle> buildListTotalShuttle(List<Game> games) {
        return new ArrayList<>(games.stream()
                .filter(g -> g.getShuttleMap() != null)
                .flatMap(g -> g.getShuttleMap().stream())
                .collect(Collectors.toMap(
                        gsm -> gsm.getShuttleBall().getShuttleName(),
                        gsm -> {
                            RptShuttle r = new RptShuttle();
                            float price = gsm.getShuttleBall().getCost();
                            r.setShuttleName(gsm.getShuttleBall().getShuttleName());
                            r.setPrice(price);
                            r.setPriceFormat(MoneyUtils.formatToVND(price));
                            r.setQuantity(gsm.getShuttleNumber());
                            return r;
                        },
                        (existing, next) -> {
                            existing.setQuantity(existing.getQuantity() + next.getQuantity());
                            return existing;
                        },
                        LinkedHashMap::new
                )).values());
    }

    private List<RptService> buildListTotalService(List<AvailablePlayer> availablePlayers) {
        return new ArrayList<>(availablePlayers.stream()
                .flatMap(p -> ServiceUtil.convertStringToListService(p.getCurrentServices()).stream())
                .filter(s -> !s.getServiceName().toLowerCase().contains(GameConstant.COST_IN_PERSON_VN.toLowerCase()))
                .collect(Collectors.toMap(
                        ServiceDTO::getServiceName,
                        s -> {
                            RptService r = new RptService();
                            float price = s.getCost();
                            r.setServiceName(s.getServiceName());
                            r.setPrice(price);
                            r.setPriceFormat(MoneyUtils.formatToVND(price));
                            r.setQuantity(1);
                            return r;
                        },
                        (existing, next) -> {
                            existing.setQuantity(existing.getQuantity() + 1);
                            return existing;
                        },
                        LinkedHashMap::new
                )).values());
    }

    private Float getPayAmount(AvailablePlayer availablePlayer) {
        return availablePlayer.getPayAmount() != null ? availablePlayer.getPayAmount() : MoneyUtils.DEFAULT;
    }
}
