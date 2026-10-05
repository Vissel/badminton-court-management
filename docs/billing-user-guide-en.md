# Billing & Tax Invoice — User Guide (English)

> Feature: every payment produces a numbered bill (hóa đơn) with VAT fields.
> Bills can be printed on a thermal/USB receipt printer, exported to PDF/Excel,
> voided for audit, and published as a MISA meInvoice e-invoice (HĐĐT).

## 1. Who can do what

| Capability | Cashier / Staff | Administrator / Root |
|---|---|---|
| Take payment, auto-issue a bill | ✔ | ✔ |
| Enter buyer/company/tax info at payment | ✔ | ✔ |
| View bill list, bill detail, reprint | ✔ | ✔ |
| Download bill PDF, export Excel | ✔ | ✔ |
| Refresh e-invoice status | ✔ | ✔ |
| Override the VAT rate on a single bill | | ✔ |
| Void a bill | | ✔ |
| Issue / retry the e-invoice (HĐĐT) | | ✔ |
| Change billing & printer configuration | | ✔ |
| Test the network printer | | ✔ |

## 2. Taking a payment (bill is created automatically)

Every payment creates a bill — nothing extra to remember.

1. On the Home page, press **Kết thúc** on a court to finish the game.
2. The payment dialog (**Xác nhận thanh toán**) opens. Review the lines:
   court fee (Tiền sân), services, shuttle balls, time rental (Thuê giờ),
   paid debts (Trả nợ), the advance deduction (Trả trước), and any new
   debt (Ghi nợ).
3. Choose **Phương thức thanh toán**: Tiền mặt (cash) or Chuyển khoản
   (transfer).
4. Optionally fill in the buyer section (see §3).
5. Confirm payment. The bill is issued in the same transaction — if the bill
   cannot be issued, the payment is not saved either.

After a successful payment, the receipt window opens automatically when
**Mở in sau thanh toán** (auto-print) is enabled in Setup.

The same applies when you collect debt from the **Quản lý nợ** page: a
**Thu nợ** (debt settlement) bill is issued for the collected amount.

## 3. Business customer information

Open the buyer section inside the payment dialog when the customer needs a
bill for a company:

- **Tên người mua** — buyer name
- **Tên công ty** — company name
- **Mã số thuế** — tax code (MST)
- **Địa chỉ** — address
- **Email nhận HĐĐT** — email to receive the e-invoice

These fields are snapshotted onto the bill and printed on the receipt /
sent to MISA.

Administrators may also see a **VAT rate** field to override the default
rate for that one bill. Staff use the configured default.

## 4. VAT (price-inclusive)

- All prices shown to the customer **already include VAT** — the amount on
  the screen is the amount to collect.
- The receipt shows **Giá chưa VAT** (net), the VAT amount, and the VAT rate
  used on the bill.
- The default VAT % is set in Setup (admin/root). VAT is extracted from the
  gross total: `net = total / (1 + rate)`.
- If the venue is not VAT-registered, keep the rate at **0** — bills still
  work normally.

## 5. Printing the receipt

Two print modes, chosen in Setup → **Kiểu in**:

| Mode | When to use | How it works |
|---|---|---|
| **In qua trình duyệt (USB)** | Printer attached by USB to the cashier computer | The receipt page renders in the browser and `window.print()` opens the OS print dialog — pick the USB printer and print |
| **In mạng (ESC/POS)** | Thermal printer on the network (Ethernet/Wi-Fi) | The server sends the receipt straight to `IP máy in : Cổng` (default port 9100) — no print dialog |

- **Khổ giấy (mm)** supports 80mm (default) or 58mm rolls.
- Vietnamese characters print correctly on the network path because the
  receipt is rendered as an image, not sent as text codes.
- Reprint any bill later from the **Hoá đơn** page → bill detail → print.

### Printer test (admin)

Setup → billing section → test button pings the configured network printer
(**Kết nối máy in thành công!** / **Không kết nối được máy in.**). Use it
after changing IP/port.

## 6. Hoá đơn page — bill management

Menu → **Hoá đơn** (`/bills`).

