package com.badminton.service;

import com.badminton.repository.GameRepository;
import com.badminton.requestmodel.SessionRequest;
import com.badminton.response.result.Result;
import com.badminton.response.result.SessionResult;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerService {

    @Autowired
    SessionServiceImpl sessionService;

    @Autowired
    GameRepository gameRepository;

    public ResponseEntity<Result<SessionResult>> closeOutDateSession() {
        log.info("Start service removeRedundantSession");
        Result<SessionResult> data = sessionService.closeOutDateSession(new SessionRequest(true));

//        log.info("check games which are not Finish or Cancel in the same date and Cancel them.");
//        List<GameResult> gameResultList = new ArrayList<>();
//        // check games which are not Finish or Cancel in the same date and Cancel them.
//        Set<String> ongoingState = GameState.getNotTerminateState();
//        List<Game> ongoingGame = gameRepository.findAllByStateInAndEndedDateIsNull(ongoingState);
//        final Timestamp endedDate = ServiceUtil.getCurrentTimeStamp();
//        ongoingGame.stream().forEach(g -> {
//                    g.setState(GameState.CANCEL.getValue());
//                    g.setEndedDate(endedDate);
//                    gameResultList.add(Converter.convertorToGameResult(g));
//                }
//        );
//        gameRepository.saveAll(ongoingGame);
//        dataResult.setGameResultList(gameResultList);
//
//        log.info("de-active all past sessions.");
//        // delete session in day
//        List<SessionResult> sessionResultList = sessionService.deactiveSessions();
//        dataResult.setSessionResultList(sessionResultList);
        log.info("End service removeRedundantSession");
        return ResponseConvertor.convert(data);
    }
}
