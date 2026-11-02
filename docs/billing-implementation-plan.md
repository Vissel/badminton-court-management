# Billing / E-Invoice — Implementation Plan (all valid batches)

> Applies the **VALID** gaps from `billing-gap-analysis-final.md` under
> **NĐ 254/2026/NĐ-CP**. Sequenced P0 → P3. The two **INVALID** items
> (G-4 amount-in-words, G-14 offline queue) are explicitly **not** built —
> see §6.

## 0. Global constraints (apply to every change)

- **Liquibase:** new schema only in fresh changesets **029+**, registered in
  `db.changelog-master.xml`. **001–028 are frozen** (never edit). MySQL
  `ALTER TABLE ADD COLUMN` has **no `IF NOT EXISTS`** — guard with a
  precondition or a dedicated changeset.
- **Errors:** surface **Vietnamese guidance** only (`GlobalExceptionHandler`);
  never raw SQL/stack text.
- **Time:** use `coreSession.getUTCPlus7Instant()` / `TimeUtils` UTC helpers.
- **Dialogs:** `sm` width + draggable resizable paper; **MUI v9** icon names.
- **Consistency:** every new FE field has a matching BE persistence path.
- **Beans:** new Spring beans can break devtools hot-restart / create cycles —
  verify an isolated `mvn -Pdev` run.
- **New dep:** ZXing (`com.google.zxing:core` + `:javase`) for QR rendering.

---

## Batch P0 — Legal must-fix

### G-1 · Adjustment / replacement invoice (score 9)
- **Enums:** add `InvoiceType.ADJUSTMENT`, `InvoiceType.REPLACEMENT`.
- **Entity `Invoice`:** add self-FK `relatedInvoice` (`related_invoice_id`),
  `correctionType` (DIEU_CHINH | THAY_THE), `adjustReason`.
- **Changeset 029:** `related_invoice_id BIGINT`, `correction_type VARCHAR(20)`,
  `adjust_reason VARCHAR(250)` + FK.
- **`CoreBillingService`:** `issueAdjustmentBill(originalId, deltas, reason)` /
  `issueReplacementBill(originalId, reason)` — new immutable bill linked to the
  original, VAT recomputed, numbering continues gaplessly.
- **`EInvoiceService`/`MisaMeInvoiceClient`:** correction payload referencing
  the original `einvoice_no` + increase/decrease flag.
- **Controller:** `POST /bills/{id}/adjust`, `POST /bills/{id}/replace` (admin).
- **DTO:** `AdjustBillRequest`, `ReplaceBillRequest`.
- **FE:** `BillingPage` admin actions → dialog (sm+draggable) → `billApi`.
- **Tests:** correction bill linked to original; VAT recompute; gapless number.

### G-2 · Correction → void path (unblock dead-end) (score 6)
- **`CoreBillingService.voidBill`:** replace the impossible "cancel the
  e-invoice first" guard with: a bill whose e-invoice is `ISSUED` may be voided
  **only after** a linked correction (G-1) exists; otherwise Vietnamese guidance
  "Vui lòng lập hóa đơn điều chỉnh/thay thế trước khi huỷ."
- Keep `EInvoiceStatus.CANCELLED` only if MISA exposes cancel/thông-báo-sai-sót;
  else document correction = adjust/replace.
- **Tests:** void blocked until correction; allowed after.

### G-3 · E-invoice data retention + `einvoicePdfUrl` bug (score 7)
- **Entity `Invoice`:** add `einvoiceXml` (LONGTEXT); populate `einvoicePdfUrl`.
- **Changeset 030:** `ALTER TABLE invoice ADD COLUMN einvoice_xml LONGTEXT`.
- **`MisaMeInvoiceClient`/`EInvoiceService.issue()`:** capture provider XML +
  PDF URL from the response (or a follow-up download) and persist both.
- **Controller:** `GET /bills/{id}/einvoice/xml`.
- **FE:** `BillingPage` "Tải XML" button when e-invoice issued.
- **Tests:** `issue()` persists xml + pdf url.

### G-11 + NEW · Buyer retrieval path: QR/link + email delivery (score 7)
- **Config/entity:** store/derive a lookup URL (`einvoice_lookup_url`, or build
  from `einvoice_no` + a portal base in `bill_config`). **Changeset 031.**
- **`ReceiptComposer` + `BillPdfService` + `EscPosNetworkPrinter`:** render a
  **QR code** (ZXing bitmap) + the lookup URL text on the 80 mm receipt/PDF and
  in the ESC/POS raster.
