package com.badminton.core.player;

import com.badminton.core.payment.PayStatus;
import com.badminton.core.session.CoreSession;
import com.badminton.dao.DaoPlayer;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.exception.validation.AvailablePlayerCheck;
import com.badminton.model.dto.AvailablePlayerPayDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.player.UpdatePlayerDTO;
import com.badminton.model.player.AvailablePlayerPayModel;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.UserRepository;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoreAvailablePlayerService {
    @Autowired
    AvailablePlayerRepository availablePlayerRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CoreSession coreSession;

    @Autowired
    DaoPlayer daoPlayer;

    /**
     *
     * @param currName
     * @param newName
     * @return
     */
    public Boolean updateAvailablePlayerName(String currName, String newName) {
        List<Session> activeSessions = coreSession.findListCurrentSession();
        if (activeSessions.isEmpty()) {
            return false;
        }
        log.info("Session is found");
        Session currentSession = activeSessions.getFirst();
        Optional<AvailablePlayer> availablePlayerOpt = availablePlayerRepository
                .findForUpdateAvailablePlayerInSessionByName(currentSession, currName);

        if (availablePlayerOpt.isPresent()) {
            AvailablePlayer availablePlayer = availablePlayerOpt.get();

            List<Player> players = userRepository.findAllByPlayerName(newName);
            Player newPlayer;
            if (!players.isEmpty()) {
                newPlayer = players.getFirst();
            } else {
                newPlayer = new Player(newName, newName);
                userRepository.save(newPlayer);
            }

            availablePlayer.setPlayer(newPlayer);
            availablePlayerRepository.save(availablePlayer);
            return true;
        }

        return false;
    }

    public AvailablePlayer getAvailablePlayerByName(Session session, String name) {
        List<AvailablePlayer> players = daoPlayer.getAllAvailablePlayerBySessionAndName(session, name);
        // Prefer the row still present in session; fall back to the earliest row.
        return players.stream()
                .filter(p -> p.getLeaveTime() == null)
                .findFirst()
                .orElse(players.getFirst());
    }

    public Player checkAvailableAndGetPlayer(Session session, String name) {
        return getAvailablePlayerByName(session, name).getPlayer();
    }

    public Optional<AvailablePlayer> findAvailableByNameInSession(String name) {
        return availablePlayerRepository.findAvailablePlayerInSessionByNameAndLeaveTimeNull(coreSession.findCurrentSession(), name);
    }

    public Optional<AvailablePlayer> findForUpdateAvailablePlayerInSessionByName(String name) {
        return availablePlayerRepository.findForUpdateAvailablePlayerInSessionByName(coreSession.findCurrentSession(), name);
    }

    /**
     *
     * @param dto
     * @return
     * @throws BusinessException
     */
    public AvailablePlayerPayModel updatePaidAvailablePlayer(AvailablePlayerPayDTO dto) throws BusinessException {
        Optional<AvailablePlayer> optPlayer = findForUpdateAvailablePlayerInSessionByName(
                dto.getName());
        AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);
        AvailablePlayer availablePlayer = optPlayer.get();

        availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(dto.getListService()));
        availablePlayer.setLeaveTime(coreSession.getUTCPlus7Instant());
        availablePlayer.setPayType(dto.getPayType());
        availablePlayer.setPayAmount(dto.getPayAmount());

        AvailablePlayer savedPlayer = availablePlayerRepository.save(availablePlayer);
        return convertToModel(savedPlayer, PayStatus.PAID);
    }

    /**
     *
     * @param dto
     * @return
     * @throws BusinessException
     */
    public AvailablePlayerPayModel updateUnPaidAvailablePlayer(AvailablePlayerPayDTO dto) throws BusinessException {
        if (coreSession.findListCurrentSession().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.CURRENT_SESSION_NOT_FOUND);
        }
        Optional<AvailablePlayer> optPlayer = findForUpdateAvailablePlayerInSessionByName(
                dto.getName());
        AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);

        AvailablePlayer availablePlayer = optPlayer.get();
        availablePlayer.setLeaveTime(coreSession.getUTCPlus7Instant());
        availablePlayer.setIsCanceled(Boolean.TRUE);
        AvailablePlayer savedPlayer = availablePlayerRepository.save(availablePlayer);
        return convertToModel(savedPlayer, PayStatus.UNPAID);
    }

    private AvailablePlayerPayModel convertToModel(AvailablePlayer savedPlayer, PayStatus payStatus) {
        return new AvailablePlayerPayModel(
                savedPlayer.getPlayer().getPlayerName(),
                savedPlayer.getPayType(),
                BigDecimal.valueOf(savedPlayer.getPayAmount()),
                savedPlayer.getLeaveTime(),
                savedPlayer.getServices(),
                payStatus);
    }

    /**
     * update available player with service change.
     * <p>
     * A line that is already on the player keeps its place and takes the requested cost, so a
     * repeated edit (e.g. a rental duration change) refreshes one line instead of stacking
     * duplicates.
     *
     * @param updateAvaPlayerDTO
     */
    public void updateAvaPlayer(UpdatePlayerDTO updateAvaPlayerDTO) {
        List<ServiceDTO> newServices = Optional.ofNullable(updateAvaPlayerDTO.getListServiceDTO())
                .orElseGet(List::of);
        findForUpdateAvailablePlayerInSessionByName(updateAvaPlayerDTO.getPlayerName())
                .ifPresent(availablePlayer -> {
                    List<ServiceDTO> currServices = new ArrayList<>(
                            ServiceUtil.convertStringToListService(availablePlayer.getCurrentServices()));
                    newServices.forEach(newService -> upsertService(currServices, newService));
                    availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(currServices));
                    availablePlayerRepository.save(availablePlayer);
                });
    }

    /**
     * Replace the cost of the line carrying the same service name, or append the line.
     */
    private void upsertService(List<ServiceDTO> services, ServiceDTO newService) {
        services.stream()
                .filter(existing -> Objects.equals(existing.getServiceName(), newService.getServiceName()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setCost(newService.getCost()),
                        () -> services.add(newService));
    }

    /**
     * drop the service lines carrying one of the requested service names.
     *
     * @param updateAvaPlayerDTO
     */
    public void removeAvaPlayer(UpdatePlayerDTO updateAvaPlayerDTO) {
        List<String> names = Optional.ofNullable(updateAvaPlayerDTO.getListServiceDTO())
                .orElseGet(List::of)
                .stream()
                .map(ServiceDTO::getServiceName)
                .collect(Collectors.toList());
        if (names.isEmpty()) {
            return;
        }
        findForUpdateAvailablePlayerInSessionByName(updateAvaPlayerDTO.getPlayerName())
                .ifPresent(availablePlayer -> {
                    List<ServiceDTO> remaining = ServiceUtil
                            .convertStringToListService(availablePlayer.getCurrentServices())
                            .stream()
                            .filter(service -> !names.contains(service.getServiceName()))
                            .collect(Collectors.toCollection(ArrayList::new));
                    availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(remaining));
                    availablePlayerRepository.save(availablePlayer);
                });
    }
}
