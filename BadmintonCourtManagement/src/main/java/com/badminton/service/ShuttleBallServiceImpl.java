package com.badminton.service;

import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.ShuttleBall;
import com.badminton.exception.BusinessException;
import com.badminton.exception.ErrorUtils;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.GameRepository;
import com.badminton.repository.GameShuttleMapRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.requestmodel.ShuttleBallRequest;
import com.badminton.requestmodel.inventory.BallConsumeInGameRequest;
import com.badminton.requestmodel.inventory.ChangeShuttleBallQuantityRequest;
import com.badminton.response.result.Result;
import com.badminton.response.result.ShuttleBallResponse;
import com.badminton.util.ShuttleBallConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShuttleBallServiceImpl {

    @Autowired
    private ShuttleBallRepository ballRepo;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private GameRepository gameRepo;
    @Autowired
    private GameShuttleMapRepository gameShuttleMapRepo;
    @Autowired
    private ShuttleBallRepository shuttleBallRepo;
    @Autowired
    ServiceTemplate serviceTemple;
    @Autowired
    TransactionTemplate transactionTemplate;

    public List<ShuttleBallResponse> getListActiveShuttleBallDTOs() {
        List<ShuttleBall> activeBalls = ballRepo.findAllByIsActive(true);
        return activeBalls.stream().map(b -> {
            ShuttleBallResponse response = new ShuttleBallResponse(b);
            if (b.getItem() != null) {
                long stock = inventoryService.getStockOnHand(b.getItem());
                response.setStockOnHand(stock);
                response.setLowStock(stock <= 10);
            }
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Boolean addListOfShuttleBallIntoCourt(int courtId, List<ShuttleBallRequest> listBallRequest) {
        log.info("Adding List<ShuttleBallDTO> into courtId {}", courtId);
        // get game by court id
        Optional<Game> optGame = gameRepo.findByCourtIdAndEndedDateIsNull(courtId);
        if (!optGame.isPresent()) {
            return Boolean.FALSE;
        }
        List<ShuttleBallDTO> listOfShuttleBall = ShuttleBallConverter.convertRequestToDTO(listBallRequest);
        Game game = optGame.get();
        listOfShuttleBall.stream().forEach(ballDTO -> {
            GameShuttleMap gameShuttleMap = getExistGameShuttleBall(ballDTO, game.getShuttleMap());
            if (gameShuttleMap != null) {
                gameShuttleMap.setShuttleNumber(gameShuttleMap.getShuttleNumber() + ballDTO.getBallQuantity());
            } else {
                game.getShuttleMap().add(createGameShuttleMap(game, ballDTO, ballDTO.getBallQuantity()));
            }
        });
        gameRepo.save(game);
        // add list shuttle ball to inventory movement
        BallConsumeInGameRequest request = new BallConsumeInGameRequest();
        request.setInGame(game.getGameId());
        request.setShuttleBallDTOList(listOfShuttleBall);
        inventoryService.recordBallConsumption(request);

        // add to list of game shuttle map if there is not exist, otherwise increasing shuttle number +1
        return Boolean.TRUE;
    }

    public Result<Boolean> changeShuttleBallQuantity(String courtIdStr, ShuttleBallDTO ballDTO) {
        log.info("Changing ShuttleBallDTO into courtId [{}], ball [{}]", courtIdStr, ballDTO.toString());
        return serviceTemple.execute(new ProcessCallback<ShuttleBallDTO, Boolean>() {
            @Override
            public ShuttleBallDTO getRequest() {
                return ballDTO;
            }

            @Override
            public void preProcess(ShuttleBallDTO request) {
                try {
                    Integer.valueOf(courtIdStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(ErrorCodeEnum.INCORRECT_TYPE.getDescription());
                }
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        // get game by court id
                        Optional<Game> optGame = gameRepo.findByCourtIdAndEndedDateIsNull(Integer.valueOf(courtIdStr));
                        if (!optGame.isPresent()) {
                            throw new RuntimeException(ErrorUtils.buildErrorMessage(ErrorCodeEnum.GAME_NOT_FOUND));
                        }
                        Game game = optGame.get();
                        GameShuttleMap gameShuttleMap = getExistGameShuttleBall(ballDTO, game.getShuttleMap());
                        if (gameShuttleMap == null) {
                            throw new RuntimeException(ErrorUtils.buildErrorMessage(ErrorCodeEnum.SHUTTLE_BALL_NOT_FOUND));
                        }
                        int oldQuantity = gameShuttleMap.getShuttleNumber();
                        int newQuantity = ballDTO.getBallQuantity();

                        gameShuttleMap.setShuttleNumber(newQuantity);
                        gameRepo.save(game);
                        // Delegate stock adjustment to InventoryService
                        ChangeShuttleBallQuantityRequest request = new ChangeShuttleBallQuantityRequest();
                        request.setGameId(game.getGameId());
                        request.setNewQuantity(newQuantity);
                        request.setOldQuantity(oldQuantity);
                        request.setShuttleBallDTO(ballDTO);
                        Result<Boolean> result = inventoryService.changeShuttleBallQuantity(
                                toChangeShuttleBallQuantityRequest(oldQuantity, newQuantity, ballDTO, game.getGameId())
                        );
                        if (!result.isSuccess()) {
                            throw new RuntimeException("Failed to adjust shuttle ball quantity in stock");
                        }
                        return Boolean.TRUE;
                    }
                });
            }
        });
    }

    private ChangeShuttleBallQuantityRequest toChangeShuttleBallQuantityRequest(int oldQuantity, int newQuantity, ShuttleBallDTO ballDTO, int gameId) {
        ChangeShuttleBallQuantityRequest request = new ChangeShuttleBallQuantityRequest();
        request.setGameId(gameId);
        request.setOldQuantity(oldQuantity);
        request.setOldQuantity(newQuantity);
        request.setShuttleBallDTO(ballDTO);
        return request;
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
