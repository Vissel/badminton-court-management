package com.badminton.service;

import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.ShuttleBall;
import com.badminton.repository.GameRepository;
import com.badminton.repository.GameShuttleMapRepository;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.ShuttleBallDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

    public List<ShuttleBallDTO> getListActiveShuttleBallDTOs() {
        List<ShuttleBall> activeBalls = ballRepo.findAllByIsActive(true);
        return activeBalls.stream().map(b -> new ShuttleBallDTO(b)).collect(Collectors.toList());
    }

    public Boolean addListOfShuttleBallIntoCourt(int courtId, List<ShuttleBallDTO> listOfShuttleBall) {
        log.info("Adding List<ShuttleBallDTO> into courtId {}", courtId);
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
        });
        gameRepo.save(game);

        // add to list of game shuttle map if there is not exist, otherwise increasing shuttle number +1
        return addBallSuccess;
    }

    public List<ShuttleBall> findAllByNameAndCost(String shuttleName, float shuttleCost) {
        return shuttleBallRepo.findAllByShuttleNameAndCostAndIsActiveTrue(shuttleName, shuttleCost);
    }

    public GameShuttleMap createGameShuttleMap(Game game, ShuttleBallDTO shuttleBallDTO, int ballNumber) {
        log.info("Creating GameShuttleMap.");
        List<ShuttleBall> shuttleBalls = findAllByNameAndCost(shuttleBallDTO.getShuttleName(), shuttleBallDTO.getShuttleCost());
        return gameShuttleMapRepo.save(new GameShuttleMap(game, shuttleBalls.getFirst(), ballNumber));
    }

    private GameShuttleMap getExistGameShuttleBall(ShuttleBallDTO shuttleBallDTO, List<GameShuttleMap> shuttleMap) {
        return shuttleMap.stream().filter(map -> map.getShuttleBall().theSameDTO(shuttleBallDTO)).findFirst().orElse(null);
    }

    public void updateShuttleBall(ShuttleBallDTO shuttleBallDTO) {
        log.info("Updating GameShuttleMap.");
        ShuttleBall shuttleBall = findAllByNameAndCost(shuttleBallDTO.getShuttleName(), shuttleBallDTO.getShuttleCost()).getFirst();
        shuttleBall.setSelected(shuttleBallDTO.isSelected());
        shuttleBallRepo.save(shuttleBall);
    }

    public void changeSelectedShuttleBall(ShuttleBallDTO selectedShuttleBallDTO) {
        Optional<ShuttleBall> prevSelectedBallOpt = shuttleBallRepo.findByIsSelectedTrueAndIsActiveTrue();
        if (prevSelectedBallOpt.isPresent()) {
            ShuttleBall prevSelectedBall = prevSelectedBallOpt.get();
            prevSelectedBall.setSelected(false);
            shuttleBallRepo.save(prevSelectedBall);
        }
        updateShuttleBall(selectedShuttleBallDTO);
    }
}
