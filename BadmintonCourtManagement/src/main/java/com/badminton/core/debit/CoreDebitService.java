package com.badminton.core.debit;

import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.entity.Debit;
import com.badminton.entity.DebitSummary;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import com.badminton.enums.DebitStatus;
import com.badminton.enums.PaymentStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.debit.*;
import com.badminton.model.dto.*;
import com.badminton.repository.DebitRepository;
import com.badminton.repository.DebitSummaryRepository;
import com.badminton.repository.PaymentDebitRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.Pagination;
import com.badminton.response.debit.*;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.MoneyUtils;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoreDebitService {
    @Autowired
    private DebitSummaryRepository debitSummaryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DebitRepository debitRepository;
    @Autowired
    private SessionServiceImpl sessionService;
    @Autowired
    private CoreAvailablePlayerService coreAvailablePlayerService;
    @Autowired
    private CorePayDebitService corePayDebitService;
    @Autowired
    private PaymentDebitRepository paymentDebitRepository;

    @Transactional
    public Boolean createDebit(CreateDebitDTO dto) throws BusinessException {
        Pair<Player, Session> playerAndSession = getPlayerAndSession(dto.getPlayerName(), dto.getCreatedTime());

        Debit debit = new Debit();
        BigDecimal debtAmount = dto.getDebitAmount();
        debit.setDebtAmount(debtAmount);
        debit.setRemainingAmount(debtAmount);
        debit.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : MoneyUtils.CURRENCY_VN);
        debit.setStatus(DebitStatus.PENDING);
        debit.setNote(dto.getNote());
        debit.setPlayer(playerAndSession.getLeft());
        debit.setSession(playerAndSession.getRight());
        debit.setCreatedDate(dto.getCreatedTime());
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
    public RemainingDebitModel getRemainingDebtsBySinglePlayer(RemainingDebitDTO remainingDebitDTO) throws BusinessException {
        // step 1: get Player by playerName, throw BusinessException if not found
        Player player = resolvePlayerByName(remainingDebitDTO.getPlayerName());

        // step 2: get DebitSummary by Player and isActive true
        Optional<DebitSummary> debitSummaryOpt = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);

        // step 3: get Debit by Player and Pagination (need convert to JPA Pagination)
        Pageable pageable = buildPageable(remainingDebitDTO.getPagination());
        Page<Debit> debitPage = debitRepository.findByPlayerIdOrderByCreatedDateDesc(
                player.getPlayerId(),
                remainingDebitDTO.getFrom(),
                remainingDebitDTO.getTo(),
                pageable);

        // step 4: convert and return RemainingDebitModel
        return convertToDebitModel(debitSummaryOpt, debitPage, remainingDebitDTO.getPlayerName());
    }

    /**
     * Full debit history of a player (paid + remaining), newest first.
     *
     * @param dto playerName, pagination (1-based), optional from/to bounds
     */
    public DebitHistoryModel getDebitHistory(DebitHistoryDTO dto) throws BusinessException {
        Player player = resolvePlayerByName(dto.getPlayerName());

        Pageable pageable = buildPageable(dto.getPagination());
        Page<Debit> debitPage = debitRepository.findHistoryByPlayerId(
                player.getPlayerId(), dto.getFrom(), dto.getTo(),
                dto.getAmountFrom() != null ? BigDecimal.valueOf(dto.getAmountFrom()) : null,
                dto.getAmountTo() != null ? BigDecimal.valueOf(dto.getAmountTo()) : null,
                pageable);

        Map<Integer, Instant> lastPaidDates = findLastPaymentDates(debitPage.getContent());
        List<DebitHistoryItemModel> items = debitPage.stream()
                .map(debit -> toHistoryItemModel(debit, lastPaidDates.get(debit.getDebitId())))
                .collect(Collectors.toList());

        return DebitHistoryModel.builder()
                .playerName(dto.getPlayerName())
                .items(items)
                .total(debitPage.getTotalElements())
                .totalPage(debitPage.getTotalPages())
                .build();
    }

    /**
     * Aggregate summary over the player's full debit history (paid + remaining).
     */
    public DebitHistorySummaryModel getDebitHistorySummary(String playerName) throws BusinessException {
        Player player = resolvePlayerByName(playerName);

        Object[] row = debitRepository.summarizeHistoryByPlayerId(player.getPlayerId());
        // Spring Data JPA returns the aggregate tuple wrapped inside a single-element Object[]
        Object[] values = (row != null && row.length == 1 && row[0] instanceof Object[] nested) ? nested : row;
        BigDecimal totalDebt = toBigDecimal(values != null && values.length > 0 ? values[0] : null);
        BigDecimal totalRemaining = toBigDecimal(values != null && values.length > 1 ? values[1] : null);
        int numDebits = values != null && values.length > 2 && values[2] instanceof Number ? ((Number) values[2]).intValue() : 0;
        int numPaid = values != null && values.length > 3 && values[3] instanceof Number ? ((Number) values[3]).intValue() : 0;

        return DebitHistorySummaryModel.builder()
                .playerName(playerName)
                .totalDebitAmount(totalDebt)
                .totalPaidAmount(totalDebt.subtract(totalRemaining))
                .totalRemainingAmount(totalRemaining)
                .numDebits(numDebits)
                .numPaidDebits(numPaid)
                .numUnpaidDebits(numDebits - numPaid)
                .currency(MoneyUtils.CURRENCY_VN)
                .build();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Resolve a player by name: exact match first, then a normalized match that
     * tolerates case, extra whitespace, leading zeros in digits and Vietnamese
     * diacritics (e.g. "nguoi choi 9" matches stored "nguoi choi 009").
     */
    private Player resolvePlayerByName(String playerName) throws BusinessException {
        List<Player> exactMatches = userRepository.findAllByPlayerName(playerName);
        if (!exactMatches.isEmpty()) {
            return exactMatches.getFirst();
        }
        String target = normalizePlayerName(playerName);
        List<Player> candidates = userRepository.findAll().stream()
                .filter(p -> normalizePlayerName(p.getPlayerName()).equals(target))
                .collect(Collectors.toList());
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND,
                "Player not found with name: " + playerName);
    }

    private static String normalizePlayerName(String name) {
        if (name == null) {
            return "";
        }
        String collapsed = name.trim().toLowerCase().replaceAll("\\s+", " ");
        String noDiacritics = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd');
        String[] tokens = noDiacritics.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.matches("0+\\d+")) {
                token = token.replaceAll("^0+", "");
            }
            sb.append(token).append(' ');
        }
        return sb.toString().trim();
    }

    private Map<Integer, Instant> findLastPaymentDates(List<Debit> debits) {
        if (debits == null || debits.isEmpty()) {
            return Map.of();
        }
        List<Integer> debitIds = debits.stream().map(Debit::getDebitId).toList();
        return paymentDebitRepository.findLastPaymentDateByDebitIds(debitIds).stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Instant) row[1]));
    }

    private DebitHistoryItemModel toHistoryItemModel(Debit debit, Instant paidDateTime) {
        BigDecimal remaining = debit.getRemainingAmount() != null ? debit.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal debt = debit.getDebtAmount() != null ? debit.getDebtAmount() : BigDecimal.ZERO;
        return DebitHistoryItemModel.builder()
                .debitId(debit.getDebitId())
                .debtDateTime(debit.getCreatedDate())
                .paidDateTime(paidDateTime)
                .debtAmount(debt)
                .remainingAmount(remaining)
                .paidAmount(debt.subtract(remaining))
                .status(debit.getStatus())
                .currency(debit.getCurrency())
                .note(debit.getNote())
                .build();
    }

    private RemainingDebitModel convertToDebitModel(Optional<DebitSummary> debitSummaryOpt, Page<Debit> debitPage, String playerName) {
        BigDecimal totalDebts = debitSummaryOpt.map(DebitSummary::getTotalDebts).orElse(BigDecimal.ZERO);
        int numberDebit = debitSummaryOpt.map(DebitSummary::getNumDebts).orElse(0);

        List<DebitModel> debitModels = debitPage.stream()
                .map(debit -> DebitModel.builder()
                        .debitId(debit.getDebitId())
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

    /**
     *
     * @param request
     * @return
     * @throws BusinessException
     */
    @Transactional
    public PrepayDebitResponse prepayDebitsForPlayer(AllocateDebitPaymentRequest request) throws BusinessException {
        Player player = resolvePlayerByName(request.getPlayerName());
        List<Debit> unpaidDebits = debitRepository.findUnpaidDebitsByPlayerIdOrderByCreatedDateAsc(player.getPlayerId());
        if (unpaidDebits.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.DEBTS_NOT_FOUND);
        }
        BigDecimal remainingAmount = request.getPayAmount();
        List<PrepayDebit> prepayDebits = new ArrayList<>();
        for (Debit debit : unpaidDebits) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal appliedAmount = debit.getRemainingAmount().min(remainingAmount);
            PrepayStatus prepayStatus = appliedAmount.compareTo(debit.getRemainingAmount()) == 0
                    ? PrepayStatus.FULL_PAY
                    : PrepayStatus.PARTIALLY_PAY;

            PrepayDebit prepayDebit = new PrepayDebit();
            prepayDebit.setPayDebit(new RemainingDebitsResponse(
                    TimeUtils.toDateTimeDisplay(debit.getCreatedDate()),
                    new MoneyResponse(appliedAmount.floatValue(),
                            debit.getCurrency() != null ? debit.getCurrency() : MoneyUtils.CURRENCY_VN),
                    debit.getNote()));
            prepayDebit.setPayStatus(prepayStatus.name());
            prepayDebits.add(prepayDebit);

            remainingAmount = remainingAmount.subtract(appliedAmount);
        }

        PrepayDebitResponse response = new PrepayDebitResponse();
        response.setPlayerName(request.getPlayerName());
        response.setPaymentAmount(request.getPayAmount() != null ? request.getPayAmount().floatValue() : 0f);
        response.setPrepayDebits(prepayDebits);
        return response;
    }

    /**
     * find player
     * find unpaid debits
     * call corePayDebitService to pay each debit until the pay amount is used all
     * update player DebitSummary
     *
     * @param allocateDebitPaymentRequest
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public AllocateDebitPaymentResponse allocateDebitPayment(AllocateDebitPaymentRequest allocateDebitPaymentRequest) {
        AllocateDebitPaymentResponse response = AllocateDebitPaymentResponse.builder()
                .playerName(allocateDebitPaymentRequest.getPlayerName())
                .paymentAmount(allocateDebitPaymentRequest.getPayAmount())
                .paymentMethod(allocateDebitPaymentRequest.getPayMethod())
                .status(PaymentStatus.INPROGRESS) // keep it default
                .build();
        try {
            log.info("Allocating DebitPayment for player:{}", allocateDebitPaymentRequest.getPlayerName());
            Player player = resolvePlayerByName(allocateDebitPaymentRequest.getPlayerName());

            List<Debit> unpaidDebits = debitRepository.findUnpaidDebitsByPlayerIdOrderByCreatedDateAsc(player.getPlayerId());
            if (unpaidDebits.isEmpty()) {
                throw new BusinessException(ErrorCodeEnum.DEBTS_NOT_FOUND);
            }

            // call corePayDebitService to pay each requested debit in the list.
            BigDecimal totalPaid = BigDecimal.ZERO;
            int numPaidDebts = 0;
            List<DebitPayDTO> debitPays = allocateDebitPaymentRequest.getListDebitPay();
            if (debitPays != null) {
                for (DebitPayDTO debitPay : debitPays) {
                    Debit debit = unpaidDebits.stream()
                            .filter(d -> TimeUtils.toDateTimeDisplay(d.getCreatedDate()).equals(debitPay.getDateTime()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException(
                                    ErrorCodeEnum.DEBIT_NOT_FOUND,
                                    "Unpaid debit not found at: " + debitPay.getDateTime()));

                    PayDebitModel payDebitModel = corePayDebitService.payForDebit(PayDebitDTO.builder()
                            .playerName(player.getPlayerName())
                            .payAmount(debitPay.getPayAmount())
                            .payMethod(allocateDebitPaymentRequest.getPayMethod())
                            .note(allocateDebitPaymentRequest.getNote())
                            .payForDebit(DebitModel.builder().debitId(debit.getDebitId()).build())
                            .build());

                    totalPaid = totalPaid.add(debitPay.getPayAmount());
                    if (DebitStatus.PAID.name().equals(payDebitModel.getStatus()) ||
                            DebitStatus.PARTIALLY_PAID.name().equals(payDebitModel.getStatus())) {
                        numPaidDebts++;
                    }
                }
            }

            DebitSummary summary = updatePlayerDebitSummary(player);
            response.setPaidDebts(totalPaid);
            response.setRemainingDebts(summary.getTotalDebts());
            response.setNumPaidDebts(numPaidDebts);
            response.setNumRemainingDebts(summary.getNumDebts());
            response.setPaymentDate(Instant.now());
            response.setStatus(summary.getNumDebts() == 0 ? PaymentStatus.SUCCESS : PaymentStatus.PARTIAL);
            response.setMessage(summary.getNumDebts() == 0
                    ? "Payment allocation completed successfully"
                    : "Payment allocation completed. Some debts are still remaining");
            response.setErrorCode(0);
        } catch (BusinessException e) {
            log.error("Allocating DebitPayment for player:{} got business error: {}", allocateDebitPaymentRequest.getPlayerName(), e.getMessage());
            response.setStatus(PaymentStatus.FAIL);
            response.setMessage(e.getErrorMessage());
            response.setErrorCode(Integer.valueOf(e.getErrorCodeEnum().getCode()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception e) {
            log.error("Allocating DebitPayment for player:{} got error:{}", allocateDebitPaymentRequest.getPlayerName()
                    , e.getMessage());
            response.setStatus(PaymentStatus.FAIL);
            response.setMessage("Server error while allocating debit payment");
            response.setErrorCode(Integer.valueOf(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return response;

    }

    @Transactional
    public DebitSummaryResponse getDebitSummary(String playerName) throws BusinessException {
        Player player = resolvePlayerByName(playerName);

        Optional<DebitSummary> summaryOpt = debitSummaryRepository.findByPlayerAndIsActiveTrue(player);
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

    private Pair<Player, Session> getPlayerAndSession(String name, Instant createdTime) throws BusinessException {
        String createdTimeStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(java.time.ZoneOffset.UTC).format(createdTime);
        List<Session> sessions = sessionService.getSessionsByDateTime(createdTimeStr);
        if (sessions.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.CURRENT_SESSION_NOT_FOUND);
        }
        for (Session session : sessions) {
            Player player = coreAvailablePlayerService.checkAvailableAndGetPlayer(session, name);
            if (player != null) {
                return Pair.of(player, session);
            }
        }
        throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND);
    }
}
