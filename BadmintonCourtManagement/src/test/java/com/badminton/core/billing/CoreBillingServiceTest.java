package com.badminton.core.billing;

import com.badminton.constant.PayType;
import com.badminton.core.billing.print.EscPosNetworkPrinter;
import com.badminton.core.billing.print.BillPdfService;
import com.badminton.core.billing.print.ReceiptComposer;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceSeries;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import com.badminton.enums.InvoiceItemType;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.model.dto.AllocateDebitPaymentRequest;
import com.badminton.model.dto.AllocateDebitPaymentResponse;
import com.badminton.model.dto.DebitPayDTO;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.repository.BillConfigRepository;
import com.badminton.repository.InvoiceRepository;
import com.badminton.repository.InvoiceSeriesRepository;
import com.badminton.service.SessionServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import({CoreBillingService.class, ReceiptComposer.class, BillPdfService.class, EscPosNetworkPrinter.class})
class CoreBillingServiceTest {

    @Autowired
    private TestEntityManager entityManager;
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CoreBillingService coreBillingService;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoiceSeriesRepository invoiceSeriesRepository;
    @Autowired
    private BillConfigRepository billConfigRepository;

    @MockBean
    private SessionServiceImpl sessionService;

    private Session session;
    private Player player;
    private AvailablePlayer availablePlayer;

    @BeforeEach
    void setUp() {
        when(sessionService.getUTCPlus7Instant()).thenAnswer(inv -> Instant.now());

        session = new Session();
        session.setActive(true);
        entityManager.persist(session);

        player = new Player();
        player.setPlayerName("Nguyen Van A");
        entityManager.persist(player);

        availablePlayer = new AvailablePlayer(player, session);
        availablePlayer.setPayType(PayType.CASH);
        availablePlayer.setPayAmount(120000f);
        entityManager.persist(availablePlayer);

        BillConfig config = new BillConfig();
        config.setBusinessName("Sân cầu lông TC");
        config.setVatRate(new BigDecimal("10"));
        config.setBillPrefix("BL");
        entityManager.persist(config);

        entityManager.flush();
    }

