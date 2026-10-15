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
import com.badminton.model.player.AvailablePlayerPayModel;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.UserRepository;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

}
