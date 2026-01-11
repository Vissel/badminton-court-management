package com.badminton.service.impl;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import com.badminton.entity.Team;
import com.badminton.exception.BusinessException;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.GameRepository;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Override
    public byte[] exportReport(String sessionId) {
        // get list player in session
        Session session = sessionService.findSessionById(sessionId);
        Assert.notNull(session, "Current session is not found");

        List<AvailablePlayer> availablePlayers = availablePlayerRepo.findAllBySession(session);
        List<Long> avaIds = availablePlayers.stream().map(AvailablePlayer::getAvaId).toList();
        List<Game> games = gameRepo.findGamesByPlayerIds(avaIds);

        // get game

        return generateXlsxReport(session, availablePlayers, games);
    }

    public byte[] generateXlsxReport(Session session, List<AvailablePlayer> players, List<Game> games) {
        // If you have a template, use: Workbook workbook = WorkbookFactory.create(new File("template.xlsx"));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");
// 1. Session Summary Header (Rows 1-4)
        writeSessionHeader(sheet, session, players);

        // 2. Data Table Header (Row 8)
        Row tableHeader = sheet.createRow(7);
        String[] headers = {"No", "playerName", "payAmount", "leaveTime", "courtFee"};
        for (int i = 0; i < headers.length; i++) tableHeader.createCell(i).setCellValue(headers[i]);

        // 3. Map for Partner Names (AvaId -> Name)
        Map<Long, String> playerMap = players.stream()
                .collect(Collectors.toMap(AvailablePlayer::getAvaId, p -> p.getPlayer().getPlayerName(), (a, b) -> a));

        int currentRow = 8;
        int no = 1;

        for (AvailablePlayer avaPlayer : players) {
            // --- ROW N: Basic Info + Game Headers ---
            Row rowHeader = sheet.createRow(currentRow++);
            rowHeader.createCell(0).setCellValue(no++);
            rowHeader.createCell(1).setCellValue(avaPlayer.getPlayer().getPlayerName());
            rowHeader.createCell(2).setCellValue(avaPlayer.getPayAmount() != null ? avaPlayer.getPayAmount() : 0.0);
            rowHeader.createCell(3).setCellValue(TimeUtils.convertInstantToTimeStr(avaPlayer.getLeaveTime()));
            rowHeader.createCell(4).setCellValue(extractCourtFee(avaPlayer.getServices()));

            // --- ROW N+1: Empty A-E + Game Values ---
            Row rowValues = sheet.createRow(currentRow++);

            int colIndex = 5;
            for (Game game : games) {
                Team matchingTeam = getPlayerTeamInGame(avaPlayer, game);

                if (matchingTeam != null) {
                    // Row N: "Court (Time):" and "partner: Name"
                    String gameTitle = String.format("%s (%s):", game.getCourt().getCourtName(), game.getCreatedDate());
                    String partnerName = playerMap.getOrDefault(matchingTeam.getPlayerTwo(), "N/A");

                    rowHeader.createCell(colIndex).setCellValue(gameTitle);
                    rowHeader.createCell(colIndex + 1).setCellValue("partner: " + partnerName);

                    // Row N+1: Expense value
                    rowValues.createCell(colIndex).setCellValue(matchingTeam.getExpenseOne());
                } else {
                    // If player didn't play in this game, fill with 0 as per requirement
                    rowValues.createCell(colIndex).setCellValue(0);
                }
                colIndex += 2; // Move to next game pair (F, H, J...)
            }
        }
        // Auto-size columns for basic info
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try ( // ===== Export to byte[] =====
              ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSessionHeader(Sheet sheet, Session session, List<AvailablePlayer> players) {
        double totalPay = players.stream().mapToDouble(p -> p.getPayAmount() != null ? p.getPayAmount() : 0).sum();

        Row r1 = sheet.createRow(2); // Row 3
        r1.createCell(0).setCellValue("session id");
        r1.createCell(1).setCellValue("date");
        r1.createCell(2).setCellValue("during");
        r1.createCell(3).setCellValue("total");

        Row r2 = sheet.createRow(3); // Row 4
        r2.createCell(0).setCellValue(session.getSessionId());
        r2.createCell(1).setCellValue(TimeUtils.toDateDisplay(session.getFromTime(), TimeUtils.newVNLocal())); // Format as needed
        r2.createCell(2).setCellValue(TimeUtils.convertInstantsToString(session.getFromTime(), session.getToTime()));
        r2.createCell(3).setCellValue(totalPay);
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

    private Team getPlayerTeamInGame(AvailablePlayer p, Game g) {
        Long pid = p.getAvaId();
        // Matching logic based on your SQL: check if ava_id is player_id1 in either game team
        if (g.getTeamOne() != null && pid.equals(g.getTeamOne().getPlayerOne())) return g.getTeamOne();
        if (g.getTeamTwo() != null && pid.equals(g.getTeamTwo().getPlayerOne())) return g.getTeamTwo();
        return null;
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
}
