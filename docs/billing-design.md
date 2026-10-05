# Billing & Tax Invoice — Software Design Document

> Feature: commercial-grade billing for the badminton venue. Every money
> collection issues a persisted, immutable, sequentially numbered bill with
> VAT fields. Output channels: browser/USB print, network ESC/POS thermal
> print, PDF, Excel export, and MISA meInvoice e-invoice (HĐĐT).

## 1. Goals and scope

| Requirement | Design decision |
|---|---|
| Every payment produces a bill | Bill issuance runs inside the payment transaction — same commit/rollback boundary |
| Internal bill with VAT fields | VAT stored on the bill (`vatRate`, `vatAmount`, `subtotal`); not a separate tax ledger |
| VAT included in displayed prices | Price-inclusive extraction: `net = gross / (1 + rate)` |
| VAT configurable by Root/Administrator | `bill_config.vatRate` default + per-bill override, both guarded server-side by `ROLE_ROOT`/`ROLE_ADMINISTRATOR` |
| Thermal printer: network ESC/POS + USB/browser | `printerMode` = `NETWORK` | `BROWSER`; one `ReceiptComposer` feeds both renderers |
| E-invoice provider: MISA meInvoice | `EInvoiceService` + `MisaMeInvoiceClient`; credentials in `application.properties` only |
| Audit | Bills are immutable; voiding sets `status=VOIDED` and keeps the number |

Out of scope: digital-signature VAT invoices to the tax authority (SignType
with ký số), credit notes, multi-currency.

## 2. Architecture

```mermaid
flowchart LR
    subgraph FE [React frontend]
        PC[PayConfirm.js<br/>buyer + VAT override]
        RD[ReceiptPrintDialog.js<br/>80mm receipt view]
        BP[BillingPage.js<br/>list/detail/void/export/e-invoice]
        SP[SetupPage.js<br/>billing config]
        BA[billApi.js]
    end

    subgraph BE [Spring backend]
        BC[BillingController<br/>/api/v1/bills]
        BS[BillingService / BillingServiceImpl]
        CB[CoreBillingService<br/>issue · VAT · numbering · void · print]
        CPS[CorePaymentService<br/>checkout hook]
        CDS[CoreDebitService<br/>debit-settlement hook]
        ES[EInvoiceService]
        MC[MisaMeInvoiceClient]
        RC[ReceiptComposer]
        EP[EscPosNetworkPrinter]
        PDF[BillPdfService]
        XL[BillExcelExportService]
    end

    subgraph DB [(MySQL)]
        INV[(invoice)]
        ITM[(invoice_item)]
        SER[(invoice_series)]
        CFG[(bill_config)]
    end

    MISA[(MISA meInvoice<br/>api.meinvoice.vn)]
    PTR[[ESC/POS printer<br/>ip:9100]]

    PC --> BA --> BC --> BS --> CB
    CPS --> CB
    CDS --> CB
    CB --> INV & ITM & SER & CFG
    CB --> RC --> EP --> PTR
    CB --> PDF
    BC --> XL
    BC --> ES --> MC --> MISA
```

`CoreBillingService` is the single writer of `invoice`/`invoice_item`.
Payment flows call it inside their transactions; read paths (list, detail,
receipt, PDF, Excel) go through `BillingService` → the same core service.

## 3. Data model

### `invoice` — bill header (immutable after issue)

```sql
invoice_id        bigint PK auto_increment
bill_no           varchar(30)  UNIQUE      -- e.g. BL000012
invoice_type      varchar(20)              -- CHECKOUT | DEBT_SETTLEMENT
session_id, ava_id, player_id, payment_id  -- traceability FKs
buyer_name/company/tax_code/address/email  -- business-customer snapshot
subtotal          decimal(12,2)            -- net (pre-VAT) extracted from gross
vat_rate          decimal(5,2)             -- VAT % snapshot at issue time
vat_amount        decimal(12,2)
total             decimal(12,2)            -- gross = sum of charge lines
collect_amount    decimal(12,2)            -- cash actually collected
currency          varchar(10)  default 'VND'
pay_type          varchar(20)              -- CASH | TRANSFER
status            varchar(15)  default ISSUED  -- ISSUED | VOIDED
issued_by/at, voided_by/at, void_reason
einvoice_status   default NONE             -- NONE|PENDING|ISSUED|FAILED|CANCELLED
einvoice_no / einvoice_ref / einvoice_pdf_url / einvoice_at
einvoice_error    varchar(500)             -- last provider error (changeset 028)
print_count, last_printed_at
note, created_date
```

