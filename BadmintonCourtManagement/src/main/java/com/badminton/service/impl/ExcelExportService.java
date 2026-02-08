package com.badminton.service.impl;

import com.badminton.constant.CommonConstant;
import com.badminton.constant.GameConstant;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import com.badminton.entity.Team;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.ReportDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final int START_DATA_ROW_INDEX = 3;
    private static final int START_HEADER_ROW_SINGLE_RPT_INDEX = 5;
    private static final int START_DATA_ROW_SINGLE_RPT_INDEX = 6;
    private static final String DATE_VN = "Ngày";
    private static final String FORMATED_DATE_VN = "Ngày đinh dạng";
    private static final String DURING_VN = "Thời gian";
    private static final String TOTAL_VN = "Tổng tiền";
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
    public Result<ByteArrayResource> exportReport(String sessionId) {

        return serviceTemple.execute(new ProcessCallback<String, ByteArrayResource>() {
            @Override
            public String getRequest() {
                return sessionId;
            }

            @Override
            public void preProcess(String request) {
                Assert.isTrue(StringUtils.isNotBlank(request), "Session id must no be blank");
            }

            @Override
            public ByteArrayResource process() throws BusinessException {
                // retrieve the session, list available players, list game
                ReportDTO reportDTO = retrieveReport(sessionId);
                return new ByteArrayResource(generateSingleXlsxReport(reportDTO));
            }
        });
    }

    @Transactional
    @Override
    public void streamingExportReportList(ExportReportRequest exportedRptRequest, OutputStream outputStream) throws IOException {
        List<ReportDTO> listReport = new ArrayList<>();
        for (Integer sessionId : exportedRptRequest.getSessionIds()) {
            listReport.add(retrieveReport(String.valueOf(sessionId))); // query report data from database - success
        }
        String title = TITLE_VN + listReport.size() + " phiên làm việc.";

        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
        ) {
            SXSSFSheet sheet = workbook.createSheet("Report");
            writeTitle(title, workbook, sheet);
            writeSessionHeader(sheet, workbook);

            int rowIndex = START_DATA_ROW_INDEX;
            for (int i = 0; i < listReport.size(); i++) {
                rowIndex = writeReportData(listReport.get(i), rowIndex, sheet);
            }

            // Auto-size columns for basic info
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setAutoFilter(new CellRangeAddress(START_HEADER_ROW_INDEX, START_HEADER_ROW_INDEX, DATE_COL, COURT_FEE_COL));
            workbook.write(outputStream);

            // Ensure all bytes are pushed out
            outputStream.flush();

            // Cleanup temporary files used by SXSSF
            workbook.dispose();
        }
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
     * @param sheet
     * @return
     */
    private int writeReportData(ReportDTO reportData, int rowIndex, SXSSFSheet sheet) {
        Session session = reportData.getSession();
        List<AvailablePlayer> players = reportData.getAvailablePlayers();
        List<Game> games = reportData.getGames();

        Map<Long, String> partnerMap = players.stream()
                .collect(Collectors.toMap(
                        AvailablePlayer::getAvaId,
                        p -> p.getPlayer().getPlayerName(), (a, b) -> a));

        String date = TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal());
        String dateFormat = TimeUtils.toVNDateFormat(session.getFromTime());
        String during = TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime());
        SXSSFCell totalAmtCell = null;
        Float totalAmt = Float.MIN_NORMAL;

        int currentRowI = rowIndex;
        int playerNo = 1;
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
            row.createCell(PLAYER_NO_COL).setCellValue(playerNo++);
            row.createCell(PLAYER_NAME_COL).setCellValue(avaPlayer.getPlayer().getPlayerName());
            row.createCell(PAY_AMT_COL).setCellValue(getPayAmount(avaPlayer));
            row.createCell(LEAVE_TIME_COL).setCellValue(TimeUtils.convertInstantToTimeStr(avaPlayer.getLeaveTime()));
            row.createCell(COURT_FEE_COL);

            // --- ROW N+1: set duplicated value for filter.
            rowNumber = sheet.createRow(currentRowI++);
            rowNumber.createCell(DATE_COL).setCellValue(date);
            rowNumber.createCell(DATE_FORMAT_COL).setCellValue(dateFormat);
            rowNumber.createCell(COURT_FEE_COL).setCellValue(extractCourtFee(avaPlayer.getServices()));
            rowNumber.createCell(PLAYER_NO_COL).setCellValue(playerNo - 1);
            rowNumber.createCell(PLAYER_NAME_COL).setCellValue(avaPlayer.getPlayer().getPlayerName());

            int colIndex = row.getLastCellNum();
            for (Game game : games) {
                setServiceValueIntoNextTwoColumn(game, avaPlayer, partnerMap, colIndex, row, rowNumber);
                colIndex += 2; // Move to next game pair (F, H, J...)
            }

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
        String title = TITLE_VN + TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal());
        // If you have a template, use: Workbook workbook = WorkbookFactory.create(new File("template.xlsx"));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");
        writeTitle(title, workbook, sheet);
        writeSessionHeader(sheet, workbook, session, players);

        Row tableHeader = sheet.createRow(START_HEADER_ROW_SINGLE_RPT_INDEX);
        String[] tblHeaders = {PLAYER_NO_VN, PLAYER_NAME_VN, PAY_AMOUNT_VN, LEAVE_TIME_VN, COURT_FEE_VN};
        for (int i = 0; i < tblHeaders.length; i++) tableHeader.createCell(i).setCellValue(tblHeaders[i]);

        // 3. Map for Partner Names (AvaId -> Name)
        Map<Long, String> partnerMap = players.stream().collect(
                Collectors.toMap(AvailablePlayer::getAvaId,
                        p -> p.getPlayer().getPlayerName(), (a, b) -> a));

        int currentRow = START_DATA_ROW_SINGLE_RPT_INDEX;
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
            for (Game game : games) {
                setServiceValueIntoNextTwoColumn(game, avaPlayer, partnerMap, colIndex, rowHeader, rowNumber);
                colIndex += 2; // Move to next game pair (F, H, J...)
            }
        }
        // Auto-size columns for basic info
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setAutoFilter(new CellRangeAddress(START_HEADER_ROW_SINGLE_RPT_INDEX, START_HEADER_ROW_SINGLE_RPT_INDEX,
                0, tblHeaders.length - 1));
        // Write to file
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.EXPORTING_ERROR, "");
        }
    }

    private void setServiceValueIntoNextTwoColumn(Game game, AvailablePlayer avaPlayer, Map<Long, String> partnerMap,
                                                  int colIndex, Row row, Row rowNumber) {
        try {
            Team matchingTeam = getPlayerTeamInGame(avaPlayer, game);

            String gameTitle = String.format("%s (%s - %s) %s:", game.getCourt().getCourtName(), TimeUtils.convertInstantToTimeStr(game.getCreatedDate()), TimeUtils.convertInstantToTimeStr(game.getEndedDate()), matchingTeam.isWin() ? GameConstant.WIN_VN : GameConstant.LOSE_VN);
            row.createCell(colIndex).setCellValue(gameTitle);

            // Row N: "Court (Time):" and "partner: Name"
            String partnerName = getPartnerName(avaPlayer, matchingTeam, partnerMap);

            row.createCell(colIndex + 1).setCellValue(GameConstant.PARTNER_VN.concat(CommonConstant.COLON).concat(partnerName));
            rowNumber.createCell(colIndex).setCellValue(getExpenseValue(avaPlayer, matchingTeam));
        } catch (NullPointerException e) {
            log.warn(e.getMessage());
        }

    }

    private String getPartnerName(AvailablePlayer currAvaPlayer, Team matchingTeam, Map<Long, String> partnerMap) {
        AvailablePlayer anotherPlayer = getAnotherPlayer(currAvaPlayer, matchingTeam);
        return anotherPlayer != null ? partnerMap.getOrDefault(anotherPlayer.getAvaId(), "N/A") : "N/A";
    }

    private double getExpenseValue(AvailablePlayer currAvaPlayer, Team matchingTeam) {
        try {
            return matchingTeam.getPlayerOne().equals(currAvaPlayer) ? matchingTeam.getExpenseOne() : matchingTeam.getExpenseTwo();
        } catch (NullPointerException e) {
            return 0.0;
        }

    }

    private String getAnotherPlayer(AvailablePlayer currAvaPlayer, Team matchingTeam, Map<Long, String> partnerMap) {
        AvailablePlayer anotherPlayer = matchingTeam.getPlayerOne();
        if (matchingTeam.getPlayerOne().equals(currAvaPlayer)) {
            anotherPlayer = matchingTeam.getPlayerTwo();
        }
        return partnerMap.getOrDefault(anotherPlayer.getAvaId(), "N/A");
    }

    private AvailablePlayer getAnotherPlayer(AvailablePlayer currAvaPlayer, Team matchingTeam) {
        AvailablePlayer anotherPlayer = matchingTeam.getPlayerOne();
        if (matchingTeam.getPlayerOne().equals(currAvaPlayer)) {
            anotherPlayer = matchingTeam.getPlayerTwo();
        }
        return anotherPlayer;
    }

    private void writeSessionHeader(Sheet sheet, Workbook workbook, Session session, List<AvailablePlayer> players) {
        double totalPay = players.stream().mapToDouble(p -> p.getPayAmount() != null ? p.getPayAmount() : 0).sum();

        Row r1 = sheet.createRow(START_HEADER_ROW_INDEX); // Row 3
        r1.createCell(DATE_COL).setCellValue(DATE_VN);
        r1.createCell(DATE_FORMAT_COL).setCellValue(FORMATED_DATE_VN);
        r1.createCell(DURING_COL).setCellValue(DURING_VN);
        r1.createCell(TOTAL_COL).setCellValue(TOTAL_VN);
        r1.setRowStyle(createTblStyle(workbook));

        Row r2 = sheet.createRow(START_DATA_ROW_INDEX); // Row 4
        r2.createCell(DATE_COL).setCellValue(TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal()));
        r2.createCell(DATE_FORMAT_COL).setCellValue(TimeUtils.toVNDateFormat(session.getFromTime())); // Format as needed
        r2.createCell(DURING_COL).setCellValue(TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime()));
        r2.createCell(TOTAL_COL).setCellValue(totalPay);
    }

    private void writeSessionHeader(SXSSFSheet sheet, Workbook workbook) {
        Row r1 = sheet.createRow(START_HEADER_ROW_INDEX); // Row 3
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

    private String extractCourtFee(String services) {
        if (services == null || services.isBlank()) return "0";
        // Optimized split to find the numeric part even in strings like "costInPerson-15000.0"
        String[] parts = services.split("[\\s\\-;,/]+");
        for (String part : parts) {
            if (part.matches(".*\\d.*")) return part; // Return the first part containing a number
        }
        return parts[0];
    }

    private Team getPlayerTeamInGame(AvailablePlayer p, Game g) throws NullPointerException {
        if (g.getTeamOne() != null && (p.equals(g.getTeamOne().getPlayerOne()) || p.equals(g.getTeamOne().getPlayerTwo())))
            return g.getTeamOne();
        if (g.getTeamTwo() != null && (p.equals(g.getTeamTwo().getPlayerOne()) || p.equals(g.getTeamTwo().getPlayerTwo())))
            return g.getTeamTwo();
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
        rptResponse.setGrossRevenue(getTotalGrossRevenue(s.getAvailablePlayers()));
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
        return new ReportDTO(session, availablePlayers, games);
    }

    private Float getPayAmount(AvailablePlayer availablePlayer) {
        return availablePlayer.getPayAmount() != null ? availablePlayer.getPayAmount() : Float.MIN_NORMAL;
    }
}
