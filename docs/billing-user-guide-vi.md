# Hướng dẫn sử dụng — Hoá đơn & Thuế (Tiếng Việt)

> Tính năng: mỗi lần thu tiền hệ thống tự xuất một hoá đơn (bill) có số thứ tự
> và đầy đủ thông tin thuế VAT. Hoá đơn in được trên máy in nhiệt (USB/mạng),
> xuất PDF/Excel, huỷ có lưu vết kiểm toán, và phát hành hoá đơn điện tử
> (HĐĐT) qua MISA meInvoice.

## 1. Ai được làm gì

| Việc | Thu ngân | Quản trị viên / Root |
|---|---|---|
| Thu tiền, tự xuất hoá đơn | ✔ | ✔ |
| Nhập thông tin người mua/công ty lúc thu | ✔ | ✔ |
| Xem danh sách, chi tiết hoá đơn, in lại | ✔ | ✔ |
| Tải PDF, xuất Excel | ✔ | ✔ |
| Cập nhật trạng thái HĐĐT | ✔ | ✔ |
| Đổi thuế suất VAT cho từng bill | | ✔ |
| Huỷ hoá đơn | | ✔ |
| Phát hành / thử lại HĐĐT | | ✔ |
| Đổi cấu hình hoá đơn & máy in | | ✔ |
| Test máy in mạng | | ✔ |

## 2. Thu tiền (hoá đơn tự xuất)

Mỗi lần thu tiền là một hoá đơn — mình hông cần làm gì thêm.

1. Ở trang chủ, bấm **Kết thúc** trên sân để kết thúc trận.
2. Hộp thoại **Xác nhận thanh toán** hiện ra. Kiểm lại các dòng: tiền sân,
   dịch vụ, tiền cầu, thuê giờ, nợ cũ thu luôn (Trả nợ), phần trừ tiền
   trả trước, với phần ghi nợ mới (Ghi nợ) nếu có.
3. Chọn **Phương thức thanh toán**: **Tiền mặt** hay **Chuyển khoản**.
4. Khách công ty thì mở phần thông tin người mua (xem mục 3).
5. Bấm xác nhận. Hoá đơn được xuất cùng lúc với thanh toán — nếu xuất bill
   bị lỗi thì thanh toán cũng hông được lưu, hông lo lệch số liệu.

Nếu trong Cài đặt có bật **Mở in sau thanh toán**, màn hình in bill sẽ tự
hiện ra sau khi thu xong.

Thu nợ ở trang **Quản lý nợ** cũng vậy — hệ thống xuất hoá đơn loại
**Thu nợ** cho số tiền đã thu.

## 3. Thông tin khách công ty

Trong hộp thoại thanh toán, mở phần người mua khi khách cần xuất bill công ty:

- **Tên người mua**
- **Tên công ty**
- **Mã số thuế** (MST)
- **Địa chỉ**
- **Email nhận HĐĐT** — email để gửi hoá đơn điện tử

Thông tin này được lưu vô bill, in ra hoá đơn và gửi qua MISA luôn.

Quản trị viên còn thấy ô **thuế suất VAT** để chỉnh riêng cho bill đó nếu
cần; thu ngân dùng thuế suất mặc định trong Cài đặt.

## 4. Thuế VAT (giá đã gồm thuế)

- Giá hiện trên màn hình **đã bao gồm VAT** — khách đưa đúng số đó thôi.
- Trên bill có dòng **Giá chưa VAT**, tiền VAT, với thuế suất áp dụng.
- Thuế suất mặc định nằm trong Cài đặt (quản trị viên chỉnh). Thuế được
  bóc ra từ tổng: `giá chưa VAT = tổng / (1 + thuế suất)`.
- Sân chưa đăng ký thuế thì để **0%** — bill vẫn xuất bình thường.

## 5. In hoá đơn

Có 2 kiểu in, chọn trong Cài đặt → **Kiểu in**:

| Kiểu | Xài khi nào | Cách hoạt động |
|---|---|---|
| **In qua trình duyệt (USB)** | Máy in cắm USB vô máy tính thu ngân | Bill hiện ra trên trình duyệt rồi mở hộp thoại in của Windows — chọn máy in USB rồi in |
| **In mạng (ESC/POS)** | Máy in nhiệt cắm mạng LAN/Wi-Fi | Server gửi bill thẳng tới `IP máy in : Cổng` (mặc định 9100) — hông cần hộp thoại in |

- **Khổ giấy (mm)**: hỗ trợ khổ 80 (mặc định) hay 58.
- In mạng vẫn ra chữ Việt đúng vì bill được vẽ thành hình chứ hông gửi mã
  chữ — hông sợ lỗi font.
- Muốn in lại bill cũ: vô trang **Hoá đơn** → bấm vô bill → bấm in.

### Test máy in (quản trị viên)

Cài đặt → phần hoá đơn → nút test sẽ thử kết nối máy in mạng
(**Kết nối máy in thành công!** / **Không kết nối được máy in.**). Nhớ test
sau khi đổi IP/cổng nha.

## 6. Trang Hoá đơn — quản lý bill

Menu trên → **Hoá đơn**.

