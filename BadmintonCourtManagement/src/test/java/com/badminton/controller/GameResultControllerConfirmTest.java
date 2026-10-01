package com.badminton.controller;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Court;
import com.badminton.entity.Game;
import com.badminton.entity.Player;
import com.badminton.entity.Team;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.GameRepository;
import com.badminton.requestmodel.AvaPlayerDTO;
import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.requestmodel.CourtDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.response.result.Result;
import com.badminton.service.ServiceTemplate;
import com.badminton.service.impl.GameServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers every flow and branch of {@link GameServiceImpl#handleFinishGame(GameDTO)}
 * through the REST entry point {@link GameResultController#confirmGameResult(GameDTO)}.
 */
@ExtendWith(MockitoExtension.class)
class GameResultControllerConfirmTest {

    private static final String COURT_ID = "1";
    private static final String COURT_NAME = "Court 1";

    @Mock
    private GameRepository gameRepository;

    private GameResultController controller;

    @BeforeEach
    void setUp() {
        GameServiceImpl gameService = new GameServiceImpl();
        ReflectionTestUtils.setField(gameService, "gameRepository", gameRepository);
        ReflectionTestUtils.setField(gameService, "serviceTemple", new ServiceTemplate());
        controller = new GameResultController();
        controller.gameService = gameService;
    }

    // ------------------------------------------------------------------
    // Validation failures (preProcess) — all Assert branches
    // ------------------------------------------------------------------

    @Test
    @DisplayName("rejects when court is null")
    void rejectWhenCourtNull() {
        GameDTO request = baseRequest();
        request.setCourt(null);

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Court must not be null.");
    }

    @Test
    @DisplayName("rejects when court id is blank")
    void rejectWhenCourtIdBlank() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtId(" ");

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Court id must not be empty.");
    }

    @Test
    @DisplayName("rejects when court id is not a number")
    void rejectWhenCourtIdNotNumeric() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtId("abc");

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejects when court name is blank")
    void rejectWhenCourtNameBlank() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtName("");

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Court name must not be empty.");
    }

    @Test
    @DisplayName("rejects when court areas are empty")
    void rejectWhenCourtAreasEmpty() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(List.of());

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Court area must not be empty.");
    }

    @Test
    @DisplayName("rejects when shuttle balls are empty")
    void rejectWhenShuttleBallsEmpty() {
        GameDTO request = baseRequest();
        request.setShuttleBalls(List.of());

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Shuttle ball must not be empty.");
    }

    @Test
    @DisplayName("rejects when no winner is marked in any court area")
    void rejectWhenNoWinner() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(List.of(
                area(GameState.Player.PLAYER_A, "P1", 50f, false),
                area(GameState.Player.PLAYER_C, "P2", 50f, false)));

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Player or expense is invalid.");
    }

    @Test
    @DisplayName("rejects when teams are unbalanced and junk areas are ignored")
    void rejectWhenTeamsUnbalanced() {
        List<CourtAreaDTO> areas = new ArrayList<>();
        areas.add(area(GameState.Player.PLAYER_A, "P1", 50f, true));
        areas.add(area(GameState.Player.PLAYER_B, "P2", 50f, false));
        areas.add(area(GameState.Player.PLAYER_C, "P3", 0f, false));
        areas.add(null);                                                          // null element ignored
        areas.add(area(GameState.Player.PLAYER_D, null, 0f, false));              // no player in area
        areas.add(area("Z", " ", 0f, false));                                     // blank player name, unknown area
        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(areas);

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Player or expense is invalid.");
    }

    @Test
    @DisplayName("rejects when area expenses do not match total ball expense")
    void rejectWhenExpenseMismatch() {
        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(List.of(
                area(GameState.Player.PLAYER_A, "P1", 40f, true),
                area(GameState.Player.PLAYER_C, "P2", 40f, false)));

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertBadRequest(res, "Player or expense is invalid.");
    }

    // ------------------------------------------------------------------
    // Game lookup
    // ------------------------------------------------------------------

    @Test
    @DisplayName("returns GAME_NOT_FOUND when no active game on the court")
    void gameNotFound() {
        when(gameRepository.findByCourtIdAndEndedDateIsNull(anyInt())).thenReturn(Optional.empty());

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(baseRequest());

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getErrorCode()).isEqualTo(100); // GAME_NOT_FOUND
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        verify(gameRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    // ------------------------------------------------------------------
    // Success paths — player availability variants per court area
    // ------------------------------------------------------------------

    @Test
    @DisplayName("singles SHARE game: winner expense 0, unknown/null areas ignored")
    void finishShareSingles() {
        Game game = game("A-player", null, "C-player", null);
        when(gameRepository.findByCourtIdAndEndedDateIsNull(anyInt())).thenReturn(Optional.of(game));

        List<CourtAreaDTO> areas = new ArrayList<>();
        areas.add(area(GameState.Player.PLAYER_C, "C-player", 100f, false)); // loser first
        areas.add(area(GameState.Player.PLAYER_A, "A-player", 0f, true));    // winner expense 0 -> SHARE
        areas.add(null);                                                     // null element filtered out
        areas.add(area("X", "Ghost", 0f, false));                            // unknown area -> switch default
        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(areas);

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData()).isTrue();

        assertThat(game.getState()).isEqualTo(GameState.FINISH.getValue());
        assertThat(game.getEndedDate()).isNotNull();
        assertThat(game.getGtype()).isEqualTo(GameType.SHARE.name());
        assertThat(game.getTeamOne().isWin()).isTrue();
        assertThat(game.getTeamOne().getExpenseOne()).isEqualTo(0f);
        assertThat(game.getTeamTwo().isWin()).isFalse();
        assertThat(game.getTeamTwo().getExpenseOne()).isEqualTo(100f);
        // only the loser with expense > 0 gets the "Tiền" service line appended
        assertThat(game.getTeamTwo().getPlayerOne().getServices()).contains("Tiền " + COURT_NAME);
        assertThat(game.getTeamOne().getPlayerOne().getServices()).isNull();
        verify(gameRepository).save(game);
    }

    @Test
    @DisplayName("doubles NEGO game: all four areas filled, winners carry expense")
    void finishNegoDoubles() {
        Game game = game("A-player", "B-player", "C-player", "D-player");
        when(gameRepository.findByCourtIdAndEndedDateIsNull(anyInt())).thenReturn(Optional.of(game));

        GameDTO request = baseRequest();
        request.setShuttleBalls(List.of(ball(100f, 1)));
        request.getCourt().setCourtAreas(List.of(
                area(GameState.Player.PLAYER_C, "C-player", 40f, false),
                area(GameState.Player.PLAYER_A, "A-player", 20f, true),
                area(GameState.Player.PLAYER_D, "D-player", 20f, false),
                area(GameState.Player.PLAYER_B, "B-player", 20f, true)));

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();

        assertThat(game.getGtype()).isEqualTo(GameType.NEGO.name());
        assertThat(game.getTeamOne().getExpenseOne()).isEqualTo(20f);
        assertThat(game.getTeamOne().getExpenseTwo()).isEqualTo(20f);
        assertThat(game.getTeamOne().isWin()).isTrue();
        assertThat(game.getTeamTwo().getExpenseOne()).isEqualTo(40f);
        assertThat(game.getTeamTwo().getExpenseTwo()).isEqualTo(20f);
        assertThat(game.getTeamTwo().isWin()).isFalse();
        // every occupied area had expense > 0 -> all four players billed
        assertThat(game.getTeamOne().getPlayerOne().getServices()).contains("Tiền " + COURT_NAME);
        assertThat(game.getTeamOne().getPlayerTwo().getServices()).contains("Tiền " + COURT_NAME);
        assertThat(game.getTeamTwo().getPlayerOne().getServices()).contains("Tiền " + COURT_NAME);
        assertThat(game.getTeamTwo().getPlayerTwo().getServices()).contains("Tiền " + COURT_NAME);
        verify(gameRepository).save(game);
    }

    @Test
    @DisplayName("unavailable player in area B with 0 expense still finishes the game")
    void finishWithMissingPlayerZeroExpense() {
        // entity teamOne.playerTwo is null even though the request fills area B
        Game game = game("A-player", null, "C-player", "D-player");
        when(gameRepository.findByCourtIdAndEndedDateIsNull(anyInt())).thenReturn(Optional.of(game));

        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(List.of(
                area(GameState.Player.PLAYER_A, "A-player", 0f, true),
                area(GameState.Player.PLAYER_B, "B-player", 0f, true),
                area(GameState.Player.PLAYER_C, "C-player", 60f, false),
                area(GameState.Player.PLAYER_D, "D-player", 40f, false)));

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(game.getState()).isEqualTo(GameState.FINISH.getValue());
        assertThat(game.getTeamOne().getExpenseTwo()).isEqualTo(0f);
        verify(gameRepository).save(game);
    }

    @Test
    @DisplayName("unavailable player in area C with expense > 0 surfaces as server error (NPE)")
    void finishWithMissingPlayerNonZeroExpense() {
        // entity teamTwo.playerOne is null but request bills area C
        Game game = game("A-player", null, null, null);
        when(gameRepository.findByCourtIdAndEndedDateIsNull(anyInt())).thenReturn(Optional.of(game));

        GameDTO request = baseRequest();
        request.getCourt().setCourtAreas(List.of(
                area(GameState.Player.PLAYER_A, "A-player", 0f, true),
                area(GameState.Player.PLAYER_C, "C-player", 100f, false)));

        ResponseEntity<Result<Boolean>> res = controller.confirmGameResult(request);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getErrorCode()).isEqualTo(500);
        assertThat(res.getStatusCode().value()).isEqualTo(500);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void assertBadRequest(ResponseEntity<Result<Boolean>> res, String message) {
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getErrorCode()).isEqualTo(400);
        assertThat(res.getBody().getErrorMessage()).contains(message);
        verifyNoInteractions(gameRepository);
    }

    /** Balanced singles request: A wins, C loses, expenses match 2 x 50 ball cost. */
    private GameDTO baseRequest() {
        CourtDTO court = new CourtDTO();
        court.setCourtId(COURT_ID);
        court.setCourtName(COURT_NAME);
        court.setCourtAreas(new ArrayList<>(List.of(
                area(GameState.Player.PLAYER_A, "P1", 0f, true),
                area(GameState.Player.PLAYER_C, "P2", 100f, false))));

        GameDTO request = new GameDTO();
        request.setCourt(court);
        request.setShuttleBalls(List.of(ball(50f, 2)));
        request.setGameState(GameState.FINISH.getValue());
        return request;
    }

    private CourtAreaDTO area(String area, String playerName, float expense, boolean win) {
        AvaPlayerDTO player = null;
        if (playerName != null) {
            player = new AvaPlayerDTO();
            player.setPlayerName(playerName);
            player.setExpense(expense);
        }
        CourtAreaDTO dto = new CourtAreaDTO();
        dto.setArea(area);
        dto.setPlayerInArea(player);
        dto.setWin(win);
        return dto;
    }

    private ShuttleBallDTO ball(float cost, int quantity) {
        ShuttleBallDTO ball = new ShuttleBallDTO();
        ball.setShuttleCost(cost);
        ball.setBallQuantity(quantity);
        return ball;
    }

    /** Builds a game whose four area slots may each be available (name) or unavailable (null). */
    private Game game(String playerA, String playerB, String playerC, String playerD) {
        Team teamOne = new Team();
        teamOne.setPlayerOne(availablePlayer(playerA));
        teamOne.setPlayerTwo(availablePlayer(playerB));
        Team teamTwo = new Team();
        teamTwo.setPlayerOne(availablePlayer(playerC));
        teamTwo.setPlayerTwo(availablePlayer(playerD));

        Court court = new Court();
        court.setCourtName(COURT_NAME);

        Game game = new Game();
        game.setCourt(court);
        game.setTeamOne(teamOne);
        game.setTeamTwo(teamTwo);
        game.setState(GameState.START.getValue());
        return game;
    }

    private AvailablePlayer availablePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player player = new Player();
        player.setPlayerName(name);
        return new AvailablePlayer(player);
    }
}
