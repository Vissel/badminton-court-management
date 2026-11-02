package com.badminton.core.billing.einvoice;

import com.badminton.constant.PayType;
import com.badminton.core.billing.InvoiceAuditLogger;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceItem;
import com.badminton.enums.CorrectionType;
import com.badminton.enums.EInvoiceStatus;
import com.badminton.enums.InvoiceItemType;
import com.badminton.enums.InvoiceKind;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.repository.BillConfigRepository;
import com.badminton.repository.InvoiceRepository;
import com.badminton.service.SessionServiceImpl;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Provider-facing behaviour of the e-invoice step, driven through a mocked
 * MISA client: state transitions (PENDING before the HTTP call), retention of
 * the XML/PDF lookups (G-3) and the correction payload (G-1).
 */
@ExtendWith(MockitoExtension.class)
class EInvoiceServiceTest {

    private static final Long BILL_ID = 7L;

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private BillConfigRepository billConfigRepository;
    @Mock
    private MisaMeInvoiceClient client;
    @Mock
    private SessionServiceImpl sessionService;
    @Mock
    private InvoiceAuditLogger auditLogger;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private EInvoiceService service;

    /**
     * Status stored by each save() call — the entity itself is mutated in place.
     */
    private final List<EInvoiceStatus> savedStatuses = new ArrayList<>();
    private Invoice invoice;
    private BillConfig config;

