import React, { useCallback, useContext, useEffect, useState } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Divider from "@mui/material/Divider";
import Tooltip from "@mui/material/Tooltip";
import Tabs from "@mui/material/Tabs";
import Tab from "@mui/material/Tab";
import PrintOutlinedIcon from "@mui/icons-material/PrintOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import BlockOutlinedIcon from "@mui/icons-material/BlockOutlined";
import SearchIcon from "@mui/icons-material/Search";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import PictureAsPdfIcon from "@mui/icons-material/PictureAsPdf";
import EditNoteOutlinedIcon from "@mui/icons-material/EditNoteOutlined";
import SwapHorizOutlinedIcon from "@mui/icons-material/SwapHorizOutlined";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import CodeOutlinedIcon from "@mui/icons-material/CodeOutlined";
import MailOutlineOutlinedIcon from "@mui/icons-material/MailOutlineOutlined";
import {
  listBills,
  getBill,
  voidBill,
  exportBillsExcel,
  downloadBillPdf,
  getBillConfig,
  issueEInvoice,
  refreshEInvoiceStatus,
  downloadEInvoicePdf,
  downloadEInvoiceXml,
  sendEInvoiceMail,
  getBillLogs,
} from "../api/billApi";
import { emitApiError } from "../api/errorBus";
import { AuthContext } from "../context/AuthContext";
import ReceiptPrintDialog from "./dialog/ReceiptPrintDialog";
import CorrectionDialog from "./dialog/CorrectionDialog";
import DraggableResizablePaper, {
  DIALOG_DRAG_HANDLE,
} from "./dialog/DraggableResizablePaper";
import TaxReportPanel from "./TaxReportPanel";
import DateInput from "./DateInput";
import { downloadExport } from "./DebtManagementPage";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";

const PAGE_SIZE = 15;

const STATUS_COLOR = { ISSUED: "success", VOIDED: "error" };
const STATUS_LABEL = { ISSUED: "Đã phát hành", VOIDED: "Đã huỷ" };
const TYPE_LABEL = {
  CHECKOUT: "Thanh toán",
  DEBT_SETTLEMENT: "Thu nợ",
  ADJUSTMENT: "Điều chỉnh",
  REPLACEMENT: "Thay thế",
};
const CORRECTION_LABEL = { ADJUSTMENT: "Điều chỉnh", REPLACEMENT: "Thay thế" };
// Internal-control trail actions (invoice_log.action).
const LOG_ACTION_LABEL = {
  ISSUE: "Phát hành",
  PRINT: "In",
  VOID: "Huỷ",
  ADJUST: "Điều chỉnh",
  REPLACE: "Thay thế",
  DISCOUNT: "Giảm giá",
  EINVOICE_ISSUE: "Phát hành HĐĐT",
  EINVOICE_SUBMIT: "Gửi HĐĐT",
  EINVOICE_STATUS: "Cập nhật HĐĐT",
  EINVOICE_MAIL: "Gửi email HĐĐT",
  EINVOICE_SEND: "Gửi HĐĐT",
  CONFIG_UPDATE: "Cập nhật cấu hình",
};
const EINVOICE_LABEL = {
  NONE: "—",
  PENDING: "Đang gửi",
  ISSUED: "Đã phát hành",
  FAILED: "Lỗi",
  CANCELLED: "Đã huỷ",
};

// issuedAt is stored as an Instant already shifted +7h — render as UTC.
const formatInstant = (iso) => {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d)) return iso;
  return d.toLocaleString("vi-VN", { timeZone: "UTC" });
};

