package com.badminton.core.debit;

import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.entity.*;
import com.badminton.enums.DebitStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.debit.DebitModel;
import com.badminton.model.debit.RemainingDebitModel;
import com.badminton.model.dto.DebitDTO;
import com.badminton.repository.*;
import com.badminton.requestmodel.Pagination;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.response.debit.DebitResponse;
import com.badminton.response.debit.DebitSummaryResponse;
import com.badminton.response.debit.MoneyResponse;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.MoneyUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CoreDebitService {
    @Autowired
    private DebitSummaryRepository debitSummaryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DebitRepository debitRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentDebitRepository paymentDebitRepository;
    @Autowired
    private SessionServiceImpl sessionService;
    @Autowired
    private CoreAvailablePlayerService coreAvailablePlayerService;

    @Transactional
    public Boolean createDebit(DebitRequest request) throws BusinessException {
        Pair<Player, Session> playerAndSession = getPlayerAndSession(request.getPlayerName(), request.getCreatedTime());

        Debit debit = new Debit();
        BigDecimal debtAmount = new BigDecimal(request.getDebitAmount());
        debit.setDebtAmount(debtAmount);
        debit.setRemainingAmount(debtAmount);
        debit.setCurrency(request.getCurrency() != null ? request.getCurrency() : MoneyUtils.CURRENCY_VN);
        debit.setStatus(DebitStatus.PENDING);
        debit.setNote(request.getNote());
        debit.setPlayer(playerAndSession.getLeft());
        debit.setSession(playerAndSession.getRight());

        debitRepository.save(debit);
        // Update player debit summary incrementally to avoid transaction issue
        increasePlayerDebitSummary(debit);
        return Boolean.TRUE;
    }

    public DebitResponse getDebitById(Integer debitId) {
        Optional<Debit> debitOpt = debitRepository.findById(debitId);
        if (!debitOpt.isPresent()) {
            return null;
        }
        return convertToResponse(debitOpt.get());
    }

    @Transactional
    public RemainingDebitModel getRemainingDebtsBySinglePlayer(DebitDTO debitDTO) throws BusinessException {
        // step 1: get Player by playerName, throw BusinessException if not found
        Player player = userRepository.findByPlayerName(debitDTO.getPlayerName())
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Player not found with name: " + debitDTO.getPlayerName()));

        // step 2: get DebitSummary by Player and isActive true
        Optional<DebitSummary> debitSummaryOpt = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);

        // step 3: get Debit by Player and Pagination (need convert to JPA Pagination)
        Pageable pageable = buildPageable(debitDTO.getPagination());
        Page<Debit> debitPage = debitRepository.findByPlayerIdOrderByCreatedDateDesc(
                player.getPlayerId(),
                debitDTO.getFrom(),
                debitDTO.getTo(),
                pageable);

        // step 4: convert and return RemainingDebitModel
        return convertToDebitModel(debitSummaryOpt, debitPage, debitDTO.getPlayerName());
    }

    private RemainingDebitModel convertToDebitModel(Optional<DebitSummary> debitSummaryOpt, Page<Debit> debitPage, String playerName) {
        BigDecimal totalDebts = debitSummaryOpt.map(DebitSummary::getTotalDebts).orElse(BigDecimal.ZERO);
        int numberDebit = debitSummaryOpt.map(DebitSummary::getNumDebts).orElse(0);

        List<DebitModel> debitModels = debitPage.stream()
                .map(debit -> DebitModel.builder()
                        .dateTime(debit.getCreatedDate())
                        .money(debit.getRemainingAmount())
                        .note(debit.getNote())
                        .build())
                .collect(Collectors.toList());

        // Update pagination total page
//        debitDTO.getPagination().setTotalPage(debitPage.getTotalPages());

        return RemainingDebitModel.builder()
                .playerName(playerName)
                .totalDebts(totalDebts)
                .numberDebit(numberDebit)
                .debitModels(debitModels)
                .build();
    }

    private Pageable buildPageable(Pagination pagination) {
        int current = pagination.getCurrent();
        int pageSize = pagination.getPageSize();
        return PageRequest.of(current - 1, pageSize);
    }

    public List<DebitResponse> getAllDebits() {
        List<Debit> debits = debitRepository.findAll();
        return debits.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DebitSummary payForDebit(Integer debitId, BigDecimal paymentAmount) {
        Optional<Debit> debitOpt = debitRepository.findById(debitId);
        if (!debitOpt.isPresent()) {
            return null;
        }

        Debit debit = debitOpt.get();
        if (paymentAmount.compareTo(debit.getRemainingAmount()) > 0) {
            return null;
        }

        Payment payment = new Payment(paymentAmount, debit.getPlayer());
        paymentRepository.save(payment);

        PaymentDebit paymentDebit = new PaymentDebit(payment, debit, paymentAmount);
        paymentDebitRepository.save(paymentDebit);

        BigDecimal newRemainingAmount = debit.getRemainingAmount().subtract(paymentAmount);
        debit.setRemainingAmount(newRemainingAmount);

        if (newRemainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            debit.setStatus(DebitStatus.PAID);
        } else {
            debit.setStatus(DebitStatus.PARTIALLY_PAID);
        }
        debitRepository.save(debit);

        return updatePlayerDebitSummary(debit.getPlayer());
    }

    /**
     * find player
     * find unpaid debits
     * save 1 payment record > determine belongs to which debit, PaymentDebit
     * update debit status, remaining amount
     * update player DebitSummary
     *
     * @param playerId
     * @param paymentAmount
     * @return
     */
    public DebitSummary payForPlayerDebits(Integer playerId, BigDecimal paymentAmount) {
        Optional<Player> playerOpt = userRepository.findById(playerId);
        if (!playerOpt.isPresent()) {
            return null;
        }

        Player player = playerOpt.get();
        List<Debit> unpaidDebits = debitRepository.findUnpaidDebitsByPlayerIdOrderByCreatedDateAsc(playerId);
        if (unpaidDebits.isEmpty()) {
            return null;
        }

        BigDecimal totalRemainingDebt = unpaidDebits.stream()
                .map(Debit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paymentAmount.compareTo(totalRemainingDebt) > 0) {
            return null;
        }

        Payment payment = new Payment(paymentAmount, player);
        paymentRepository.save(payment);

        BigDecimal remainingPayment = paymentAmount;
        for (Debit debit : unpaidDebits) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal debitRemaining = debit.getRemainingAmount();
            if (debitRemaining.compareTo(remainingPayment) <= 0) {
                PaymentDebit paymentDebit = new PaymentDebit(payment, debit, debitRemaining);
                paymentDebitRepository.save(paymentDebit);
                remainingPayment = remainingPayment.subtract(debitRemaining);
                debit.setRemainingAmount(BigDecimal.ZERO);
                debit.setStatus(DebitStatus.PAID);
                debitRepository.save(debit);
            } else {
                PaymentDebit paymentDebit = new PaymentDebit(payment, debit, remainingPayment);
                paymentDebitRepository.save(paymentDebit);
                BigDecimal newRemainingAmount = debitRemaining.subtract(remainingPayment);
                debit.setRemainingAmount(newRemainingAmount);
                debit.setStatus(DebitStatus.PARTIALLY_PAID);
                debitRepository.save(debit);
                remainingPayment = BigDecimal.ZERO;
            }
        }

        return updatePlayerDebitSummary(player);
    }

    @Transactional
    public DebitSummaryResponse getDebitSummary(String playerName) throws BusinessException {
        List<Player> players = userRepository.findAllByPlayerName(playerName);
        if (players.size() != 1) {
            throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND);
        }

        Optional<DebitSummary> summaryOpt = debitSummaryRepository.findByPlayerAndIsActiveTrue(players.getFirst());
        if (!summaryOpt.isPresent()) {
            return new DebitSummaryResponse(
                    playerName,
                    new MoneyResponse(
                            0.0f,
                            MoneyUtils.CURRENCY_VN
                    ),
                    0
            );
        }

        DebitSummary summary = summaryOpt.get();
        MoneyResponse moneyResponse = new MoneyResponse(
                summary.getTotalDebts().floatValue(),
                summary.getCurrency()
        );

        return new DebitSummaryResponse(
                playerName,
                moneyResponse,
                summary.getNumDebts()
        );
    }

    private DebitResponse convertToResponse(Debit debit) {
        return new DebitResponse(
        );
    }

    private DebitSummary calculateDebitSummary() {
        BigDecimal totalDebts = debitRepository.findAll().stream()
                .map(Debit::getDebtAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long numDebts = debitRepository.count();

        DebitSummary summary = new DebitSummary();
        summary.setTotalDebts(totalDebts);
        summary.setCurrency("VND");
        summary.setNumDebts((int) numDebts);
        return summary;
    }

    private DebitSummary updateDebitSummary() {
        DebitSummary summary = calculateDebitSummary();
        return debitSummaryRepository.save(summary);
    }

    private DebitSummary calculatePlayerDebitSummary(Player player) {
        List<Debit> playerDebits = debitRepository.findByPlayerIdOrderByCreatedDateDesc(player.getPlayerId());
        BigDecimal totalRemainingDebts = playerDebits.stream()
                .map(Debit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int numDebts = (int) playerDebits.stream()
                .filter(d -> d.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        Optional<DebitSummary> existingSummary = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);
        DebitSummary summary;
        if (existingSummary.isPresent()) {
            summary = existingSummary.get();
            summary.setTotalDebts(totalRemainingDebts);
            summary.setNumDebts(numDebts);
            summary.setIsActive(numDebts > 0);
        } else {
            summary = new DebitSummary();
            summary.setTotalDebts(totalRemainingDebts);
            summary.setCurrency("VND");
            summary.setNumDebts(numDebts);
            summary.setPlayer(player);
            summary.setIsActive(numDebts > 0);
        }
        return summary;
    }

    private DebitSummary updatePlayerDebitSummary(Player player) {
//        DebitSummary summary = calculatePlayerDebitSummary(player);
        List<Debit> playerDebits = debitRepository.findByPlayerIdOrderByCreatedDateDesc(player.getPlayerId());
        BigDecimal totalRemainingDebts = playerDebits.stream()
                .map(Debit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int numDebts = (int) playerDebits.stream()
                .filter(d -> d.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        Optional<DebitSummary> existingSummary = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);
        DebitSummary summary;
        if (existingSummary.isPresent()) {
            summary = existingSummary.get();
            summary.setTotalDebts(totalRemainingDebts);
            summary.setNumDebts(numDebts);
            summary.setIsActive(numDebts > 0);
        } else {
            summary = new DebitSummary();
            summary.setTotalDebts(totalRemainingDebts);
            summary.setCurrency(MoneyUtils.CURRENCY_VN);
            summary.setNumDebts(numDebts);
            summary.setPlayer(player);
            summary.setIsActive(numDebts > 0);
        }
        return debitSummaryRepository.save(summary);
    }

    private void increasePlayerDebitSummary(Debit newDebit) {
        final BigDecimal newDebitAmount = newDebit.getDebtAmount();
        final Player player = newDebit.getPlayer();
        // only one debit summary active per player is allowed
        Optional<DebitSummary> existingSummary = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);
        DebitSummary summary;
        if (existingSummary.isPresent()) {
            summary = existingSummary.get();
            summary.setTotalDebts(summary.getTotalDebts().add(newDebitAmount));
            summary.setNumDebts(summary.getNumDebts() + 1);
        } else {
            summary = new DebitSummary();
            summary.setTotalDebts(newDebitAmount);
            summary.setCurrency(MoneyUtils.CURRENCY_VN);
            summary.setNumDebts(1);
            summary.setPlayer(player);
        }
        summary.setIsActive(true);
        debitSummaryRepository.save(summary);
    }

    private Pair<Player, Session> getPlayerAndSession(String name, String createdTime) throws BusinessException {
        Session session = sessionService.getSessionByDateTime(createdTime);
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.CURRENT_SESSION_NOT_FOUND);
        }
        Player player = coreAvailablePlayerService.checkAvailableAndGetPlayer(session, name);
        if (player == null) {
            throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND);
        }

        return Pair.of(player, session);
    }
}