    @BeforeEach
    void setUp() {
        config = new BillConfig();
        config.setEinvoiceEnabled(true);
        config.setInvoiceKind(InvoiceKind.BAN_HANG);
        config.setEinvoiceSeries("C26TAA");
        config.setTaxCode("0312345678");
        config.setEinvoiceLookupBase("https://hoadon.vn/tra-cuu/{no}?ky={series}&mst={taxCode}");
        lenient().when(billConfigRepository.findFirstByOrderByConfigIdAsc()).thenReturn(Optional.of(config));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(sessionService.getUTCPlus7Instant()).thenReturn(Instant.parse("2026-11-01T05:00:00Z"));
        lenient().when(client.isConfigured()).thenReturn(true);

        invoice = bill();
        lenient().when(invoiceRepository.findById(BILL_ID)).thenReturn(Optional.of(invoice));
        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice saved = call.getArgument(0);
            savedStatuses.add(saved.getEinvoiceStatus());
            return saved;
        });
    }

    @Test
    void issueCommitsPendingBeforePublishingAndArchivesXmlAndLookups() throws Exception {
        when(client.publish(any())).thenReturn(issuedResponse());

        Invoice issued = service.issue(BILL_ID);

        // G-16: the in-flight state is committed before the provider is called.
        assertEquals(EInvoiceStatus.PENDING, savedStatuses.get(0));
        InOrder order = inOrder(invoiceRepository, client);
        order.verify(invoiceRepository).save(any(Invoice.class));
        order.verify(client).publish(any());

        // G-3: provider XML + PDF/lookup URLs are retained on the bill.
        assertEquals(EInvoiceStatus.ISSUED, issued.getEinvoiceStatus());
        assertEquals("0000123", issued.getEinvoiceNo());
        assertEquals("REF-1", issued.getEinvoiceRef());
        assertEquals("<Invoice/>", issued.getEinvoiceXml());
        assertEquals("https://hoadon.vn/tra-cuu/0000123?ky=C26TAA&mst=0312345678",
                issued.getEinvoicePdfUrl());
        assertEquals(issued.getEinvoicePdfUrl(), issued.getEinvoiceLookupUrl());

        // G-11: the buyer receives the invoice by email straight after issue.
        verify(client).sendMail(eq("buyer@example.com"), eq("REF-1"), eq("0000123"));
    }

    @Test
    void issueMarksFailedWhenProviderRejects() throws Exception {
        JsonObject rejection = new JsonObject();
        rejection.addProperty("ErrorCode", "E01");
        rejection.addProperty("Description", "Ký hiệu hoá đơn không hợp lệ");
        when(client.publish(any())).thenReturn(rejection);

        Invoice failed = service.issue(BILL_ID);

        assertEquals(EInvoiceStatus.FAILED, failed.getEinvoiceStatus());
        assertEquals("Ký hiệu hoá đơn không hợp lệ", failed.getEinvoiceError());
    }

    @Test
    void issueUsesConfiguredInvoiceKindForTheTitle() throws Exception {
        config.setInvoiceKind(InvoiceKind.GTGT);
        when(client.publish(any())).thenReturn(issuedResponse());

        service.issue(BILL_ID);

        Map<String, Object> payload = capturedPayload();
        assertEquals("HÓA ĐƠN GIÁ TRỊ GIA TĂNG", payload.get("InvoiceName"));
        assertEquals(1, payload.get("InvoiceTypeID"));
        // G-8: the line snapshot drives the VAT line, not the bill-level rate.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detail = (List<Map<String, Object>>) payload.get("InvoiceDetail");
        assertEquals(new BigDecimal("9.09"), new BigDecimal(detail.get(0).get("TaxRate").toString()));
        assertEquals(new BigDecimal("9091"), new BigDecimal(detail.get(0).get("VATAmount").toString()));
    }

    @Test
    void adjustmentInvoiceCarriesTheOriginalReference() throws Exception {
        Invoice origin = bill();
        origin.setBillNo("BL-0001");
        origin.setEinvoiceNo("0000100");
        invoice.setInvoiceType(InvoiceType.ADJUSTMENT);
        invoice.setCorrectionType(CorrectionType.DIEU_CHINH);
        invoice.setAdjustReason("Tính sai tiền sân");
        invoice.setRelatedInvoice(origin);
        invoice.setTotal(new BigDecimal("-20000"));
        when(client.publish(any())).thenReturn(issuedResponse());

        service.issue(BILL_ID);

        Map<String, Object> payload = capturedPayload();
        assertEquals(Boolean.TRUE, payload.get("IsAdjustment"));
        assertEquals(1, payload.get("AdjustmentType"));
        assertEquals("Tính sai tiền sân", payload.get("AdjustmentReason"));
        assertEquals("0000100", payload.get("OriginInvoiceNo"));
        assertEquals("BL-0001", payload.get("OriginTransactionID"));
        assertEquals(Boolean.FALSE, payload.get("IsIncrease"));
    }

    @Test
    void downloadXmlFallsBackToTheProviderAndPersistsTheResult() throws Exception {
        invoice.setEinvoiceNo("0000123");
        when(client.downloadXml("0000123")).thenReturn("<Invoice fetched=\"true\"/>");

        String xml = service.downloadXml(BILL_ID);

        assertEquals("<Invoice fetched=\"true\"/>", xml);
        assertEquals(xml, invoice.getEinvoiceXml());
    }

    @Test
    void sendMailRequiresAnIssuedEInvoice() {
        invoice.setEinvoiceStatus(EInvoiceStatus.NONE);

        assertThrows(IllegalArgumentException.class, () -> service.sendMail(BILL_ID, null));
    }

    @Test
    void sendMailFallsBackToTheBuyerEmailStoredOnTheBill() throws Exception {
        invoice.setEinvoiceStatus(EInvoiceStatus.ISSUED);
        invoice.setEinvoiceNo("0000123");
        invoice.setEinvoiceRef("REF-1");
        when(client.sendMail(anyString(), anyString(), anyString())).thenReturn(new JsonObject());

        service.sendMail(BILL_ID, null);

        verify(client).sendMail(eq("buyer@example.com"), eq("REF-1"), eq("0000123"));
        assertNotNull(invoice.getBuyerEmail());
    }

    // ---------- helpers ----------

    private Invoice bill() {
        Invoice bill = new Invoice();
        bill.setInvoiceId(BILL_ID);
        bill.setBillNo("BL-0002");
        bill.setInvoiceType(InvoiceType.CHECKOUT);
        bill.setStatus(InvoiceStatus.ISSUED);
        bill.setEinvoiceStatus(EInvoiceStatus.NONE);
        bill.setPayType(PayType.CASH);
        bill.setCurrency("VND");
        bill.setIssuedAt(Instant.parse("2026-11-01T05:00:00Z"));
        bill.setIssuedBy("tester");
        bill.setSubtotal(new BigDecimal("90909"));
        bill.setVatRate(new BigDecimal("10"));
        bill.setVatAmount(new BigDecimal("9091"));
        bill.setTotal(new BigDecimal("100000"));
        bill.setCollectAmount(new BigDecimal("100000"));
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setBuyerName("Nguyễn Văn A");
        bill.setBuyerEmail("buyer@example.com");

        InvoiceItem line = new InvoiceItem(InvoiceItemType.COURT_FEE, "Tiền sân",
                BigDecimal.ONE, new BigDecimal("100000"), new BigDecimal("100000"));
        line.setLineNo(1);
        line.setVatRate(new BigDecimal("9.09"));
        line.setVatAmount(new BigDecimal("9091"));
        bill.addItem(line);
        return bill;
    }

    private JsonObject issuedResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("Success", true);
        response.addProperty("InvoiceNo", "0000123");
        response.addProperty("RefID", "REF-1");
        response.addProperty("InvoiceXML", "<Invoice/>");
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() throws Exception {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).publish(captor.capture());
        assertTrue(captor.getValue().containsKey("InvoiceDetail"));
        return captor.getValue();
    }
}
