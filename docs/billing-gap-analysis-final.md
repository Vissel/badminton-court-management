# Billing / E-Invoice — FINAL Gap Analysis (Vietnam tax-accurate)

> Supersedes `billing-gap-analysis.md`. This version re-evaluates every gap
> against **current Vietnamese law as of Nov 2026** and against what a
> **single badminton venue issuing hóa đơn máy tính tiền** actually needs.
> Each original gap is marked **VALID / PARTIAL / INVALID**, re-scored, and
> only the valid ones feed the implementation plan
> (`billing-implementation-plan.md`).

## 1. Legal baseline (corrected)

| Instrument | Status (Nov 2026) | Relevance |
|---|---|---|
| **Nghị định 254/2026/NĐ-CP** (hiệu lực 01/07/2026) | **OPERATIVE** — replaces NĐ 123/2020, NĐ 41/2022, NĐ 70/2025 (khoản 2 Điều 43) | Current governing decree for hóa đơn, chứng từ |
| Nghị định 123/2020/NĐ-CP | **Expired 01/07/2026** | Cited by the .docx and the 1st analysis — now outdated |
| Nghị định 70/2025/NĐ-CP (hiệu lực 01/06/2025) | **Expired 01/07/2026** (content largely carried into 254/2026) | Introduced the POS-invoice mandate & content rules |
| Quyết định 1450/QĐ-TCT | Active | XML data-format standard for e-invoices |
| Thông tư 78/2021/TT-BTC | Being superseded under Luật Quản lý thuế 2025 | Guidance referenced by the .docx |

**Correction vs the 1st analysis:** references to "Điều 19 NĐ 123" must be read
as the error-handling rules now in **NĐ 254/2026** (originally Khoản 13 Điều 1
NĐ 70/2025). The legal substance (điều chỉnh / thay thế / thông báo sai sót)
survives; only the citation changes.

## 2. Venue context & invoice model

- The system publishes with **`SignType = "5"` → hóa đơn điện tử khởi tạo từ
  máy tính tiền** (POS e-invoice). This is the **correct model** for a venue
  selling services/goods directly to walk-in consumers.
- **In mandatory scope?** NĐ 70/2025 (carried into 254/2026) requires POS
  e-invoices for hộ/cá nhân kinh doanh **doanh thu ≥ 1 tỷ đồng/năm** and for
  enterprises selling directly to consumers in groups that include **dịch vụ
  vui chơi, giải trí / dịch vụ phục vụ cá nhân khác**. A busy badminton venue
  (court rental + shuttlecock/drinks retail) falls here. *Confirm the venue's
  exact registered tax method (khoán / trực tiếp / khấu trừ) and scope with its
  managing Cục Thuế — that choice drives which content/report rules apply.*
- **No digital signature required** for POS invoices (khoản 9 Điều 8 NĐ
  254/2026) → the design's "out of scope: ký số" is **legally correct**.
- **Buyer info is optional** on a POS invoice — only required *if the buyer
  requests* it (name/address/MST/personal-ID/phone). The system's ad-hoc
  `BuyerInfo` capture is therefore **compliant**.

### Mandatory content of a hóa đơn máy tính tiền (Điều 11 NĐ 123 as amended by NĐ 70/2025, carried into NĐ 254/2026)

1. Seller: name, address, MST — ✅ `bill_config`
2. Buyer: name/address/MST/phone — **only if buyer requests** — ✅ optional `BuyerInfo`
3. Goods/services: name, unit price, quantity, payment price — ✅ `invoice_item`
4. **If taxpayer uses khấu trừ method:** price excl. VAT, VAT rate, VAT amount,
   total incl. VAT — ✅ `subtotal`/`vatRate`/`vatAmount`/`total` (only needed
   under khấu trừ)
5. Issue time — ✅ `issuedAt`
6. **Mã của CQT / thông tin truy xuất + buyer retrieval path** — seller must
   send the e-invoice electronically (SMS/email) **or** provide a **link / QR
   code** for the buyer to look up & download — ❌ **NOT implemented (new gap)**

