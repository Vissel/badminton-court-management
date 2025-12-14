package com.badminton.service;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.entity.Game;
import com.badminton.entity.Team;
import com.badminton.exception.BusinessException;
import com.badminton.exception.ErrorMess;
import com.badminton.exception.enums.BizCode;
import com.badminton.repository.GameRepository;
import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.ShuttleBallDTO;
import com.badminton.response.result.*;
import com.badminton.service.calculator.GameExpenseCalculator;
import com.badminton.util.CommonUtil;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    @Autowired
    GameRepository gameRepository;
    @Autowired
    GameExpenseCalculator gameExpenseCalculator;

    /**
     * Get a GameResult when user click on Finish btn.
     * <br> In this time, get the shuttle ball number and quantityNot calculate the money yet. </br>
     * <br> Front-end will base on money, ball's  quantity to calculate.
     * <br> Human verify, do adjustment and   click confirm after that >> next api </br>
     *
     * @param courtId
     * @return
     */

    public GameResult getGameResult(int courtId) {
        GameResult result = new GameResult();
        Optional<Game> optGame = gameRepository.findByCourtIdAndEndedDateIsNull(courtId);
        if (optGame.isPresent()) {
            Game startedGame = optGame.get();
            result.setGameId(startedGame.getGameId());
            result.setCourtResult(new CourtResult(startedGame.getCourt().getCourtId(), startedGame.getCourt().getCourtName()));
            final Map<ShuttleBallResult, Integer> ballResMap = ServiceUtil.retrievedShuttleBallMap(startedGame.getShuttleMap());
            result.setBallResultMap(ballResMap);
            result.setTeamOneResult(buildTeamResult(startedGame.getTeamOne(), ballResMap, startedGame));
            result.setTeamTwoResult(buildTeamResult(startedGame.getTeamTwo(), ballResMap, startedGame));
            result.setCreatedDate(startedGame.getCreatedDate());
            result.setEndedDate(startedGame.getEndedDate());
            result.setState(startedGame.getState());
        }
        return result;
    }

    public Result handleFinishGame(GameDTO gameRequest) {
        Result result = new Result();
        String errorMess = null;
        try {
            // 1. validate Game confirmation value
            validateGameFinishField(gameRequest);

            // 2. get Game
            int courtId = Integer.valueOf(gameRequest.getCourt().getCourtId());
            Optional<Game> gOption = gameRepository.findByCourtIdAndEndedDateIsNull(courtId);
            if (!gOption.isPresent()) {
                errorMess = "There is no Game with court id:" + gameRequest.getCourt().getCourtId();
                throw new BusinessException(BizCode.GAME_NOT_FOUND, errorMess);
            }
            Game game = gOption.get();
            // 3. validate from request the type of game: SHARE or NEGO and set expense
            findGTypeAndSetExpense(game, gameRequest.getCourt().getCourtAreas());
            // 4. set value: state, Team's expense, endedDate, gType.
            game.setEndedDate(ServiceUtil.getCurrentTimeStamp());
            game.setState(GameState.FINISH.getValue());

            gameRepository.save(game);
            result.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(ErrorMess.REQUEST_VAL, e.getMessage());
            errorMess = e.getMessage();
            result.setSuccess(false);
        } catch (BusinessException e) {
            log.error(ErrorMess.BIZ_VAL, e.getMessage());
            result.setSuccess(false);
        } catch (Exception e) {
            log.error(e.getMessage());
            result.setSuccess(false);
        } finally {
            result.setErrorMessage(errorMess);
        }
        return result;
    }

    private TeamResult buildTeamResult(Team team, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
        TeamResult teamResult = new TeamResult();
        Float totalBallCost = gameExpenseCalculator.getTotalBallCost(shuttleMap);

//        teamResult.setWin(ServiceUtil.getGameStatus(team.isWin()));
        if (ServiceUtil.availablePlayerNotNull(team.getPlayerOne())) {
            teamResult.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(team));
        }
        if (ServiceUtil.availablePlayerNotNull(team.getPlayerTwo())) {
            teamResult.setPlayerTwoName(ServiceUtil.getPlayerTwoNameBy(team));
        }