### `invoice_item` — line snapshots

```sql
item_id, invoice_id FK, line_no
item_type   -- COURT_FEE | SERVICE | RENT_BY_TIME | DEBT_PAID
            -- | DEBT_CREATED | ADVANCE_DEDUCT
item_name, qty, unit_price, amount   -- amount negative for deduction lines
note
```

Line classification (`CoreBillingService.classify`): the parser maps the
legacy `services` JSON names — `Tiền sân` → COURT_FEE, `rentByTime` →
RENT_BY_TIME, `Trả trước` → ADVANCE_DEDUCT, `Trả nợ` → DEBT_PAID,
`Ghi nợ` → DEBT_CREATED, everything else → SERVICE.

### `invoice_series` — gapless numbering

```sql
series_key  varchar(20) PK   -- bill prefix, e.g. 'BL'
current_no  bigint default 0
```

`nextBillNo()` runs `SELECT … FOR UPDATE` on the series row inside the
issuing transaction → sequential numbers with no gaps (voided numbers stay
reserved).

### `bill_config` — single-row venue profile

Business name, tax code, address, phone, `bill_prefix`, `vat_rate`,
`bill_footer`, `printer_mode` (BROWSER|NETWORK), `printer_ip`,
`printer_port` (9100), `paper_width` (80|58), `einvoice_enabled`,
`einvoice_series`, `einvoice_template`, `auto_print`. Seeded once; the MISA
credentials are *not* here — see §7.

## 4. Core flows

### 4.1 Checkout payment → bill issue

```mermaid
sequenceDiagram
    participant U as Cashier
    participant FE as HomePage/PayConfirm
    participant Pay as PayServiceImpl
    participant CP as CorePaymentService
    participant DA as DebitService (allocateDebitPayment)
    participant CB as CoreBillingService
    participant DB as invoice / invoice_series

    U->>FE: Confirm payment (+ optional buyer info, VAT override)
    FE->>Pay: POST /api/v1/pay/payToPlayer {payType, services, buyer, vatRate?}
    Pay->>CP: payForPlayerAndCreateDebt (transactional)
    alt selected debts to settle
        CP->>DA: allocateDebitPayment(issueBill=false)
        Note over DA: settles debts inside same tx;<br/>no separate bill
    end
    CP->>CP: save AvailablePlayer (leaveTime, payType, payAmount, services JSON)
    CP->>CB: issueCheckoutBill(PaymentDTO, savedPlayer)
    CB->>DB: SELECT current_no FROM invoice_series FOR UPDATE
    CB->>DB: insert invoice + invoice_item lines
    Note over CB: VAT extracted: net = gross/(1+rate);<br/>collect = charges − advance − new debt
    CB-->>CP: Invoice (billId, billNo)
    CP-->>Pay: PaymentDebitModel + bill refs
    Pay-->>FE: PayResponse {billId, billNo}
    FE->>FE: open ReceiptPrintDialog (if auto_print)
    FE->>Pay: GET /api/v1/bills/{id}/receipt → render → print
```

If any step throws, the whole transaction rolls back — a payment can never
exist without its bill and vice versa.

### 4.2 Standalone debt settlement

`POST /api/v1/debit/pay` → `DebitServiceImpl` → `allocateDebitPayment` with
`issueBill=true` → `issueDebitSettlementBill()` creates a
`DEBT_SETTLEMENT` bill for the collected amount. The `issueBill` flag on
`AllocateDebitPaymentRequest` exists solely to prevent double billing:
checkout calls the same allocator with `false` because settled debts appear
as `Trả nợ` lines on the checkout bill.

### 4.3 Print dispatch