    private PaymentDTO checkoutDto(List<ServiceDTO> services) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPlayerName(player.getPlayerName());
        dto.setServices(services);
        dto.setTotalPay("120000");
        dto.setPayType(PayType.CASH);
        return dto;
    }

    @Test
    void checkoutBill_itemizesChargesAndDeductions() {
        PaymentDTO dto = checkoutDto(List.of(
                new ServiceDTO("Tiền sân", 60000),
                new ServiceDTO("Nước suối", 40000),
                new ServiceDTO("Trả trước", -20000),
                // checkout folds settled debts in as a positive line
                new ServiceDTO("Trả nợ", 70000),
                // Ghi nợ arrives positive — the bill stores it as a deduction
                new ServiceDTO("Ghi nợ", 30000)));

        Invoice bill = coreBillingService.issueCheckoutBill(dto, availablePlayer);

        // gross = charges only = 60k + 40k + 70k
        assertEquals(0, bill.getTotal().compareTo(new BigDecimal("170000")));
        // collected = charges - advance - new debt
        assertEquals(0, bill.getCollectAmount().compareTo(new BigDecimal("120000")));
        // VAT extracted from gross: 170000 / 1.1 = 154545.45 -> 154545
        assertEquals(0, bill.getSubtotal().compareTo(new BigDecimal("154545")));
        assertEquals(0, bill.getVatAmount().compareTo(new BigDecimal("15455")));
        assertEquals("BL-000001", bill.getBillNo());
        assertEquals(InvoiceType.CHECKOUT, bill.getInvoiceType());
        assertEquals(InvoiceStatus.ISSUED, bill.getStatus());
        assertEquals(player.getPlayerName(), bill.getBuyerName());
        assertEquals(5, bill.getItems().size());

        var advance = bill.getItems().stream()
                .filter(i -> i.getItemType() == InvoiceItemType.ADVANCE_DEDUCT).findFirst().orElseThrow();
        assertEquals(0, advance.getAmount().compareTo(new BigDecimal("-20000")));
        var debtCreated = bill.getItems().stream()
                .filter(i -> i.getItemType() == InvoiceItemType.DEBT_CREATED).findFirst().orElseThrow();
        assertEquals(0, debtCreated.getAmount().compareTo(new BigDecimal("-30000")));
    }

    @Test
    void checkoutBill_advancesStoredPositive_stillDeducts() {
        PaymentDTO dto = checkoutDto(List.of(
                new ServiceDTO("Tiền sân", 50000),
                new ServiceDTO("Trả trước", 10000)));

        Invoice bill = coreBillingService.issueCheckoutBill(dto, availablePlayer);
        assertEquals(0, bill.getCollectAmount().compareTo(new BigDecimal("40000")));
    }

    @Test
    void billNumbers_areSequential() {
        Invoice first = coreBillingService.issueCheckoutBill(
                checkoutDto(List.of(new ServiceDTO("Tiền sân", 50000))), availablePlayer);
        Invoice second = coreBillingService.issueCheckoutBill(
                checkoutDto(List.of(new ServiceDTO("Tiền sân", 50000))), availablePlayer);
        assertEquals("BL-000001", first.getBillNo());
        assertEquals("BL-000002", second.getBillNo());

        InvoiceSeries series = invoiceSeriesRepository.findById("BILL").orElseThrow();
        assertEquals(2, series.getCurrentNo());
    }

    @Test
    void debitSettlementBill_sumsDebtLines() {
        AllocateDebitPaymentRequest request = AllocateDebitPaymentRequest.builder()
                .playerName(player.getPlayerName())
                .payMethod(PayType.TRANSFER)
                .listDebitPay(List.of(
                        DebitPayDTO.builder().dateTime("2026-10-01 8:00:00").payAmount(new BigDecimal("50000")).build(),
                        DebitPayDTO.builder().dateTime("2026-10-02 9:00:00").payAmount(new BigDecimal("30000")).build()))
                .build();
        AllocateDebitPaymentResponse allocation = AllocateDebitPaymentResponse.builder()
                .paymentMethod(PayType.TRANSFER)
                .build();

        Invoice bill = coreBillingService.issueDebitSettlementBill(request, allocation);

        assertEquals(InvoiceType.DEBT_SETTLEMENT, bill.getInvoiceType());
        assertEquals(0, bill.getTotal().compareTo(new BigDecimal("80000")));
        assertEquals(0, bill.getCollectAmount().compareTo(new BigDecimal("80000")));
        assertEquals(PayType.TRANSFER, bill.getPayType());
        assertEquals(2, bill.getItems().size());
        assertTrue(bill.getItems().stream()
                .allMatch(i -> i.getItemType() == InvoiceItemType.DEBT_PAID));
    }

    @Test
    void voidBill_marksVoidedAndRejectsDoubleVoid() throws Exception {
        Invoice bill = coreBillingService.issueCheckoutBill(
                checkoutDto(List.of(new ServiceDTO("Tiền sân", 50000))), availablePlayer);

        var response = coreBillingService.voidBill(bill.getInvoiceId(), "Test void");
        assertEquals("VOIDED", response.getStatus());
        assertEquals("Test void", response.getVoidReason());

        assertThrows(IllegalArgumentException.class,
                () -> coreBillingService.voidBill(bill.getInvoiceId(), "again"));
    }

    @Test
    void printBill_browserMode_countsPrint() throws Exception {
        Invoice bill = coreBillingService.issueCheckoutBill(
                checkoutDto(List.of(new ServiceDTO("Tiền sân", 50000))), availablePlayer);

        var response = coreBillingService.printBill(bill.getInvoiceId(), "BROWSER");
        assertEquals(1, response.getPrintCount());
        assertNotNull(invoiceRepository.findById(bill.getInvoiceId()).orElseThrow().getLastPrintedAt());
    }

    @Test
    void receiptComposer_producesHeaderItemsAndVat() {
        ReceiptComposer composer = new ReceiptComposer();
        Invoice invoice = new Invoice();
        invoice.setBillNo("BL-000007");
        invoice.setPayType(PayType.TRANSFER);
        invoice.setTotal(new BigDecimal("110000"));
        invoice.setCollectAmount(new BigDecimal("110000"));
        invoice.setVatRate(new BigDecimal("10"));
        invoice.setSubtotal(new BigDecimal("100000"));
        invoice.setVatAmount(new BigDecimal("10000"));
        invoice.setIssuedAt(Instant.parse("2026-10-05T08:30:00Z"));
        invoice.setIssuedBy("staff");
        invoice.setPlayer(player);

        BillConfig config = new BillConfig();
        config.setBusinessName("Sân TC");
        config.setTaxCode("0123456789");

        var doc = composer.compose(invoice, config);
        String all = doc.getLines().stream()
                .map(l -> (l.getLeft() != null ? l.getLeft() : "") + "|" + (l.getRight() != null ? l.getRight() : ""))
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(all.contains("Sân TC"));
        assertTrue(all.contains("MST: 0123456789"));
        assertTrue(all.contains("HOÁ ĐƠN BÁN HÀNG"));
        assertTrue(all.contains("BL-000007"));
        assertTrue(all.contains("05/10/2026 08:30"));
        assertTrue(all.contains("Chuyển khoản"));
        assertTrue(all.contains("Tổng cộng"));
        assertTrue(all.contains("Trong đó VAT (10%)"));
        assertTrue(all.contains("Thực thu"));
    }
}
