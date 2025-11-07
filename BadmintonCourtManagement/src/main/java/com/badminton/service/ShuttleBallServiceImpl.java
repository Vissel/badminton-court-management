package com.badminton.service;

import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.ShuttleBall;
import com.badminton.repository.GameRepository;
import com.badminton.repository.GameShuttleMapRepository;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.ShuttleBallDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShuttleBallServiceImpl {

    @Autowired
    private ShuttleBallRepositoty ballRepo;
    @Autowired
    private GameRepository gameRepo;
    @Autowired
    private GameShuttleMapRepository gameShuttleMapRepo;
    @Autowired
    private ShuttleBallRepositoty shuttleBallRepo;

    public List<ShuttleBallDTO> getActiveShuttleBalls() {
        List<ShuttleBall> activeBalls = ballRepo.findAllByIsActive(true);
        return activeBalls.stream().map(b -> new ShuttleBallDTO(b)).collect(Collectors.toList());
    }

    public Boolean addListOfShuttleBallIntoCourt(int courtId, List<ShuttleBallDTO> listOfShuttleBall) {
        Boolean addBallSuccess = Boolean.TRUE;
        // get game by court id
        Optional<Game> optGame = gameRepo.findByCourtIdAndEndedDateIsNull(courtId);
        if (!optGame.isPresent()) {
            addBallSuccess = Boolean.FALSE;
        }
        Game game = optGame.get();
        listOfShuttleBall.stream().forEach(ballDTO -> {
                    GameShuttleMap gameShuttleMap = getExistGameShuttleBall(ballDTO, game.getShuttleMap());
                    if (gameShuttleMap != null) {
                        gameShuttleMap.setShuttleNumber(gameShuttleMap.getShuttleNumber() + 1);
                    } else {
                        game.getShuttleMap().add(createGameShuttleMap(game, ballDTO, 1));
                    }
                }
        );
        gameRepo.save(game);

        // add to list of game shuttle map if there is not exist, otherwise increasing shuttle number +1
        return addBallSuccess;
    }

    private GameShuttleMap createGameShuttleMap(Game game, ShuttleBallDTO ballDTO, int ballNumber) {
        List<ShuttleBall> shuttleBalls = shuttleBallRepo.findAllByShuttleNameAndCostAndIsActiveTrue(ballDTO.getShuttleName(), ballDTO.getShuttleCost());
        return gameShuttleMapRepo.save(new GameShuttleMap(game,
                shuttleBalls.getFirst(), ballNumber));
    }

    private GameShuttleMap getExistGameShuttleBall(ShuttleBallDTO ballDTO, List<GameShuttleMap> shuttleMap) {
        return shuttleMap.stream().filter(map -> map.getShuttleBall().theSameDTO(ballDTO)).findFirst().orElse(null);
    }

}