```mermaid
sequenceDiagram
    participant FE as ReceiptPrintDialog
    participant BC as BillingController
    participant CB as CoreBillingService
    participant RC as ReceiptComposer
    participant EP as EscPosNetworkPrinter
    participant P as ESC/POS printer

    FE->>BC: POST /bills/{id}/print?channel=...
    BC->>CB: printBill(billId, channel)
    alt channel = NETWORK (or configured mode)
        CB->>RC: compose(invoice, config) → ReceiptDocument
        CB->>EP: send(document, printerIp, port)
        EP->>EP: render lines as bitmap (DejaVu Sans)
        EP->>P: ESC @ + GS v 0 raster + feed + cut → :9100
    else BROWSER
        Note over CB: only print_count/last_printed_at<br/>recorded; FE already ran window.print()
    end
    CB-->>BC: BillResponse (printCount updated)
```

The raster approach avoids ESC/POS code-page problems with Vietnamese
diacritics — the printer receives image data, not text.

### 4.4 E-invoice lifecycle

```mermaid
sequenceDiagram
    participant A as Admin
    participant BC as BillingController
    participant ES as EInvoiceService
    participant MC as MisaMeInvoiceClient
    participant M as MISA meInvoice

    A->>BC: POST /bills/{id}/einvoice
    BC->>BC: hasAdminRole() else 403
    BC->>ES: issue(billId)
    ES->>ES: guard: einvoiceEnabled, bill ISSUED, not already ISSUED
    ES->>MC: POST token → access_token
    ES->>MC: POST /invoice (buyer, lines, VAT, series/template)
    MC->>M: request
    alt success
        ES->>ES: einvoice_status=ISSUED + einvoice_no/ref/pdf_url
    else failure
        ES->>ES: einvoice_status=FAILED + einvoice_error (ops review)
    end
    Note over A,M: GET /einvoice/status re-polls MISA;<br/>POST /einvoice retries a FAILED bill;<br/>GET /einvoice/pdf downloads the provider PDF
```

Statuses: `NONE → PENDING → ISSUED | FAILED`; `CANCELLED` reserved for a
future cancel flow. `einvoice_error` records the last provider message so
cashiers can read *why* without server logs.

## 5. VAT & money math

```
gross   = Σ positive line amounts            → invoice.total
net     = gross / (1 + vatRate)              → invoice.subtotal
vat     = gross − net                        → invoice.vat_amount
collect = gross + Σ deduction lines          → invoice.collect_amount
          (advance deduct + new debt are negative lines)
```

- Rate resolution: request override (admin only) → `bill_config.vatRate` → 0.
- Rate snapshot on the bill; later config changes never rewrite history.
- `collectAmount` is computed from lines, not trusted from `payAmount` —
  the two differ whenever advance or new debt is involved.

## 6. Security

- Read endpoints (`/list`, `/{id}`, `/receipt`, exports, e-invoice status):
  any authenticated user.
- Mutations restricted server-side to `ROLE_ROOT`/`ROLE_ADMINISTRATOR`
  (`hasAdminRole()` in `BillingController`): void, config update, printer
  test, e-invoice issue/retry. The FE hides these controls, but the check
  is enforced on the server — the FE is bypassable.
- Per-bill VAT override also re-checked in `CoreBillingService.resolveVatRate`.
- MISA credentials: `einvoice.misa.*` in `application.properties`;
  `BillConfigResponse` deliberately omits them — never stored in DB or
  returned by the API.

## 7. Configuration

`application.properties`:

```properties
einvoice.misa.base-url=https://api.meinvoice.vn
einvoice.misa.app-id=...
einvoice.misa.tax-code=...
einvoice.misa.username=...
einvoice.misa.password=...
einvoice.misa.timeout-seconds=15
# path overrides if MISA moves endpoints:
einvoice.misa.token-path=/api/integration/token
einvoice.misa.publish-path=/api/integration/invoice
einvoice.misa.status-path=/api/integration/invoice/status
einvoice.misa.download-path=/api/integration/invoice/download
einvoice.misa.send-mail-path=/api/integration/invoice/send-mail
```

`EInvoiceProperties.isConfigured()` short-circuits issue when credentials
are absent → clean `BusinessException` instead of a provider 401.