// yyyy-MM-dd -> next-day yyyy-MM-dd (toDate is exclusive on the backend)
const plusOneDay = (yyyyMMdd) => {
  const d = new Date(`${yyyyMMdd}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + 1);
  return d.toISOString().slice(0, 10);
};

const PageHeader = ({ tab, onTabChange, showTax }) => (
  <>
    <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
      Quản lý hoá đơn
    </Typography>
    {showTax && (
      <Tabs
        value={tab}
        onChange={onTabChange}
        sx={{ mb: 2, borderBottom: 1, borderColor: "divider" }}
      >
        <Tab label="Hoá đơn" />
        <Tab label="Báo cáo thuế" />
      </Tabs>
    )}
  </>
);

export default function BillingPage() {
  const { hasRole } = useContext(AuthContext);
  const canVoid = hasRole("Root", "Administrator");

  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [invoiceType, setInvoiceType] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(false);

  // Detail dialog / void dialog / receipt dialog state
  const [detailBill, setDetailBill] = useState(null);
  const [voidTarget, setVoidTarget] = useState(null);
  const [voidReason, setVoidReason] = useState("");
  const [printBillId, setPrintBillId] = useState(null);
  const [exporting, setExporting] = useState(false);
  // einvoiceEnabled gates the "Phát hành HĐĐT" actions.
  const [einvoiceEnabled, setEinvoiceEnabled] = useState(false);
  const [einvoiceBusy, setEinvoiceBusy] = useState(false);
  const [tab, setTab] = useState(0);
  // Correction dialog: { bill, mode } where mode is "ADJUST" | "REPLACE".
  const [correction, setCorrection] = useState(null);
  const [traceTarget, setTraceTarget] = useState(null);
  const [traceLogs, setTraceLogs] = useState([]);
  const [traceLoading, setTraceLoading] = useState(false);

  useEffect(() => {
    getBillConfig()
      .then((res) => {
        if (res?.data?.success && res.data.data) {
          setEinvoiceEnabled(!!res.data.data.einvoiceEnabled);
        }
      })
      .catch(() => { });
  }, []);

  const currentFilters = useCallback(
    () => ({
      keyword: keyword.trim() || null,
      status: status || null,
      invoiceType: invoiceType || null,
      fromDate: fromDate || null,
      toDate: toDate ? plusOneDay(toDate) : null,
    }),
    [keyword, status, invoiceType, fromDate, toDate]
  );

  const fetchBills = useCallback(() => {
    setLoading(true);
    listBills({
      ...currentFilters(),
      pagination: { current: page, pageSize: PAGE_SIZE, totalPage: 0 },
    })
      .then((res) => {
        const result = res?.data;
        if (result?.success && result.data) {
          setBills(result.data.list || []);
          setTotal(result.data.total || 0);
          setTotalPages(result.data.pagination?.totalPage || 0);
        } else {
          emitApiError(result?.errorMessage || "Không tải được danh sách hoá đơn.");
        }
      })
      .catch(() => { })
      .finally(() => setLoading(false));
  }, [currentFilters, page]);

  const handleExportExcel = () => {
    setExporting(true);
    exportBillsExcel(currentFilters())
      .then((res) => {
        if (res?.data) downloadExport(res, "hoa-don.xlsx");
      })
      .catch(() => { })
      .finally(() => setExporting(false));
  };

  const handleDownloadPdf = (billId) => {
    downloadBillPdf(billId)
      .then((res) => {
        if (res?.data) downloadExport(res, `hoa-don-${billId}.pdf`);
      })
      .catch(() => { });
  };

  const handleIssueEInvoice = (billId) => {
    setEinvoiceBusy(true);
    issueEInvoice(billId)
      .then((res) => {
        const result = res?.data;
        if (result?.success) {
          setDetailBill(result.data);
          fetchBills();
        } else {
          emitApiError(result?.errorMessage || "Phát hành HĐĐT thất bại.");
        }
      })
      .catch(() => { })
      .finally(() => setEinvoiceBusy(false));
  };

  const handleRefreshEInvoice = (billId) => {
    setEinvoiceBusy(true);
    refreshEInvoiceStatus(billId)
      .then((res) => {
        const result = res?.data;
        if (result?.success) {
          setDetailBill(result.data);
          fetchBills();
        } else {
          emitApiError(result?.errorMessage || "Cập nhật trạng thái HĐĐT thất bại.");
        }
      })
      .catch(() => { })
      .finally(() => setEinvoiceBusy(false));
  };

  const handleDownloadEInvoice = (billId) => {
    downloadEInvoicePdf(billId)
      .then((res) => {
        if (res?.data) downloadExport(res, `hddt-${billId}.pdf`);
      })
      .catch(() => { });
  };

  // Archived provider XML — the retention copy required by Điều 11.
  const handleDownloadXml = (bill) => {
    downloadEInvoiceXml(bill.billId)
      .then((res) => {
        if (res?.data) downloadExport(res, `hddt-${bill.billNo || bill.billId}.xml`);
      })
      .catch(() => { });
  };

  const handleSendMail = (billId) => {
    setEinvoiceBusy(true);
    sendEInvoiceMail(billId)
      .then((res) => {
        const result = res?.data;
        if (result?.success) {
          if (result.data) setDetailBill(result.data);
          fetchBills();
        } else {
          emitApiError(result?.errorMessage || "Gửi email HĐĐT thất bại.");
        }
      })
      .catch(() => { })
      .finally(() => setEinvoiceBusy(false));
  };

  const openTrace = (billId) => {
    setTraceTarget(billId);
    setTraceLogs([]);
    setTraceLoading(true);
    getBillLogs(billId)
      .then((res) => {
        const result = res?.data;
        if (result?.success) setTraceLogs(result.data || []);
        else emitApiError(result?.errorMessage || "Không tải được lịch sử hoá đơn.");
      })
      .catch(() => { })
      .finally(() => setTraceLoading(false));
  };

  // A correction produces a brand-new bill; surface it right away.
  const handleCorrectionDone = (newBill) => {
    setDetailBill(null);
    fetchBills();
    if (newBill?.billId) setDetailBill(newBill);
  };

  useEffect(() => {
    fetchBills();
  }, [fetchBills]);

  const applyFilters = () => {
    if (page === 1) fetchBills();
    else setPage(1);
  };

  const openDetail = (billId) => {
    getBill(billId)
      .then((res) => {
        const result = res?.data;
        if (result?.success && result.data) setDetailBill(result.data);
        else emitApiError(result?.errorMessage || "Không tải được hoá đơn.");
      })
      .catch(() => { });
  };

  const confirmVoid = () => {
    if (!voidReason.trim()) {
      emitApiError("Vui lòng nhập lý do huỷ hoá đơn.");
      return;
    }
    voidBill(voidTarget.billId, voidReason.trim())
      .then((res) => {
        const result = res?.data;
        if (result?.success) {
          setVoidTarget(null);
          setVoidReason("");
          if (detailBill?.billId === voidTarget.billId) setDetailBill(result.data);
          fetchBills();
        } else {
          emitApiError(result?.errorMessage || "Huỷ hoá đơn không thành công.");
        }
      })
      .catch(() => { });
  };

  // Tax report tab (G-6) — a standalone panel, so the bill list keeps its state.
  if (tab === 1 && canVoid) {
    return (
      <Box sx={{ maxWidth: 1200, mx: "auto", p: { xs: 1, sm: 2 } }}>
        <PageHeader
          tab={tab}
          onTabChange={(_, value) => setTab(value)}
          showTax={canVoid}
        />
        <TaxReportPanel />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1200, mx: "auto", p: { xs: 1, sm: 2 } }}>
      <PageHeader
        tab={tab}
        onTabChange={(_, value) => setTab(value)}
        showTax={canVoid}
      />

      {/* Filters */}
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={1.5}
        sx={{ mb: 2, flexWrap: "wrap" }}
        useFlexGap
      >
        <TextField
          size="small"
          label="Tìm kiếm"
          placeholder="Số HĐ, khách hàng, MST…"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && applyFilters()}
          sx={{ minWidth: 220 }}
        />
        <DateInput
          size="small"
          label="Từ ngày"
          value={fromDate}
          onChange={setFromDate}
        />
        <DateInput
          size="small"
          label="Đến ngày"
          value={toDate}
          onChange={setToDate}
        />
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Trạng thái</InputLabel>
          <Select
            label="Trạng thái"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <MenuItem value="">Tất cả</MenuItem>
            <MenuItem value="ISSUED">Đã phát hành</MenuItem>
            <MenuItem value="VOIDED">Đã huỷ</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Loại</InputLabel>
          <Select
            label="Loại"
            value={invoiceType}
            onChange={(e) => setInvoiceType(e.target.value)}
          >
            <MenuItem value="">Tất cả</MenuItem>
            <MenuItem value="CHECKOUT">Thanh toán</MenuItem>
            <MenuItem value="DEBT_SETTLEMENT">Thu nợ</MenuItem>
            <MenuItem value="ADJUSTMENT">Điều chỉnh</MenuItem>
            <MenuItem value="REPLACEMENT">Thay thế</MenuItem>
          </Select>
        </FormControl>
        <Button
          variant="contained"
          startIcon={<SearchIcon />}
          onClick={applyFilters}
        >
          Tìm
        </Button>
        <Button
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportExcel}
          disabled={exporting}
        >
          Xuất Excel
        </Button>
      </Stack>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Số HĐ</TableCell>
              <TableCell>Thời gian</TableCell>
              <TableCell>Khách hàng</TableCell>
              <TableCell>Loại</TableCell>
              <TableCell>Thanh toán</TableCell>
              <TableCell align="right">Tổng ({VN_CURRENCY})</TableCell>
              <TableCell align="right">Thu ({VN_CURRENCY})</TableCell>
              <TableCell>Trạng thái</TableCell>
              <TableCell>HĐĐT</TableCell>
              <TableCell align="right">Thao tác</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={28} />
                </TableCell>
              </TableRow>
            ) : bills.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Không có hoá đơn</Typography>
                </TableCell>
              </TableRow>
            ) : (
              bills.map((bill) => (
                <TableRow key={bill.billId} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                      {bill.billNo}
                    </Typography>
                  </TableCell>
                  <TableCell>{formatInstant(bill.issuedAt)}</TableCell>
                  <TableCell>{bill.buyerName || bill.playerName || "—"}</TableCell>
                  <TableCell>{TYPE_LABEL[bill.invoiceType] || bill.invoiceType}</TableCell>
                  <TableCell>
                    {bill.payType === "TRANSFER" ? "Chuyển khoản" : "Tiền mặt"}
                  </TableCell>
                  <TableCell align="right">{formatVND(bill.total)}</TableCell>
                  <TableCell align="right">{formatVND(bill.collectAmount)}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={STATUS_COLOR[bill.status] || "default"}
                      label={STATUS_LABEL[bill.status] || bill.status}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {EINVOICE_LABEL[bill.einvoiceStatus] || bill.einvoiceStatus}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Chi tiết">
                      <IconButton size="small" onClick={() => openDetail(bill.billId)}>
                        <VisibilityOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="In hoá đơn">
                      <IconButton size="small" onClick={() => setPrintBillId(bill.billId)}>
                        <PrintOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Tải PDF">
                      <IconButton size="small" onClick={() => handleDownloadPdf(bill.billId)}>
                        <PictureAsPdfIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {canVoid && bill.status === "ISSUED" && (
                      <>
                        <Tooltip title="Lập hoá đơn điều chỉnh">
                          <IconButton
                            size="small"
                            onClick={() => setCorrection({ bill, mode: "ADJUST" })}
                          >
                            <EditNoteOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Lập hoá đơn thay thế">
                          <IconButton
                            size="small"
                            onClick={() => setCorrection({ bill, mode: "REPLACE" })}
                          >
                            <SwapHorizOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Huỷ hoá đơn">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => setVoidTarget(bill)}
                          >
                            <BlockOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Tổng {total} hoá đơn
        </Typography>
        {totalPages > 1 && (
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, p) => setPage(p)}
            color="primary"
          />
        )}
      </Stack>

      {/* Bill detail dialog */}
      <Dialog
        open={detailBill != null}
        onClose={() => setDetailBill(null)}
        maxWidth="sm"
        fullWidth
        PaperComponent={DraggableResizablePaper}
      >
        {detailBill && (
          <>
            <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
              Hoá đơn {detailBill.billNo}
              <Chip
                size="small"
                sx={{ ml: 1 }}
                color={STATUS_COLOR[detailBill.status] || "default"}
                label={STATUS_LABEL[detailBill.status] || detailBill.status}
              />
            </DialogTitle>
            <DialogContent>
              <Stack spacing={0.5} sx={{ mb: 1 }}>
                <Typography variant="body2">
                  Thời gian: {formatInstant(detailBill.issuedAt)} — Thu ngân:{" "}
                  {detailBill.issuedBy || "—"}
                </Typography>
                <Typography variant="body2">
                  Khách hàng: {detailBill.buyerName || detailBill.playerName || "—"}
                </Typography>
                {detailBill.buyerCompany && (
                  <Typography variant="body2">
                    Đơn vị: {detailBill.buyerCompany}
                    {detailBill.buyerTaxCode ? ` — MST: ${detailBill.buyerTaxCode}` : ""}
                  </Typography>
                )}
                <Typography variant="body2">
                  Thanh toán: {detailBill.payType === "TRANSFER" ? "Chuyển khoản" : "Tiền mặt"}
                </Typography>
                {detailBill.status === "VOIDED" && (
                  <Typography variant="body2" color="error">
                    Đã huỷ bởi {detailBill.voidedBy} lúc {formatInstant(detailBill.voidedAt)}:{" "}
                    {detailBill.voidReason}
                  </Typography>
                )}
                {detailBill.relatedBillNo && (
                  <Typography variant="body2">
                    Hoá đơn gốc: {detailBill.relatedBillNo}
                    {detailBill.correctionType
                      ? ` — ${CORRECTION_LABEL[detailBill.correctionType] ||
                      detailBill.correctionType}`
                      : ""}
                  </Typography>
                )}
                {detailBill.adjustReason && (
                  <Typography variant="body2">Lý do: {detailBill.adjustReason}</Typography>
                )}
                {detailBill.corrected && (
                  <Typography variant="body2" color="warning.main">
                    Đã có hoá đơn điều chỉnh/thay thế cho hoá đơn này.
                  </Typography>
                )}
                {detailBill.einvoiceNo && (
                  <Typography variant="body2">HĐĐT: {detailBill.einvoiceNo}</Typography>
                )}
                {detailBill.einvoiceStatus === "FAILED" && detailBill.einvoiceError && (
                  <Typography variant="body2" color="error">
                    Lỗi HĐĐT: {detailBill.einvoiceError}
                  </Typography>
                )}
              </Stack>
              <Divider sx={{ my: 1 }} />
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Nội dung</TableCell>
                    <TableCell align="right">SL</TableCell>
                    <TableCell align="right">Đơn giá</TableCell>
                    <TableCell align="right">Thành tiền</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(detailBill.items || []).map((item) => (
                    <TableRow key={item.lineNo}>
                      <TableCell>{item.itemName}</TableCell>
                      <TableCell align="right">{item.qty}</TableCell>
                      <TableCell align="right">{formatVND(item.unitPrice)}</TableCell>
                      <TableCell align="right">{formatVND(item.amount)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Divider sx={{ my: 1 }} />
              <Stack spacing={0.5} alignItems="flex-end">
                {detailBill.discountAmount > 0 && (
                  <Typography variant="body2" color="success.main">
                    Giảm giá: −{formatVND(detailBill.discountAmount)} {VN_CURRENCY}
                  </Typography>
                )}
                <Typography variant="body2">
                  Tổng cộng: <strong>{formatVND(detailBill.total)} {VN_CURRENCY}</strong>
                </Typography>
                {detailBill.vatRate > 0 && (
                  <>
                    <Typography variant="body2">
                      Trong đó VAT ({detailBill.vatRate}%): {formatVND(detailBill.vatAmount)} {VN_CURRENCY}
                    </Typography>
                    <Typography variant="body2">
                      Giá chưa VAT: {formatVND(detailBill.subtotal)} {VN_CURRENCY}
                    </Typography>
                  </>
                )}
                <Typography variant="body2">
                  Thực thu: <strong>{formatVND(detailBill.collectAmount)} {VN_CURRENCY}</strong>
                </Typography>
              </Stack>
            </DialogContent>
            <DialogActions sx={{ flexWrap: "wrap", gap: 0.5 }}>
              {einvoiceEnabled && detailBill.status === "ISSUED" && (
                <>
                  {canVoid && ["NONE", "FAILED"].includes(detailBill.einvoiceStatus) && (
                    <Button
                      color="secondary"
                      disabled={einvoiceBusy}
                      onClick={() => handleIssueEInvoice(detailBill.billId)}
                    >
                      {detailBill.einvoiceStatus === "FAILED" ? "Thử lại HĐĐT" : "Phát hành HĐĐT"}
                    </Button>
                  )}
                  {["PENDING", "ISSUED"].includes(detailBill.einvoiceStatus) && (
                    <Button
                      disabled={einvoiceBusy}
                      onClick={() => handleRefreshEInvoice(detailBill.billId)}
                    >
                      Cập nhật HĐĐT
                    </Button>
                  )}
                  {detailBill.einvoiceStatus === "ISSUED" && detailBill.einvoiceNo && (
                    <Button onClick={() => handleDownloadEInvoice(detailBill.billId)}>
                      Tải HĐĐT
                    </Button>
                  )}
                  {detailBill.einvoiceStatus === "ISSUED" && (
                    <>
                      {detailBill.einvoiceXmlAvailable && (
                        <Button
                          startIcon={<CodeOutlinedIcon />}
                          onClick={() => handleDownloadXml(detailBill)}
                        >
                          Tải XML
                        </Button>
                      )}
                      {detailBill.buyerEmail && (
                        <Button
                          startIcon={<MailOutlineOutlinedIcon />}
                          disabled={einvoiceBusy}
                          onClick={() => handleSendMail(detailBill.billId)}
                        >
                          Gửi email HĐĐT
                        </Button>
                      )}
                    </>
                  )}
                </>
              )}
              {canVoid && detailBill.status === "ISSUED" && (
                <>
                  <Button
                    startIcon={<EditNoteOutlinedIcon />}
                    onClick={() => setCorrection({ bill: detailBill, mode: "ADJUST" })}
                  >
                    Điều chỉnh
                  </Button>
                  <Button
                    startIcon={<SwapHorizOutlinedIcon />}
                    onClick={() => setCorrection({ bill: detailBill, mode: "REPLACE" })}
                  >
                    Thay thế
                  </Button>
                </>
              )}
              {canVoid && detailBill.status === "ISSUED" && (
                <Button
                  color="error"
                  startIcon={<BlockOutlinedIcon />}
                  onClick={() => setVoidTarget(detailBill)}
                >
                  Huỷ HĐ
                </Button>
              )}
              {canVoid && (
                <Button
                  startIcon={<HistoryOutlinedIcon />}
                  onClick={() => openTrace(detailBill.billId)}
                >
                  Lịch sử
                </Button>
              )}
              <Button
                startIcon={<PictureAsPdfIcon />}
                onClick={() => handleDownloadPdf(detailBill.billId)}
              >
                PDF
              </Button>
              <Button
                startIcon={<PrintOutlinedIcon />}
                onClick={() => setPrintBillId(detailBill.billId)}
              >
                In
              </Button>
              <Button variant="outlined" onClick={() => setDetailBill(null)}>
                Đóng
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Void reason dialog */}
      <Dialog
        open={voidTarget != null}
        onClose={() => setVoidTarget(null)}
        maxWidth="sm"
        fullWidth
        PaperComponent={DraggableResizablePaper}
      >
        <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
          Huỷ hoá đơn {voidTarget?.billNo}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Hoá đơn đã huỷ vẫn được lưu lại để đối chiếu. Vui lòng nhập lý do.
          </Typography>
          <TextField
            autoFocus
            fullWidth
            size="small"
            label="Lý do huỷ"
            value={voidReason}
            onChange={(e) => setVoidReason(e.target.value)}
            multiline
            minRows={2}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVoidTarget(null)}>Đóng</Button>
          <Button variant="contained" color="error" onClick={confirmVoid}>
            Xác nhận huỷ
          </Button>
        </DialogActions>
      </Dialog>

      <CorrectionDialog
        show={correction != null}
        bill={correction?.bill}
        mode={correction?.mode}
        onClose={() => setCorrection(null)}
        onDone={handleCorrectionDone}
      />

      {/* Internal-control trail (G-5) */}
      <Dialog
        open={traceTarget != null}
        onClose={() => setTraceTarget(null)}
        maxWidth="sm"
        fullWidth
        PaperComponent={DraggableResizablePaper}
      >
        <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
          Lịch sử hoá đơn
        </DialogTitle>
        <DialogContent dividers>
          {traceLoading ? (
            <Box sx={{ textAlign: "center", py: 4 }}>
              <CircularProgress size={28} />
            </Box>
          ) : traceLogs.length === 0 ? (
            <Typography color="text.secondary">Chưa có ghi nhận nào.</Typography>
          ) : (
            <Stack spacing={1.5}>
              {traceLogs.map((log) => (
                <Box key={log.logId}>
                  <Stack
                    direction="row"
                    spacing={1}
                    alignItems="center"
                    sx={{ flexWrap: "wrap" }}
                    useFlexGap
                  >
                    <Chip
                      size="small"
                      label={LOG_ACTION_LABEL[log.action] || log.action}
                    />
                    <Typography variant="caption" color="text.secondary">
                      {log.createdAt} · {log.actor || "—"}
                      {log.ip ? ` · ${log.ip}` : ""}
                    </Typography>
                  </Stack>
                  {log.detail && <Typography variant="body2">{log.detail}</Typography>}
                  {log.error && (
                    <Typography variant="body2" color="error">
                      Lỗi: {log.error}
                    </Typography>
                  )}
                </Box>
              ))}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={() => setTraceTarget(null)}>
            Đóng
          </Button>
        </DialogActions>
      </Dialog>

      <ReceiptPrintDialog
        show={printBillId != null}
        billId={printBillId}
        onClose={() => setPrintBillId(null)}
      />
    </Box>
  );
}