- **Tìm kiếm** — tìm theo số hoá đơn, tên khách, hay mã số thuế.
- **Từ ngày / Đến ngày** — lọc theo ngày xuất bill.
- **Loại** — bill thanh toán hay **Thu nợ**.
- **Trạng thái** — đã phát hành / **Đã huỷ**.
- Bấm vô dòng bill (hoặc nút **Chi tiết**) để xem đủ: từng dòng tiền, giá
  chưa VAT, tiền VAT, tổng cộng, **Thực thu** (tiền thật sự thu được),
  thông tin người mua, trạng thái HĐĐT.

### Thao tác trên bill

- **In hoá đơn** — in lại theo kiểu in đang cấu hình.
- **Tải PDF** — tải file PDF khổ 80mm của bill.
- **Huỷ hoá đơn** (quản trị viên) — phải nhập **Lý do huỷ**. Bill hông bị
  xoá mà chuyển thành **Đã huỷ**, số bill vẫn giữ nguyên để đối chiếu sổ
  sách sau này.
- **Xuất Excel** — tải về file `bills.xlsx` đúng theo danh sách đang lọc.

## 7. Hoá đơn điện tử (HĐĐT — MISA meInvoice)

Khi đã bật **Bật HĐĐT** trong Cài đặt:

1. Mở bill → bấm **Phát hành HĐĐT** để gửi bill qua MISA meInvoice.
2. Trạng thái trên bill: NONE → PENDING → **Đã phát hành** hoặc FAILED.
   Bấm cập nhật trạng thái để lấy kết quả mới nhất từ MISA.
3. Khi đã phát hành, bill lưu số hoá đơn điện tử và mình **tải PDF HĐĐT**
   về được (đây là file PDF chính thức từ MISA, khác với PDF bill nội bộ).
4. Nếu phát hành lỗi, lỗi được lưu ngay trên bill — xem lỗi trong chi tiết,
   sửa xong bấm **Thử lại HĐĐT**.

> Phát hành HĐĐT là nghiệp vụ pháp lý — chỉ quản trị viên mới làm được.
> Tài khoản MISA nằm trong file `application.properties` của server
> (`einvoice.misa.*`), hông nhập trong app.

## 8. Cấu hình hoá đơn (Cài đặt — quản trị viên)

Trang Cài đặt → phần hoá đơn:

| Ô | Ý nghĩa |
|---|---|
| **Tên đơn vị kinh doanh** | Tên sân in trên bill |
| **Mã số thuế** | MST của sân |
| **Địa chỉ** / **Điện thoại** | Thông tin liên hệ trên bill |
| **Tiền tố số HĐ** | Tiền tố số bill (mặc định `BL` → `BL000001…`) |
| **Thuế suất VAT (%)** | Thuế suất mặc định, từ 0–99 |
| **Nội dung cuối hoá đơn** | Dòng chữ cuối bill (lời cảm ơn, khuyến mãi…) |
| **Kiểu in** | In qua trình duyệt (USB) hay In mạng (ESC/POS) |
| **IP máy in** / **Cổng máy in** | Địa chỉ máy in mạng (cổng mặc định 9100) |
| **Khổ giấy (mm)** | 80 hay 58 |
| **Bật HĐĐT** | Bật tích hợp MISA meInvoice |
| **Ký hiệu HĐĐT** / **Mẫu số HĐĐT** | Ký hiệu / mẫu số hóa đơn trên MISA |
| **Mở in sau thanh toán** | Tự mở màn hình in sau mỗi lần thu |
| **Giá bán đã bao gồm VAT** | Giá hiển thị đã gồm thuế (cố định) |

Lưu xong báo **Lưu cấu hình hoá đơn thành công!**

## 9. Xử lý sự cố

| Triệu chứng | Kiểm tra |
|---|---|
| In mạng hông ra | IP/cổng đúng chưa? Máy in cùng mạng LAN chưa? Bấm test máy in trong Cài đặt. |
| Hiện hộp thoại in thay vì in thẳng | Đang để kiểu in BROWSER — đổi sang In mạng, hoặc chọn đúng máy in USB trong hộp thoại. |
| HĐĐT FAILED | Mở chi tiết bill xem lỗi MISA trả về. Thường gặp: thiếu MST người mua, sai ký hiệu/mẫu số, tài khoản MISA hết hạn. Sửa xong bấm **Thử lại HĐĐT**. |
| Số bill bị nhảy | Bill đã huỷ vẫn giữ số — cố ý vậy để sổ sách khớp. |
| Thu tiền mà hông có bill | Hông xảy ra được — bill xuất cùng giao dịch thu tiền; bill lỗi thì thanh toán cũng rollback. Báo quản trị viên kiểm tra. |

## 10. Từ vựng

| Từ | Nghĩa |
|---|---|
| Hoá đơn (bill) | Chứng từ thu tiền có số thứ tự |
| HĐĐT | Hoá đơn điện tử qua MISA meInvoice |
| Thực thu | Tiền thật sự thu (sau khi trừ trả trước, trừ nợ mới) |
| Trả trước | Tiền khách ứng lúc vô chơi (dòng trừ) |
| Trả nợ | Dòng thu nợ cũ trong bill |
| Ghi nợ | Khoản nợ mới — giảm tiền thu ngay |
| Đã huỷ | Bill bị huỷ, vẫn lưu lại để kiểm tra |
