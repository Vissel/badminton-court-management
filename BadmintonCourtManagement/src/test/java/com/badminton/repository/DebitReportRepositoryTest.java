package com.badminton.repository;

import com.badminton.entity.Debit;
import com.badminton.entity.Payment;
import com.badminton.entity.PaymentDebit;
import com.badminton.entity.Player;
import com.badminton.enums.DebitStatus;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class DebitReportRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DebitReportRepository debitReportRepository;

    @Autowired
    private PaymentDebitRepository paymentDebitRepository;

    private Player playerAn;
    private Player playerBinh;
    private Instant baseTime;
    private Payment payment1;
    private Payment payment2;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-09-01T00:00:00Z");

        playerAn = new Player("Nguyễn Văn An", "pass");
        playerBinh = new Player("Trần Thị Bình", "pass");
        entityManager.persist(playerAn);
        entityManager.persist(playerBinh);
        entityManager.flush();

        // Player An debits
        Debit debitAn1 = createDebit(playerAn, new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                baseTime.plusSeconds(0), DebitStatus.PENDING, "Note 1");
        Debit debitAn2 = createDebit(playerAn, new BigDecimal("200000.00"), new BigDecimal("50000.00"),
                baseTime.plusSeconds(86400), DebitStatus.PARTIALLY_PAID, "Note 2");
        Debit debitAn3 = createDebit(playerAn, new BigDecimal("150000.00"), BigDecimal.ZERO,
                baseTime.plusSeconds(172800), DebitStatus.PAID, "Note 3");

        // Player Binh debits
        Debit debitBinh1 = createDebit(playerBinh, new BigDecimal("300000.00"), new BigDecimal("300000.00"),
                baseTime.plusSeconds(43200), DebitStatus.PENDING, null);

        entityManager.persist(debitAn1);
        entityManager.persist(debitAn2);
        entityManager.persist(debitAn3);
        entityManager.persist(debitBinh1);
        entityManager.flush();

        // Payment for debitAn2 (two partial payments)
        payment1 = new Payment(new BigDecimal("100000.00"), "First pay", playerAn, com.badminton.constant.PayType.CASH);
        payment2 = new Payment(new BigDecimal("50000.00"), "Second pay", playerAn, com.badminton.constant.PayType.CASH);
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();

        PaymentDebit pd1 = new PaymentDebit(payment1, debitAn2, new BigDecimal("100000.00"));
        PaymentDebit pd2 = new PaymentDebit(payment2, debitAn2, new BigDecimal("50000.00"));
        entityManager.persist(pd1);
        entityManager.persist(pd2);
        entityManager.flush();

        // Hibernate generated DDL omits the DEFAULT CURRENT_TIMESTAMP on payment_date,
        // so populate it explicitly for the test data.
        em.createNativeQuery("UPDATE \"bad-court-management-db\".payment SET payment_date = CURRENT_TIMESTAMP " +
                        "WHERE payment_id IN (:ids)")
                .setParameter("ids", List.of(payment1.getPaymentId(), payment2.getPaymentId()))
                .executeUpdate();
        em.flush();
    }

    private Debit createDebit(Player player, BigDecimal debtAmount, BigDecimal remainingAmount,
                              Instant createdDate, DebitStatus status, String note) {
        Debit debit = new Debit();
        debit.setDebtAmount(debtAmount);
        debit.setRemainingAmount(remainingAmount);
        debit.setCurrency("VND");
        debit.setStatus(status);
        debit.setCreatedDate(createdDate);
        debit.setNote(note);
        debit.setPlayer(player);
        // session_id is non-nullable; create a minimal Session
        com.badminton.entity.Session session = new com.badminton.entity.Session();
        session.setFromTime(baseTime);
        session.setToTime(baseTime.plusSeconds(3600));
        session.setActive(true);
        entityManager.persist(session);
        debit.setSession(session);
        return debit;
    }

    @Test
    void currentSummaryExcludesFullyPaidDebts() {
        List<DebtPlayerSummaryRow> rows = debitReportRepository.findCurrentSummary(
                null, null, baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "p.playerName ASC");

        assertEquals(2, rows.size());
        DebtPlayerSummaryRow an = rows.stream().filter(r -> r.getPlayerName().equals("Nguyễn Văn An")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("150000.00"), an.getTotalRemainingDebt());
        assertEquals(2L, an.getNumDebits());

        DebtPlayerSummaryRow binh = rows.stream().filter(r -> r.getPlayerName().equals("Trần Thị Bình")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("300000.00"), binh.getTotalRemainingDebt());
    }

    @Test
    void historySummaryIncludesAllDebtStatuses() {
        List<DebtPlayerSummaryRow> rows = debitReportRepository.findHistorySummary(
                null, null, baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "p.playerName ASC");

        assertEquals(2, rows.size());
        DebtPlayerSummaryRow an = rows.stream().filter(r -> r.getPlayerName().equals("Nguyễn Văn An")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("450000.00"), an.getTotalDebt());
        assertEquals(new BigDecimal("300000.00"), an.getTotalPaid());
        assertEquals(new BigDecimal("150000.00"), an.getTotalRemaining());
        assertEquals(3L, an.getTotalDebits());
        assertEquals(1L, an.getPaidDebits());
    }

    @Test
    void currentDetailsByPlayerFilter() {
        List<DebtCurrentReportRow> rows = debitReportRepository.findCurrentDetails(
                null, "An", baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "p.playerName ASC", 100, 0);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.getPlayerName().contains("An")));
    }

    @Test
    void historyDetailsReturnOneRowPerDebit() {
        List<DebtHistoryReportRow> rows = debitReportRepository.findHistoryDetails(
                playerAn.getPlayerId(), null, baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "d.debitId ASC", 100, 0);

        assertEquals(3, rows.size());
        DebtHistoryReportRow partial = rows.stream()
                .filter(r -> r.getStatus() == DebitStatus.PARTIALLY_PAID)
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("200000.00"), partial.getDebtAmount());
        assertEquals(new BigDecimal("50000.00"), partial.getRemainingAmount());
        assertEquals(new BigDecimal("150000.00"), partial.getPaidAmount());
    }

    @Test
    void dateBoundariesExcludeOutOfRange() {
        List<DebtCurrentReportRow> rows = debitReportRepository.findCurrentDetails(
                null, null, baseTime.plusSeconds(400000), baseTime.plusSeconds(500000),
                "d.debitId ASC", 100, 0);
        assertTrue(rows.isEmpty());
    }

    @Test
    void summaryTotalsEqualDetailTotalsForCurrent() {
        List<DebtPlayerSummaryRow> summaries = debitReportRepository.findCurrentSummary(
                null, null, baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "p.playerName ASC");
        BigDecimal summaryTotal = summaries.stream()
                .map(s -> s.getTotalRemainingDebt() != null ? s.getTotalRemainingDebt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long summaryCount = summaries.stream().mapToLong(DebtPlayerSummaryRow::getNumDebits).sum();

        List<DebtCurrentReportRow> details = debitReportRepository.findCurrentDetails(
                null, null, baseTime.minusSeconds(1), baseTime.plusSeconds(300000),
                "d.debitId ASC", 100, 0);
        BigDecimal detailTotal = details.stream()
                .map(d -> d.getRemainingAmount() != null ? d.getRemainingAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(summaryTotal, detailTotal);
        assertEquals(summaryCount.intValue(), details.size());
    }

    @Test
    void latestPaymentDateIsReturnedByDebitId() {
        List<Integer> debitIds = List.of(debitAn2().getDebitId());
        List<Object[]> rows = paymentDebitRepository.findLastPaymentDateByDebitIds(debitIds);

        assertEquals(1, rows.size());
        assertNotNull(rows.get(0)[1]);
    }

    private Debit debitAn2() {
        return em.createQuery("SELECT d FROM Debit d WHERE d.note = 'Note 2'", Debit.class)
                .getSingleResult();
    }
}