> **"Số tiền bằng chữ" is NOT in this list** → not legally required for POS
> invoices (this invalidates the 1st analysis's G-4 "mandatory" claim).

## 3. Re-evaluation of the 16 original gaps

Legend: **Validity** = legal + applicability verdict; **New score** = 0–10
(severity for *this* venue under *current* law); **Pri** = P0 legal must-fix,
P1 tax-ops/integrity, P2 business/config, P3 optional, **—** = do not do.

| # | Gap | Validity | Legal basis / why | New score | Pri |
|---|---|---|---|---|---|
| G-1 | Adjustment / replacement invoice | **VALID** | Error handling under NĐ 254/2026 (orig. Khoản 13 Điều 1 NĐ 70/2025): amount/MST/tax-rate errors ⇒ lập HĐ **điều chỉnh** hoặc **thay thế**. System only has internal `VOIDED` — no correction invoice | **9** | **P0** |
| G-2 | "Cancel e-invoice on provider" | **PARTIAL (reframe)** | VN e-invoice law has **no "hủy"** for an issued coded invoice — correction is via G-1. The *real* defect is the **dead-end**: `voidBill` blocks when an e-invoice is live and points to a non-existent "cancel". Fix = correction path (G-1) then allow internal void | **6** | **P0** |
| G-3 | Store/download original XML (+ `einvoicePdfUrl` never written) | **VALID** | Seller must retain e-invoice data (archive/accounting retention, ~10 yrs) and be able to reproduce it. Only `einvoice_no/ref/pdf_url` kept; `einvoicePdfUrl` is never set by `EInvoiceService.issue()` (real bug) | **7** | **P0** |
| G-4 | Số tiền bằng chữ (amount in words) | **INVALID (as "mandatory")** | Not in the mandatory content list for hóa đơn máy tính tiền; MISA renders it on standard templates anyway. Optional polish on the local 80 mm receipt at most | **1** | **—** |
| G-5 | Audit log (`invoice_logs`) | **PARTIAL (downgrade)** | No VN tax rule mandates an internal invoice-log table with payloads. Good **internal control** (who issued/voided/printed/config-changed), not a legal requirement | **4** | **P2** |
| G-6 | Bảng kê bán ra / period tax report | **VALID (method-dependent)** | Venue still files periodic tax (doanh thu/GTGT) per its method; needs a **revenue + VAT summary by day/month/quarter**. Current Excel = flat bill list, no aggregation. ("bảng kê bán ra" strictly applies only to khấu trừ) | **6** | **P1** |
| G-7 | Payment idempotency key | **VALID (integrity)** | Not a tax rule per se, but a duplicate bill on retry **misstates declared revenue** ⇒ indirect legal risk. No client transaction id today | **6** | **P1** |
| G-8 | Per-item VAT rates | **PARTIAL (conditional)** | Only required if venue uses **khấu trừ** AND sells mixed-rate items. For máy tính tiền / trực tiếp / khoán a single total suffices | **4** | **P2** |
| G-9 | Customer (buyer) master data | **INVALID as legal / P3 convenience** | Buyer info is **optional** on a POS invoice. A customer directory is checkout convenience for regulars/B2B only | **2** | **P3** |
| G-10 | Discount / chiết khấu | **PARTIAL (business)** | Discounts must be reflected in the charged total (they are). A dedicated discount line is transparency/convenience, not legally mandated | **3** | **P2** |
| G-11 | Email / Zalo delivery | **VALID (merge w/ new QR gap)** | Điều 11(5): seller must deliver electronically **or** provide link/QR. `IsSendInvoice`/`ReceiverEmail` flags exist but the send-mail endpoint is never called; no QR/link on the receipt for walk-ins | **6** | **P0** |
| G-12 | Line-vs-total VAT rounding | **VALID (conditional)** | .docx + CQT structure check: Σ line VAT must equal total VAT. `lineVat()` rounds each line to 0 decimals → drift. Only bites when per-line VAT is shown (khấu trừ) | **5** | **P1** |
| G-13 | Config: bank account + invoice pattern | **PARTIAL (mostly covered)** | Ký hiệu mẫu số / ký hiệu hóa đơn map to `einvoice_series` + `einvoice_template` (MISA-side). Bank account = optional display field | **3** | **P2** |
| G-14 | Offline queue (IndexedDB / PENDING_SYNC) | **INVALID (not applicable)** | On-prem LAN + local server: payments persist offline; only the MISA call needs internet and is retryable (FAILED→retry). The .docx concern targets cloud POS | **1** | **—** |
| G-15 | Invoice-kind config (GTGT vs bán hàng) | **VALID (minor)** | Hardcoded `"Hóa đơn bán hàng"`. NĐ 254/2026 Điều 8: khấu trừ ⇒ GTGT; trực tiếp ⇒ bán hàng. Should be configurable to match the venue's method | **3** | **P2** |
| G-16 | PENDING state never used | **VALID (hygiene)** | `EInvoiceStatus.PENDING` defined but unreachable (sync issue). Set it before the HTTP call, or remove it | **2** | **P2** |
| **NEW** | **Buyer retrieval path: QR / lookup link on receipt** | **VALID (missed by 1st analysis)** | Điều 11(5) NĐ 70/2025 → NĐ 254/2026: POS invoice must carry **mã CQT / thông tin truy xuất** and the seller must give the buyer a **QR/link** to retrieve it. `ReceiptComposer`/PDF render **no QR and no lookup URL**; no `lookup_code` stored | **7** | **P0** |

## 4. Verdict summary

**VALID & must address (feed the plan):**
- **P0 (legal must-fix):** G-1 adjustment/replacement · G-2 correction→void
  path · G-3 XML retention + `einvoicePdfUrl` bug · G-11+NEW buyer retrieval
  (QR/link + email delivery)
- **P1 (tax-ops / integrity):** G-6 period revenue+VAT report · G-7 payment
  idempotency · G-12 line-vs-total VAT rounding (if khấu trừ)
- **P2 (business / config / hygiene):** G-8 per-item VAT (if khấu trừ) ·
  G-15 invoice-kind config · G-5 audit log (internal control) · G-10 discount ·
  G-13 bank/pattern config · G-16 PENDING hygiene
- **P3 (optional):** G-9 customer master data

**INVALID / DO NOT DO (with reason):**
- **G-4 amount-in-words** — not a mandatory field for hóa đơn máy tính tiền.
- **G-14 offline queue** — not applicable to an on-prem LAN single-venue app.

**Net:** 14 of 16 original gaps survive in some form (2 fully valid+high,
several downgraded/reframed), **2 are invalid** (G-4, G-14), and **1 new legal
gap** (buyer QR/lookup) was found that the 1st analysis missed.

## 5. Key accuracy corrections vs the 1st analysis

1. **Governing law updated** to NĐ 254/2026 (NĐ 123/2020 & 70/2025 expired
   01/07/2026).
2. **G-4 downgraded from "mandatory (7)" to "invalid (1)"** — amount-in-words
   is not required for POS invoices.
3. **New P0 gap added** — buyer QR/lookup retrieval path (Điều 11(5)), which is
   legally required and currently absent.
4. **G-2 reframed** — there is no "hủy" for an issued e-invoice; the fix is the
   điều chỉnh/thay thế path (G-1) plus un-blocking internal void.
5. **G-5 downgraded** — audit log is internal control, not a tax mandate.
6. **G-8/G-12 made conditional** on the venue using the khấu trừ method.
7. **G-14 confirmed not applicable** to this deployment.

> Implementation constraints (carried into the plan): new schema lands in fresh
> Liquibase changesets **029+** (001–028 are frozen); user-facing errors in
> **Vietnamese guidance** only; time via the project's **UTC+7 helpers**; any
> new dialog follows the **sm + draggable** standard; **MUI v9** icon names;
> full-stack editability consistency (FE field ⇄ BE persistence).
