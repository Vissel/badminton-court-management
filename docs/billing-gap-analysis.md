# Billing Feature — Gap Analysis

> ⚠️ **SUPERSEDED** — see `billing-gap-analysis-final.md` for the
> Vietnam-tax-accurate re-evaluation (NĐ 254/2026) and
> `billing-implementation-plan.md` for the build plan. This 1st pass cited the
> now-expired NĐ 123/2020 and over-rated G-4 (amount-in-words); it missed the
> mandatory buyer QR/lookup gap.

> Comparison of the business analysis in `Phát Triển Hóa Đơn Điện Tử.docx`
> against the current billing feature (`billing-design.md` + implemented
> code). Date: 2026-11-01.

## 1. Sources compared

| Source | Reference |
|---|---|
| Business analysis | `docs/Phát Triển Hóa Đơn Điện Tử.docx` (business checklist, technical deep-dive, enterprise architecture) |
| Current design | `docs/billing-design.md` |
| Implemented code | `core/billing/CoreBillingService.java`, `core/billing/einvoice/EInvoiceService.java`, `core/billing/einvoice/MisaMeInvoiceClient.java`, `controller/BillingController.java`, `entity/Invoice.java`, `bad-court-mana-ui/src/page/BillingPage.js` |

## 2. Gap scorecard (0–10; higher = larger, more urgent gap)

| # | Gap | Required by .docx | Current state | Score | Suggestion |
|---|---|---|---|---|---|
| G-1 | **Hóa đơn điều chỉnh / thay thế** (Điều 19 NĐ 123) | Mandatory error-handling after issuance: adjustment (tăng/giảm) + replacement invoice | Only internal `VOIDED`; `InvoiceStatus` = {ISSUED, VOIDED}. No adjust/replace entity, link, or flow | **9** | Add `related_invoice_id` + `ADJUSTMENT`/`REPLACEMENT` invoice types, issue via new admin endpoint that references the original bill and sends MISA the correction payload |
| G-2 | **Cancel e-invoice on provider** | "Hủy hóa đơn" lifecycle | `CANCELLED` enum defined but unreachable — no endpoint, no client method. `voidBill` guard says *"cancel the e-invoice first"* but that capability does not exist → a bill with a wrong live e-invoice is a dead end | **8** | Implement MISA cancel API call + `POST /bills/{id}/einvoice/cancel` (admin), then allow void |
| G-3 | **Store & download original XML** | Legally required archive (lưu trữ XML gốc theo NĐ 123) | No XML column/storage; only `einvoice_no/ref/pdf_url` saved. `einvoicePdfUrl` is also never written by `EInvoiceService.issue()` | **8** | Add `einvoice_xml LONGTEXT` (changeset 029+), persist provider XML on issue, add `GET /bills/{id}/einvoice/xml` download; also populate `einvoicePdfUrl` |
| G-4 | **Số tiền bằng chữ** (`TgTTTBChu`) | Mandatory in QĐ 1450 XML + standard on VN invoices | Not implemented anywhere — no `amountInWords` field, no Vietnamese number-to-words util, not sent to MISA, not on receipt/PDF | **7** | Add `MoneyUtils.amountInWords(BigDecimal)` (UTF-8 Vietnamese), persist on bill, include in MISA payload and receipt/PDF rendering |
| G-5 | **Audit log (invoice_logs)** | Tamper-proof history: who created/signed/voided, request/response payloads, for tax inspection | Only `issued_by/voided_by` columns; no log table for print, e-invoice issue, config changes, no old/new values | **7** | Add `invoice_log` table (action, payload, response, error, user, time); write on issue/void/einvoice/config/print; read-only endpoint |
| G-6 | **Bảng kê bán ra / tax reports** | Revenue summary by day/month/quarter + sales list for tờ khai thuế (hộ kê khai) | Excel export is a flat bill list only; no tax-period aggregation, no bảng kê format | **6** | Add `POST /bills/tax-report` (period, grouped totals, VAT split) + bảng kê export template in `BillExcelExportService` |
| G-7 | **Idempotency key on payment** | Client UUID per "Thanh toán" click; retry must return old result, never a 2nd bill | `PayRequest` has no client transaction id; retry after timeout double-charges and creates 2 bills. `TransactionID` = `billNo` only dedups the provider submission | **6** | Add `clientTransactionId` to `PayRequest`/`PayDebitRequest`, unique index on `invoice`, return existing bill on duplicate instead of re-issuing |
| G-8 | **Per-item VAT rates** (0%, 5%, 10%, KCT) | Master data: tax rate per good/service | Single venue-wide `vatRate`; `Service` entity has no tax field; every line uses the bill rate | **5** | Add `vat_rate` to `service` + `invoice_item`; classify per line; fall back to config rate |
| G-9 | **Customer (buyer) master data** | Directory: name, MST, address, phone, email, bank account; reuse at POS | Buyer info captured ad-hoc per bill only (`BuyerInfo`), no table, no autofill for returning business customers | **5** | New `customer` table + CRUD + autocomplete in PayConfirm |
| G-10 | **Discount / chiết khấu** | Auto-calc per line and invoice-level | No discount support; negative lines exist only for advance/new-debt | **4** | Add `discount_amount` to `invoice_item` + optional invoice-level discount in `PayRequest` |
| G-11 | **Email/Zalo delivery** | Auto-send PDF + lookup link via Email/Zalo ZNS | Only payload flags (`IsSendInvoice`, `ReceiverEmail`); `send-mail-path` is configured but never called; no Zalo | **4** | Call MISA send-mail endpoint after issue; optional Zalo ZNS adapter later |
| G-12 | **Rounding consistency (line vs total VAT)** | Tax office rejects if Σ lines ≠ total | `lineVat()` rounds each line to 0 decimals; per-line VATs can drift a few đồng from gross-extracted `vatAmount` | **4** | Compute per-line VATs, then adjust the last line (or send header totals only) so Σ = `TotalVATAmount` exactly |
| G-13 | **Config: bank account + invoice pattern** | Business profile: bank account, Ký hiệu mẫu số + Ký hiệu hóa đơn (e.g. 1C24TAA) | `bill_config` has no bank account; only one `einvoice_series` + `einvoice_template` (no separate mẫu số/ký hiệu) | **3** | Add `bank_name`/`bank_account` and `einvoice_pattern` columns; extend `BillConfigRequest` + SetupPage billing section |
| G-14 | **Offline queue (IndexedDB / PENDING_SYNC)** | POS keeps selling during internet outage | Nothing; but venue runs LAN + local server, and payments persist locally — outage only blocks e-invoice issuance, which is retryable | **3** | Low priority for this deployment; if needed later: `localStorage` queue in FE + resend on reconnect |
| G-15 | **Invoice kind config (GTGT vs bán hàng)** | `KHMSHDon` 1: GTGT / 2: bán hàng per business type | Hardcoded `"Hóa đơn bán hàng"` in `EInvoiceService.buildPayload` | **3** | Add `invoice_kind` to `bill_config` and map to the provider field |
| G-16 | **PENDING state never used** | Lifecycle NONE→PENDING→ISSUED\|FAILED | Enum exists but issue is synchronous; jumps straight to ISSUED/FAILED | **2** | Set `PENDING` before the HTTP call in `issue()` for accurate in-flight state on crash |

