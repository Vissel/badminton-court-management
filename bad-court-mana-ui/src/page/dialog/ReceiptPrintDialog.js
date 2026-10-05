import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import PrintOutlinedIcon from "@mui/icons-material/PrintOutlined";
import { getReceipt, printBill } from "../../api/billApi";
import { emitApiError } from "../../api/errorBus";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";

// Only the receipt area prints — everything else is hidden by this print CSS.
const PRINT_CSS = `
@media print {
  body * { visibility: hidden; }
  .receipt-print-area, .receipt-print-area * { visibility: visible; }
  .receipt-print-area {
    position: fixed;
    left: 0;
    top: 0;
    width: 72mm;
    margin: 0;
    padding: 2mm;
  }
}
`;

const ITEM_TYPE_LABEL = {
  COURT_FEE: "Tiền sân",
  SERVICE: "Dịch vụ",
  RENT_BY_TIME: "Thuê giờ",
  DEBT_PAID: "Trả nợ",
  DEBT_CREATED: "Ghi nợ",
  ADVANCE_DEDUCT: "Trả trước",
};

// issuedAt is stored as an Instant already shifted +7h — format it as UTC
// (same convention as DateTimeUtils.formatVNDateTime) to avoid double-shifting.
const formatIssuedAt = (iso) => {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d)) return iso;
  return d.toLocaleString("vi-VN", { timeZone: "UTC" });
};

const Line = ({ label, value, bold, indent }) => (
  <Box
    sx={{
      display: "flex",
      justifyContent: "space-between",
      fontFamily: "monospace",
      fontSize: "12px",
      fontWeight: bold ? 700 : 400,
      pl: indent ? 2 : 0,
    }}
  >
    <span>{label}</span>
    <span>{value}</span>
  </Box>
);