## 8. API surface (`/api/v1/bills`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/list` | user | Filtered, paginated bill list (`BillListRequest`: dates, type, status, search, page) |
| POST | `/export` | user | Same filter → `bills.xlsx` stream (SXSSF) |
| GET | `/{billId}` | user | Bill detail with items |
| GET | `/{billId}/receipt` | user | Bill + seller profile merged for the print view |
| GET | `/{billId}/pdf` | user | 80mm PDF receipt (PDFBox) |
| POST | `/{billId}/print?channel=` | user | Record/push print; `NETWORK` sends ESC/POS |
| POST | `/printer/test` | admin | Probe `printer_ip:printer_port` |
| POST | `/{billId}/void` | admin | `VOIDED` + reason, number stays reserved |
| POST | `/{billId}/einvoice` | admin | Publish/retry to MISA |
| GET | `/{billId}/einvoice/status` | user | Refresh einvoice_* from provider |
| GET | `/{billId}/einvoice/pdf` | user | Provider-side PDF download |
| GET | `/config` | user | Billing config (no secrets) |
| PUT | `/config` | admin | Update billing config |

Streaming endpoints wrap checked `BusinessException` into `IOException` —
`StreamingResponseBody.writeTo` accepts only `IOException`.

Payment endpoints touched: `POST /api/v1/pay/payToPlayer` (returns
`billId`/`billNo`), `POST /api/v1/debit/pay` (issues DEBT_SETTLEMENT bill).
`PayRequest`/`PayDebitRequest` accept `buyer{...}` + optional `vatRate`.

## 9. Component map

| Layer | Component | File |
|---|---|---|
| Controller | BillingController | `controller/BillingController.java` |
| Service iface | BillingService / BillingServiceImpl | `service/BillingService*.java` |
| Core | CoreBillingService | `core/billing/CoreBillingService.java` |
| Entities | Invoice, InvoiceItem, InvoiceSeries, BillConfig | `entity/` |
| Repositories | InvoiceRepository, InvoiceItemRepository, InvoiceSeriesRepository, BillConfigRepository | `repository/` |
| Print | ReceiptDocument, ReceiptComposer, EscPosNetworkPrinter | `core/billing/print/` |
| Exports | BillPdfService (PDFBox), BillExcelExportService (POI SXSSF) | `core/billing/print/` |
| E-invoice | EInvoiceService, MisaMeInvoiceClient, EInvoiceProperties | `core/billing/einvoice/` |
| Hooks | CorePaymentService.payForPlayerAndCreateDebt, CoreDebitService.allocateDebitPayment | `core/` |
| DTOs | BillResponse, BillItemResponse, ReceiptResponse, BillConfigResponse / BillListRequest, BillConfigRequest, VoidBillRequest, BuyerInfo | `requestmodel.billing` / `response.billing` |
| FE | billApi.js, ReceiptPrintDialog.js, BillingPage.js, PayConfirm.js, SetupPage.js | `bad-court-mana-ui/src/` |
| Schema | `db/changelog/billing-tables.sql` (026), `billing-einvoice-error.sql` (028) | resources |
| Tests | CoreBillingServiceTest (7 tests: numbering, VAT, collect, buyer, void) | `src/test/` |

## 10. Error & transaction guarantees

- Bill issue runs inside the payment tx (per `AGENTS.md` convention) —
  `BusinessException` messages propagate to the user unchanged while the tx
  rolls back.
- `invoice_series` row lock makes concurrent checkouts serialize on the
  counter — no duplicate numbers.
- Network-printer and MISA failures never roll back a payment: printing and
  e-invoice are post-issue actions with their own status/error columns.
- Void requires a reason; `VoidBillRequest.reason` is persisted on the bill.

## 11. Testing

- `CoreBillingServiceTest` (H2, `@SpringBootTest`, `create-drop`): bill
  numbering, VAT-inclusive math, collect-amount vs total, buyer snapshot,
  debt-settlement bills, void.
- Full suite: 55 tests green; `mvn -Pdev clean package` → WAR.
- FE: `npm run build` clean (2 pre-existing hook warnings unrelated to billing).
- Liquibase: 026 applied once — schema additions land in new changesets
  (028 added `einvoice_error` after 026 shipped locally; never edit an
  applied changeset).