## 3. Already covered (no gap)

- **Internal bill on every payment** — issuance inside payment transaction, gapless numbering (`invoice_series` FOR UPDATE), immutability, void keeps number.
- **VAT math** — price-inclusive extraction `net = gross/(1+rate)`, rate snapshot, admin-only override enforced in `resolveVatRate`.
- **T-VAN via adapter** — matches the .docx recommended "Lựa chọn 1" (use provider API, never direct GDT): `MisaMeInvoiceClient` is loosely coupled, token-cached, timeout-bounded, never logs payloads.
- **Print channels** — ESC/POS network raster + browser/USB + 80 mm PDF.
- **Search/export** — date range, keyword, status, type filters; Excel (SXSSF) + PDF download.
- **RBAC** — void/config/e-invoice are ROOT/ADMINISTRATOR server-side; cashier can only create and print (matches .docx "thu ngân không được hủy").
- **SignType 5 (máy tính tiền, no ký số)** — matches the hộ khoán/POS model; digital signature is documented as out of scope in billing-design §1.

## 4. Conscious exclusions (.docx enterprise section — not applicable)

API gateway/WAF, Redis, RabbitMQ/Kafka, WebSocket/SSE, Docker/K8s, Cloud HSM —
these target multi-tenant SaaS; this is a single-venue monolith and the sync
MISA call with FAILED+retry is the right-sized design. No action recommended.

## 5. Suggested fix order

1. **Batch 1 (legal blockers, G-1 + G-2 + G-3 + G-4):** adjustment/replacement + provider cancel + XML archive + amount-in-words — hard legal requirements once the venue actually issues e-invoices.
2. **Batch 2 (compliance hardening, G-5 + G-6 + G-7):** audit log, bảng kê bán ra, idempotency key.
3. **Batch 3 (business convenience, G-8 → G-16):** per-item VAT, customer directory, discounts, email delivery, rounding fix, config extras.

## 6. Implementation constraints

- All schema additions must land in new Liquibase changesets (026/028 are
  applied — never edit them).
- New error text must follow the Vietnamese-guidance convention (never raw
  SQL/stack text to the UI).