const ReceiptPrintDialog = ({ show, billId, onClose }) => {
  const [receipt, setReceipt] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!show || !billId) return;
    setLoading(true);
    getReceipt(billId)
      .then((res) => {
        const result = res?.data;
        if (result && result.success && result.data) {
          setReceipt(result.data);
        } else {
          emitApiError(result?.errorMessage || "Không tải được hoá đơn.");
          onClose();
        }
      })
      .catch(() => onClose())
      .finally(() => setLoading(false));
  }, [show, billId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePrint = () => {
    if (receipt?.printerMode === "NETWORK") {
      // Backend renders + pushes an ESC/POS raster job to the printer IP:port
      // and counts the print itself.
      printBill(billId)
        .then((res) => {
          const result = res?.data;
          if (!result?.success) {
            emitApiError(result?.errorMessage || "In hoá đơn thất bại.");
          }
        })
        .catch(() => { });
      return;
    }
    window.print();
    if (billId) printBill(billId, "BROWSER").catch(() => { });
  };

  const bill = receipt?.bill;
  const seller = receipt?.seller;
  const items = bill?.items || [];
  const chargeItems = items.filter(
    (i) => i.itemType !== "ADVANCE_DEDUCT" && i.itemType !== "DEBT_CREATED"
  );
  const deductItems = items.filter(
    (i) => i.itemType === "ADVANCE_DEDUCT" || i.itemType === "DEBT_CREATED"
  );

  return (
    <Dialog open={show} onClose={onClose} maxWidth="xs" fullWidth>
      <style>{PRINT_CSS}</style>
      <DialogContent sx={{ display: "flex", justifyContent: "center" }}>
        {loading || !bill ? (
          <Box sx={{ py: 4 }}>
            <CircularProgress size={28} />
          </Box>
        ) : (
          <Box
            className="receipt-print-area"
            sx={{
              width: 300,
              fontFamily: "monospace",
              color: "black",
              bgcolor: "white",
            }}
          >
            {/* Seller */}
            <Typography
              align="center"
              sx={{ fontFamily: "monospace", fontWeight: 700, fontSize: "14px" }}
            >
              {seller?.businessName || "Sân cầu lông"}
            </Typography>
            {seller?.address && (
              <Typography
                align="center"
                sx={{ fontFamily: "monospace", fontSize: "11px" }}
              >
                {seller.address}
              </Typography>
            )}
            {seller?.phone && (
              <Typography
                align="center"
                sx={{ fontFamily: "monospace", fontSize: "11px" }}
              >
                ĐT: {seller.phone}
              </Typography>
            )}
            {seller?.taxCode && (
              <Typography
                align="center"
                sx={{ fontFamily: "monospace", fontSize: "11px" }}
              >
                MST: {seller.taxCode}
              </Typography>
            )}

            <Divider sx={{ borderStyle: "dashed", my: 1 }} />
            <Typography
              align="center"
              sx={{ fontFamily: "monospace", fontWeight: 700, fontSize: "13px" }}
            >
              HOÁ ĐƠN BÁN HÀNG
            </Typography>
            <Typography
              align="center"
              sx={{ fontFamily: "monospace", fontSize: "12px" }}
            >
              Số: {bill.billNo}
            </Typography>
            <Typography
              align="center"
              sx={{ fontFamily: "monospace", fontSize: "11px" }}
            >
              {formatIssuedAt(bill.issuedAt)}
            </Typography>

            <Divider sx={{ borderStyle: "dashed", my: 1 }} />
            <Line label="Khách hàng" value={bill.buyerName || bill.playerName || ""} />
            {bill.buyerCompany && <Line label="Đơn vị" value={bill.buyerCompany} />}
            {bill.buyerTaxCode && <Line label="MST" value={bill.buyerTaxCode} />}
            {bill.issuedBy && <Line label="Thu ngân" value={bill.issuedBy} />}
            {bill.payType && (
              <Line
                label="Thanh toán"
                value={bill.payType === "TRANSFER" ? "Chuyển khoản" : "Tiền mặt"}
              />
            )}

            <Divider sx={{ borderStyle: "dashed", my: 1 }} />
            {chargeItems.map((item) => (
              <Box key={item.lineNo} sx={{ fontFamily: "monospace", fontSize: "12px" }}>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                  <span>{item.itemName}</span>
                  <span>{formatVND(item.amount)}</span>
                </Box>
                {item.qty > 1 && (
                  <Box sx={{ pl: 2, color: "#555", fontSize: "11px" }}>
                    {item.qty} x {formatVND(item.unitPrice)}
                  </Box>
                )}
              </Box>
            ))}
            {deductItems.map((item) => (
              <Box
                key={item.lineNo}
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  fontFamily: "monospace",
                  fontSize: "12px",
                }}
              >
                <span>{ITEM_TYPE_LABEL[item.itemType] || item.itemName}</span>
                <span>{formatVND(item.amount)}</span>
              </Box>
            ))}

            <Divider sx={{ borderStyle: "dashed", my: 1 }} />
            <Line label="Tổng cộng" value={`${formatVND(bill.total)} ${VN_CURRENCY}`} bold />
            {bill.vatRate > 0 && (
              <>
                <Line indent label={`Trong đó VAT (${bill.vatRate}%)`} value={formatVND(bill.vatAmount)} />
                <Line indent label="Giá chưa VAT" value={formatVND(bill.subtotal)} />
              </>
            )}
            <Line label="Thực thu" value={`${formatVND(bill.collectAmount)} ${VN_CURRENCY}`} bold />

            {bill.status === "VOIDED" && (
              <Typography
                align="center"
                sx={{ fontFamily: "monospace", fontWeight: 700, fontSize: "14px", mt: 1 }}
              >
                *** ĐÃ HUỶ ***
              </Typography>
            )}
            {bill.einvoiceNo && <Line label="HĐĐT" value={bill.einvoiceNo} />}

            {seller?.billFooter && (
              <>
                <Divider sx={{ borderStyle: "dashed", my: 1 }} />
                <Typography
                  align="center"
                  sx={{ fontFamily: "monospace", fontSize: "11px", whiteSpace: "pre-line" }}
                >
                  {seller.billFooter}
                </Typography>
              </>
            )}
            <Typography
              align="center"
              sx={{ fontFamily: "monospace", fontSize: "11px", mt: 1 }}
            >
              Cảm ơn quý khách!
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button variant="outlined" onClick={onClose} fullWidth>
          Đóng
        </Button>
        <Button
          variant="contained"
          startIcon={<PrintOutlinedIcon />}
          onClick={handlePrint}
          disabled={loading || !bill}
          fullWidth
        >
          In hoá đơn
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ReceiptPrintDialog;