//        setExpenseIfShareLose(startedGame.getGtype(), team.isWin(), totalBallCost, teamResult);
        return teamResult;
    }

    //    private TeamResult buildTeamTwoResult(CourtDTO court, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        List<CourtAreaDTO> teamOneList = court.getCourtAreas().stream().filter(area -> GameState.Player.PLAYER_A.equals(area.getArea()) || GameState.Player.PLAYER_B.equals(area.getArea()))
//                .collect(Collectors.toList());
//        return buildTeamResult(teamOneList, shuttleMap, startedGame);
//    }
//
//    private TeamResult buildTeamOneResult(CourtDTO court, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        List<CourtAreaDTO> teamTwoList = court.getCourtAreas().stream().filter(area -> GameState.Player.PLAYER_C.equals(area.getArea()) || GameState.Player.PLAYER_D.equals(area.getArea()))
//                .collect(Collectors.toList());
//        return buildTeamResult(teamTwoList, shuttleMap, startedGame);
//    }
//
//    private TeamResult buildTeamResult(List<CourtAreaDTO> teamList, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        TeamResult teamResult = new TeamResult();
//
//        Float totalBallCost = gameExpenseCalculator.getTotalBallCost(shuttleMap);
//        float remainBallCost = totalBallCost / GameConstant.BISECT;
//        for (CourtAreaDTO courtAreaDTO : teamList) {
//            switch (courtAreaDTO.getArea()) {
//                case GameState.Player.PLAYER_A, GameState.Player.PLAYER_C:
//                    teamResult.setPlayerOneName(ServiceUtil.getPlayerInArea(courtAreaDTO));
//                    setPlayerOneExpenseIfShareLose(startedGame.getGtype(), courtAreaDTO.isWin(), remainBallCost, teamResult);
//                    break;
//                case GameState.Player.PLAYER_B, GameState.Player.PLAYER_D:
//                    teamResult.setPlayerTwoName(ServiceUtil.getPlayerInArea(courtAreaDTO));
//                    setPlayerTwoExpenseIfShareLose(startedGame.getGtype(), courtAreaDTO.isWin(), remainBallCost, teamResult);
//                    break;
//                default:
//                    break;
//            }
//            teamResult.setWin(ServiceUtil.getGameStatus(courtAreaDTO.isWin()));
//        }
//
//        return teamResult;
//    }

    private Map<String, CourtAreaDTO> mapAreaStrKey(List<CourtAreaDTO> listCourtAreas) {
        return listCourtAreas.stream().filter(Objects::nonNull).collect(Collectors.toMap(
                        CourtAreaDTO::getArea,
                        a -> a
                )
        );
    }


    private void findGTypeAndSetExpense(Game game, List<CourtAreaDTO> listCourtAreas) throws BusinessException {
        Map<String, CourtAreaDTO> areaCourtMap = mapAreaStrKey(listCourtAreas);
        String winAreaDTO = findWinAreaDTO(areaCourtMap);
        GameType gType = findGameType(areaCourtMap);
        // set gType to game
        game.setGtype(gType.name());
        if (GameType.SHARE.equals(gType)) {
            Team loseTeam = findLoseTeam(game, areaCourtMap.get(winAreaDTO));
            setSharedExpenseToLoseTeam(gameExpenseCalculator.getTotalBallCost(game), loseTeam);
        } else {
            // don't need to calculate expense, negotiation has been done from FE by admin's input
            setNegotiateExpense(game, areaCourtMap);
        }
    }

    private GameType findGameType(Map<String, CourtAreaDTO> areaCourtMap) {
        for (Map.Entry<String, CourtAreaDTO> entry : areaCourtMap.entrySet()) {
            if (entry.getValue().isWin()) {
                if (entry.getValue().getPlayerInArea().getExpense() != 0.0f) {
                    return GameType.NEGO;
                }
            }
        }
        return GameType.SHARE;
    }

    private String findWinAreaDTO(Map<String, CourtAreaDTO> areaCourtMap) throws BusinessException {
        Map.Entry<String, CourtAreaDTO> entry = areaCourtMap.entrySet().stream().filter(map -> map.getValue().isWin()).findFirst()
                .orElseThrow(() -> new BusinessException(BizCode.WINNER_NOT_FOUND, "No specific winner."));
        return entry.getKey();
    }

    private Team findLoseTeam(Game game, CourtAreaDTO winAreaDTO) {
        if (ServiceUtil.isTeamOne(winAreaDTO.getArea())) {
            return game.getTeamTwo();
        }
        return game.getTeamOne();
    }

    private void setSharedExpenseToLoseTeam(float totalBallCost, Team loseTeam) {
        float sharedExpense = (float) totalBallCost / 2;
        loseTeam.setExpenseOne(sharedExpense);
        loseTeam.setExpenseTwo(sharedExpense);
    }

    private void setNegotiateExpense(Game game, Map<String, CourtAreaDTO> areaCourtMap) {
        float expense;
        for (Map.Entry<String, CourtAreaDTO> entry : areaCourtMap.entrySet()) {
            expense = entry.getValue().getPlayerInArea().getExpense();
            findTeamAndSetExpense(game, entry.getKey(), expense);
        }
    }

    private void findTeamAndSetExpense(Game game, String areaDTO, float expense) {
        switch (areaDTO) {
            case GameState.Player.PLAYER_A -> game.getTeamOne().setExpenseOne(expense);
            case GameState.Player.PLAYER_B -> game.getTeamOne().setExpenseTwo(expense);
            case GameState.Player.PLAYER_C -> game.getTeamTwo().setExpenseOne(expense);
            case GameState.Player.PLAYER_D -> game.getTeamTwo().setExpenseTwo(expense);
        }
    }

    private void validateGameFinishField(GameDTO gameRequest) {
        Assert.notNull(gameRequest.getCourt(), "Court must not be null.");
        Assert.isTrue(StringUtils.isNotBlank(gameRequest.getCourt().getCourtId()), "Court id must not be empty.");

        Integer.valueOf(gameRequest.getCourt().getCourtId());

        Assert.isTrue(StringUtils.isNotBlank(gameRequest.getCourt().getCourtName()), "Court name must not be empty.");
        Assert.notEmpty(gameRequest.getCourt().getCourtAreas(), "Court area must not be empty.");
        Assert.notEmpty(gameRequest.getShuttleMap(), "Shuttle ball must not be empty.");
        boolean isValidTeamPlayers = haveValidTeamPlayers(gameRequest.getCourt().getCourtAreas(), getTotalBallExpense(gameRequest.getShuttleBalls()));
        Assert.isTrue(isValidTeamPlayers, "Player or expense is invalid.");
    }

    private Float getTotalBallExpense(List<ShuttleBallDTO> shuttleBalls) {
        return shuttleBalls.stream().map(b -> b.getShuttleCost() * b.getBallQuantity()).reduce(0f, Float::sum);
    }

    private boolean haveValidTeamPlayers(List<CourtAreaDTO> courtAreas, float totalBallExpense) {
        boolean hasWinner = false;
        int numTeamOne = 0;
        int numTeamTwo = 0;
        float actualExpenseTotal = 0.0f;
        for (CourtAreaDTO area : courtAreas) {
            if (validCourtArea(area)) {
                hasWinner = checkWinner(area.isWin(), hasWinner);
                numTeamOne += checkTeamOne(area);
                numTeamTwo += checkTeamTwo(area);
                actualExpenseTotal += area.getPlayerInArea().getExpense();
            }
        }

        return validateTeamPlayer(hasWinner, numTeamOne, numTeamTwo, totalBallExpense, actualExpenseTotal);
    }

    private boolean validateTeamPlayer(boolean hasWinner, int numTeamOne, int numTeamTwo, float totalBallExpense, float actualExpenseTotal) {
        return hasWinner && numTeamOne == numTeamTwo && totalBallExpense == actualExpenseTotal;
    }

    private int checkTeamTwo(CourtAreaDTO area) {
        if (ServiceUtil.isTeamTwo(area.getArea())) {
            return 1;
        }
        return 0;
    }

    private int checkTeamOne(CourtAreaDTO area) {
        if (ServiceUtil.isTeamOne(area.getArea())) {
            return 1;
        }
        return 0;
    }

    private boolean checkWinner(boolean win, boolean hasWinner) {
        if (!hasWinner && win) {
            return true;
        }
        return hasWinner;
    }

    private boolean validCourtArea(CourtAreaDTO area) {
        return area != null &&
                area.getPlayerInArea() != null && CommonUtil.isNotNullEmpty(area.getArea(), area.getPlayerInArea().getPlayerName());
    }
}