- **`MisaMeInvoiceClient.sendMail(...)`:** call the already-configured
  `send-mail-path`; `EInvoiceService` invokes it after a successful issue when a
  buyer email exists (satisfies "gửi bằng hình thức điện tử").
- **Controller:** `POST /bills/{id}/einvoice/send` (resend).
- **FE:** show QR in `ReceiptPrintDialog`; "Gửi email HĐ" action.
- **Tests:** receipt composition includes QR/lookup; send-mail called with email.

---

## Batch P1 — Tax-ops & data integrity

### G-6 · Period revenue + VAT report (score 6)
- **`InvoiceRepository`:** aggregation (Σ total, subtotal, vatAmount, count)
  grouped by day/month/quarter with the existing filters.
- **Service:** `taxReport(BillReportRequest{fromDate,toDate,groupBy})` →
  `BillReportResponse` (summary + rows).
- **`BillExcelExportService`:** add a "Báo cáo doanh thu & VAT" / bảng kê sheet.
- **Controller:** `POST /bills/tax-report` + `POST /bills/tax-report/export`.
- **FE:** new "Báo cáo thuế" tab (period picker via shared `DateInput` dd/mm/yyyy,
  group-by, totals, export).
- **Tests:** aggregation totals match seeded bills.

### G-7 · Payment idempotency (score 6)
- **`PayRequest`/`PayDebitRequest`:** add `clientTransactionId`.
- **`Invoice`:** add `client_txn_id VARCHAR(64)` + **UNIQUE** index.
  **Changeset 032.**
- **`CorePaymentService`/`CoreBillingService`:** look up an existing invoice by
  `clientTransactionId` before issuing; return it instead of double-charging;
  rely on the unique constraint to win the race.
- **FE:** generate one UUID per payment attempt in `PayConfirm`/`HomePage`;
  reuse it on retry.
- **Tests:** duplicate id returns the same bill, no second insert.

### G-12 · Line-vs-total VAT rounding (conditional: khấu trừ) (score 5)
- **`EInvoiceService`:** compute per-line VAT, then reconcile the residual onto
  the last line so **Σ line VAT == `invoice.vatAmount`** exactly (or send
  header-only totals if MISA accepts). Active only when per-line VAT is shown.
- **Tests:** multi-line bill Σ line VAT equals total VAT.

---

## Batch P2 — Business / config / hygiene

### G-8 · Per-item VAT rate (conditional: khấu trừ + mixed rates) (score 4)
- **`Service` + `service` table:** add `vat_rate DECIMAL(5,2)`.
- **`InvoiceItem`:** add `vat_rate`, `vat_amount`. **Changeset 033.**
- **`CoreBillingService.toItem`:** resolve per-item rate (service → config
  default); `EInvoiceService` uses the per-line rate.
- **FE `SetupPage`:** edit per-service VAT (full-stack consistency).
- **Tests:** mixed-rate bill → correct per-line + total VAT.

### G-15 · Invoice-kind config (score 3)
- **`BillConfig`:** add `invoice_kind` (GTGT | BAN_HANG) + `einvoice_pattern`.
  **Changeset 034.**
- **`EInvoiceService.buildPayload`:** use the configured kind instead of the
  hardcoded `"Hóa đơn bán hàng"`.
- **FE `SetupPage`:** invoice-kind select in the billing section.

### G-5 · Audit log — internal control (score 4)
- **New `InvoiceLog` entity + table** (changeset 035): `invoice_id`, `action`
  (ISSUE/VOID/ADJUST/REPLACE/PRINT/EINVOICE_ISSUE/CONFIG_UPDATE), `actor`, `ip`,
  `detail`, `request/response payload`, `error`, `created_at`.
- Write from `CoreBillingService`/`EInvoiceService`/`BillingController` on each
  sensitive op; `GET /bills/{id}/logs` (admin); `BillingPage` detail "Lịch sử".
- **Tests:** log rows written on issue/void.

### G-10 · Discount line (score 3)
- **`InvoiceItem`:** add `discount_amount` **or** a `DISCOUNT` negative line
  type. **Changeset 036.**
- **`PayRequest`:** optional per-line/invoice discount; `CoreBillingService`
  applies it so `total` = net charged.
- **FE `PayConfirm`:** discount input.
- **Tests:** discounted total correct.