- **Tìm kiếm** — search by bill number, customer name, or tax code.
- **Từ ngày / Đến ngày** — filter by issue date.
- **Loại** — payment bill vs **Thu nợ** (debt settlement).
- **Trạng thái** — ISSUED / Đã huỷ.
- Click a row or **Chi tiết** to see the full bill: lines, net/VAT/total,
  **Thực thu** (amount actually collected), buyer info, e-invoice status.

### Actions on a bill

- **In hoá đơn** — reprint via the configured channel.
- **Tải PDF** — download an 80mm PDF copy of the receipt.
- **Huỷ hoá đơn** (admin) — void the bill. You must enter **Lý do huỷ**
  (void reason). The bill is *not* deleted — it stays in the list as
  **Đã huỷ** and its bill number stays reserved, so the audit trail is
  complete.
- **Excel export** — export the currently filtered list to `bills.xlsx`.

## 7. E-invoice (HĐĐT — MISA meInvoice)

When **Bật HĐĐT** is on in Setup:

1. Open the bill → **Phát hành HĐĐT**. The bill is sent to MISA meInvoice.
2. Statuses on the bill: NONE → PENDING → **Đã phát hành** (ISSUED) or
   FAILED. Use refresh to pull the latest status from MISA.
3. When issued, the bill stores the e-invoice number/reference and you can
   **download the e-invoice PDF** (the official MISA PDF, different from the
   internal receipt PDF).
4. If issuing fails, the error is recorded on the bill (visible in the
   detail) — fix the cause and press **Thử lại HĐĐT**.

> Issuing an e-invoice is a legal act — restricted to admin/root.
> MISA credentials are configured in the server
> `application.properties` (`einvoice.misa.*`), never in the app UI.

## 8. Billing configuration (Setup — admin/root)

Setup page → billing (Hoá đơn / Thuế) section:

| Field | Meaning |
|---|---|
| **Tên đơn vị kinh doanh** | Seller name printed on receipts |
| **Mã số thuế** | Venue tax code |
| **Địa chỉ** / **Điện thoại** | Seller contact lines on the receipt |
| **Tiền tố số HĐ** | Bill number prefix (default `BL` → `BL000001…`) |
| **Thuế suất VAT (%)** | Default VAT rate, 0–99 |
| **Nội dung cuối hoá đơn** | Receipt footer line |
| **Kiểu in** | BROWSER (USB) or NETWORK (ESC/POS) |
| **IP máy in** / **Cổng máy in** | Network printer address (default port 9100) |
| **Khổ giấy (mm)** | 80 or 58 |
| **Bật HĐĐT** | Enable MISA meInvoice integration |
| **Ký hiệu HĐĐT** / **Mẫu số HĐĐT** | MISA invoice series / template code |
| **Mở in sau thanh toán** | Auto-open the receipt after each payment |
| **Giá bán đã bao gồm VAT** | Displayed prices include VAT (fixed behaviour) |

Save → **Lưu cấu hình hoá đơn thành công!**

## 9. Troubleshooting

| Symptom | Check |
|---|---|
| Nothing prints (network mode) | Printer IP/port correct? Printer on the same LAN? Run the printer test in Setup. |
| Print dialog appears instead | Printer mode is BROWSER — switch to NETWORK for direct thermal printing, or pick the USB printer in the dialog. |
| E-invoice FAILED | Open the bill detail — the provider error is stored on the bill. Common causes: missing buyer tax code, wrong series/template, expired MISA credentials. Fix and **Thử lại HĐĐT**. |
| Bill number skipped | Voided bills keep their number — this is intentional for audit. |
| Payment saved but no bill | Should not happen — bill issuance is in the same transaction; if the bill failed, the payment rolled back. Check with admin if a payment seems missing. |

## 10. Terms

| Vietnamese | English |
|---|---|
| Hoá đơn | Bill / invoice |
| HĐĐT | E-invoice (MISA meInvoice) |
| Thực thu | Amount actually collected (after advance/debt adjustments) |
| Trả trước | Advance payment (deduction line) |
| Trả nợ | Debt repayment line |
| Ghi nợ | Newly recorded debt (reduces amount collected) |
| Đã huỷ | Voided |