### G-13 · Config: bank account + pattern (score 3)
- **`BillConfig`:** add `bank_name`, `bank_account` (combine with changeset 034).
- **`BillConfigRequest/Response` + `SetupPage`**; `ReceiptComposer` prints bank
  info. **Tests:** config round-trip.

### G-16 · PENDING state hygiene (score 2)
- **`EInvoiceService.issue()`:** set `EInvoiceStatus.PENDING` and save **before**
  the HTTP call, then transition to ISSUED/FAILED (accurate in-flight state on
  crash). **Tests:** status transition.

---

## Batch P3 — Optional

### G-9 · Customer master data (score 2)
- **New `customer` table + entity + repo** (changeset 037): name, tax_code,
  address, phone, email, bank.
- CRUD endpoints; `PayConfirm` autocomplete by name/phone → fills `BuyerInfo`.
- Only if the venue wants faster checkout for regulars/B2B.

---

## 6. Explicitly NOT implemented

| Gap | Reason |
|---|---|
| **G-4 amount-in-words** | Not a mandatory field on hóa đơn máy tính tiền (Điều 11); MISA renders it on standard templates. Optional local-receipt polish only. |
| **G-14 offline queue** | On-prem LAN + local server: payments persist offline; only the MISA call needs internet and is already retryable. The .docx concern targets cloud POS. |

## 7. Dependencies & sequencing

1. **G-1 before G-2** (void depends on the correction path).
2. **G-3 stores the lookup/PDF refs the NEW QR gap renders** — do G-3 then QR.
3. **G-8 before G-12** if both are built (per-line VAT feeds rounding fix).
4. P0 → P1 → P2 → P3; each batch independently shippable behind `bill_config`
   flags where feasible.

## 8. Verification per batch

- **Backend:** extend `CoreBillingServiceTest`; add `EInvoiceServiceTest`
  (mock `MisaMeInvoiceClient`); H2 `@SpringBootTest` `create-drop`.
- **Build:** `mvn -Pdev clean package` → WAR green; FE `npm run build` clean.
- **Manual:** issue → adjust → void; QR renders on receipt/PDF; tax-report
  export; double-submit returns one bill.

---

## 9. Implementation status

All batches are built. Changeset numbering shifted because 029–031 were
already taken when the work landed: the new schema lives in **032–038**
(all registered in `db.changelog-master.xml`; 001–031 untouched).

| Gap | Status | Where |
|---|---|---|
| G-1 adjust / replace | done | `CoreBillingService.issueAdjustmentBill/issueReplacementBill`, `POST /bills/{id}/adjust`·`/replace`, `CorrectionDialog` |
| G-2 correction → void | done | `CoreBillingService.voidBill` guard + `existsByRelatedInvoiceInvoiceId` |
| G-3 XML retention | done | `einvoice_xml`/`einvoice_pdf_url`, `GET /bills/{id}/einvoice/xml`, "Tải XML" |
| G-11 QR + e-mail | done | `ReceiptComposer` + `BillPdfService` + `EscPosNetworkPrinter`, `GET /bills/{id}/qr`, `POST /bills/{id}/einvoice/send` |
| G-6 tax report | done | `POST /bills/tax-report`·`/export`, "Báo cáo thuế" tab |
| G-7 idempotency | done | `client_txn_id` UNIQUE, `CorePaymentService` replay before charging |
| G-12 line-vs-total VAT | done | `applyVat` sums line VAT onto the header, payload reuses the snapshot |
| G-8 per-item VAT | done | `service.vat_rate`, `invoice_item.vat_rate/vat_amount`, Setup table |
| G-15 invoice kind | done | `bill_config.invoice_kind`/`einvoice_pattern`, title + `InvoiceTypeID` |
| G-5 audit log | done | `invoice_log`, `GET /bills/{id}/logs`, "Lịch sử" |
| G-10 discount | done | `DISCOUNT` line + `discount_amount`, `PayConfirm` input |
| G-13 bank/pattern | done | `bill_config.bank_name/bank_account`, printed on TRANSFER receipts |
| G-16 PENDING hygiene | done | PENDING committed in `REQUIRES_NEW` before `client.publish` |
| G-9 customer directory | done | `customer` table + CRUD, `GET /api/v1/customers` (also COORDINATOR), `PayConfirm` autocomplete, Setup catalogue |

Test coverage for the new paths: `EInvoiceServiceTest` (7),
`CorePaymentServiceIdempotencyTest` (1), plus the G-1/G-2/G-7/G-8/G-10 cases
in `CoreBillingServiceTest` — 100 tests green under `mvn -Pdev clean package`.
