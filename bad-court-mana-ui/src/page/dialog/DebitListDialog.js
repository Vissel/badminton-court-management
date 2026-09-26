import React, { useState, useEffect, useRef } from "react";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import Divider from "@mui/material/Divider";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemText from "@mui/material/ListItemText";
import TextField from "@mui/material/TextField";
import Checkbox from "@mui/material/Checkbox";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import InputAdornment from "@mui/material/InputAdornment";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import Pagination from "@mui/material/Pagination";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import api from "../../api/index";
import { emitApiError } from "../../api/errorBus";
import { listDebitHistory, REMAINING_DEBTS_FILTER } from "../../api/debtApi";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import { formatVNDateTime, parseServerDateTime, toServerDateTimeString } from "../DateTimeUtils";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./DraggableResizablePaper";

const DEFAULT_FILTER = {
  from: "2026-01-01",
  to: "9999-12-31",
  amountFrom: 0,
  amountTo: 0,
};

const DEFAULT_PAGINATION = {
  current: 1,
  pageSize: 50,
  totalPage: 0,
};

const HISTORY_PAGE_SIZE = 10;
// Raw input strings — empty means unbounded
const EMPTY_HISTORY_FILTER = { from: "", to: "", amountFrom: "", amountTo: "" };

const STATUS_META = {
  PAID: { label: "Đã trả", color: "success" },
  PARTIALLY_PAID: { label: "Trả một phần", color: "warning" },
  PENDING: { label: "Chưa trả", color: "default" },
};

const DebitListDialog = ({ show, playerName, onClose, onPaid, preselectedDebits, onAddToPayment, readOnly = false, historyMode = false }) => {
  const isReadOnly = readOnly || historyMode;
  const [debtData, setDebtData] = useState({
    remainingDebits: [],
    debitSummary: null,
  });
  const [payAmount, setPayAmount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH");
  const [note, setNote] = useState("");
  const [selected, setSelected] = useState(new Set());
  const [partialSelected, setPartialSelected] = useState(new Set());
  const [partialAmounts, setPartialAmounts] = useState(new Map());
  const [prePayResponse, setPrePayResponse] = useState(null);
  const [paying, setPaying] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    severity: "success",
    message: "",
    autoHideDuration: null,
  });
  const prePayTimerRef = useRef(null);
  const skipPrePayRef = useRef(false);
  const successCloseTimerRef = useRef(null);

  // ── historyMode: /history browser — server-side filter + pagination ──
  const [histPage, setHistPage] = useState(1);
  const [histData, setHistData] = useState({ list: [], totalPage: 0, total: 0 });
  const [histLoading, setHistLoading] = useState(false);
  const [filterInput, setFilterInput] = useState(EMPTY_HISTORY_FILTER);
  const [appliedFilter, setAppliedFilter] = useState(REMAINING_DEBTS_FILTER);

  const normalizeDateTime = (dateTime) => {
    const parsed = parseServerDateTime(dateTime);
    // Truncate to seconds because the server may return fractional seconds in ISO strings.
    return parsed ? String(Math.floor(parsed.getTime() / 1000)) : dateTime;
  };

  useEffect(() => {
    if (!show || !playerName) {
      setDebtData({ remainingDebits: [], debitSummary: null });
      setPayAmount("");
      setPaymentMethod("CASH");
      setNote("");
      setSelected(new Set());
      setPartialSelected(new Set());
      setPartialAmounts(new Map());
      setPrePayResponse(null);
      setPaying(false);
      setSnackbar((s) => ({ ...s, open: false }));
      setHistPage(1);
      setHistData({ list: [], totalPage: 0, total: 0 });
      setHistLoading(false);
      setFilterInput(EMPTY_HISTORY_FILTER);
      setAppliedFilter(REMAINING_DEBTS_FILTER);
      if (successCloseTimerRef.current) {
        clearTimeout(successCloseTimerRef.current);
        successCloseTimerRef.current = null;
      }
      return;
    }
    if (historyMode) return; // the /history effect below owns this mode's fetch
    // Debts checked on the caller's page are pre-ticked here, matched by
    // dateTime|note|amount — the same identity the prePay mapping uses.
    const applyPreselected = (debts) => {
      if (!preselectedDebits?.length || isReadOnly) return;
      const keyOf = (d) =>
        `${normalizeDateTime(d?.dateTime)}|${d?.note}|${d?.money?.amount}`;
      const queues = new Map();
      debts.forEach((d, idx) => {
        const q = queues.get(keyOf(d)) || [];
        q.push(idx);
        queues.set(keyOf(d), q);
      });
      const sel = new Set();
      preselectedDebits.forEach((d) => {
        const q = queues.get(keyOf(d));
        if (q?.length) sel.add(q.shift());
      });
      if (sel.size === 0) return;
      skipPrePayRef.current = true; // keep the amount-driven prePay from overwriting it
      setSelected(sel);
      setPayAmount(
        String([...sel].reduce((s, i) => s + (debts[i]?.money?.amount || 0), 0))
      );
    };

    api
      .post("/api/v1/debit/listRemainingDebts", {
        playerNames: [playerName],
        pagination: DEFAULT_PAGINATION,
        filter: DEFAULT_FILTER,
      })
      .then((res) => {
        const b = res?.data; // Result<GetRemainingDebtResponse>
        const data = b?.success ? b.data : null;
        if (data) {
          setDebtData(data);
          applyPreselected(data.remainingDebits || []);
        } else {
          setDebtData({ remainingDebits: [], debitSummary: null });
        }
      })
      .catch(() => setDebtData({ remainingDebits: [], debitSummary: null }));
  }, [show, playerName, preselectedDebits, isReadOnly, historyMode]);

  // Draft filter inputs are pushed into the /history request 500ms after
  // typing stops — the backend does the filtering, nothing is filtered here.
  useEffect(() => {
    if (!historyMode || !show) return;
    const t = setTimeout(() => {
      setHistPage(1);
      setAppliedFilter({
        from: filterInput.from || REMAINING_DEBTS_FILTER.from,
        to: filterInput.to || REMAINING_DEBTS_FILTER.to,
        amountFrom: Number(filterInput.amountFrom) || 0,
        amountTo: Number(filterInput.amountTo) || 0,
      });
    }, 500);
    return () => clearTimeout(t);
  }, [filterInput, historyMode, show]);

  useEffect(() => {
    if (!historyMode || !show || !playerName) return;
    setHistLoading(true);
    listDebitHistory(
      playerName,
      { current: histPage, pageSize: HISTORY_PAGE_SIZE, totalPage: 0 },
      appliedFilter
    )
      .then((res) => {
        const b = res?.data;
        const data = b?.success ? b.data : null;
        setHistData({
          list: data?.list || [],
          totalPage: data?.pagination?.totalPage || 0,
          total: data?.total || 0,
        });
      })
      .catch(() => setHistData({ list: [], totalPage: 0, total: 0 }))
      .finally(() => setHistLoading(false));
  }, [historyMode, show, playerName, histPage, appliedFilter]);

  const numericPay = payAmount ? Number(payAmount) : 0;
  const { remainingDebits = [], debitSummary } = debtData;
  const totalDebts = debitSummary?.totalDebts?.amount || 0;
  const numberDebit = debitSummary?.numberDebit || 0;
  const appliedPayTotal = prePayResponse?.prepayDebits
    ? prePayResponse.prepayDebits.reduce(
      (sum, item) => sum + (item?.payDebit?.money?.amount || 0),
      0
    )
    : numericPay;

  useEffect(() => {
    if (prePayTimerRef.current) {
      clearTimeout(prePayTimerRef.current);
    }
    if (skipPrePayRef.current) {
      skipPrePayRef.current = false;
      setPrePayResponse(null);
      return;
    }
    if (isReadOnly) {
      setPrePayResponse(null);
      return;
    }
    if (!numericPay || numericPay <= 0) {
      setPrePayResponse(null);
      setSelected(new Set());
      setPartialSelected(new Set());
      setPartialAmounts(new Map());
      return;
    }
    prePayTimerRef.current = setTimeout(() => {
      api
        .post("/api/v1/debit/prePay", {
          playerName,
          totalPayAmount: numericPay,
          paymentMethod: paymentMethod,
          note: note,
        })
        .then((res) => {
          const b = res?.data; // Result<PrepayDebitResponse>
          setPrePayResponse(b?.success ? b.data : null);
        })
        .catch(() => setPrePayResponse(null));
    }, 500);
    return () => {
      if (prePayTimerRef.current) {
        clearTimeout(prePayTimerRef.current);
      }
    };
  }, [numericPay, playerName, paymentMethod, note, isReadOnly]);

  const handlePayAmountChange = (e) => {
    const val = e.target.value;
    if (val === "" || /^\d+$/.test(val)) {
      skipPrePayRef.current = false;
      setPayAmount(val);
    }
  };

  const toggleSelected = (idx) => {
    const nextSelected = new Set(selected);
    if (nextSelected.has(idx)) {
      nextSelected.delete(idx);
    } else {
      nextSelected.add(idx);
    }
    setSelected(nextSelected);
    setPartialSelected(new Set());
    setPartialAmounts(new Map());

    const total = [...nextSelected].reduce(
      (sum, i) => sum + (remainingDebits[i]?.money?.amount || 0),
      0
    );
    skipPrePayRef.current = true;
    setPayAmount(total > 0 ? String(total) : "");
  };

  const buildListDebitPay = () => {
    const listDebitPay = [];
    selected.forEach((idx) => {
      const debt = remainingDebits[idx];
      const amount = debt?.money?.amount || 0;
      if (debt?.dateTime && amount > 0) {
        listDebitPay.push({
          dateTime: toServerDateTimeString(debt.dateTime),
          payAmount: amount,
        });
      }
    });
    partialSelected.forEach((idx) => {
      const debt = remainingDebits[idx];
      const amount = partialAmounts.get(idx) || 0;
      if (debt?.dateTime && amount > 0) {
        listDebitPay.push({
          dateTime: toServerDateTimeString(debt.dateTime),
          payAmount: amount,
        });
      }
    });
    return listDebitPay;
  };

  // "Add to payment" mode: hand the selection to the caller (PayConfirm) as a
  // PayDebitRequest-shaped object — it is settled later by /pay/payToPlayer.
  const handleAddToPayment = () => {
    const listDebitPay = buildListDebitPay();
    if (listDebitPay.length === 0) return;
    onAddToPayment?.({
      playerName,
      totalPayAmount: listDebitPay.reduce((sum, item) => sum + item.payAmount, 0),
      paymentMethod,
      note,
      listDebitPay,
    });
    onClose();
  };

  const handlePay = () => {
    if (paying) return;
    const listDebitPay = buildListDebitPay();
    if (listDebitPay.length === 0) return;
    if (prePayTimerRef.current) {
      clearTimeout(prePayTimerRef.current);
      prePayTimerRef.current = null;
    }
    const totalPayAmount = listDebitPay.reduce((sum, item) => sum + item.payAmount, 0);
    setPaying(true);
    api
      .post("/api/v1/debit/pay", {
        playerName,
        totalPayAmount,
        paymentMethod: paymentMethod,
        note: note,
        listDebitPay,
      })
      .then((res) => {
        const b = res?.data; // Result<PayDebitResponse>
        const data = b?.success ? b.data : null;
        const status = data?.status;
        if (status === "SUCCESS" || status === "PARTIAL") {
          setSnackbar({
            open: true,
            severity: "success",
            message: "Thanh toán thành công",
            autoHideDuration: 2000,
          });
          successCloseTimerRef.current = setTimeout(() => {
            onPaid?.();
            onClose();
          }, 2000);
        } else {
          emitApiError(data?.message || b?.errorMessage || "Thanh toán thất bại");
        }
      })
      .catch(() => {
        // The api interceptor already shows the standard error popup
      })
      .finally(() => setPaying(false));
  };

  useEffect(() => {
    if (!prePayResponse?.prepayDebits || remainingDebits.length === 0) {
      return;
    }
    const fullQueueMap = new Map();
    const partialQueueMap = new Map();
    const enqueue = (map, key, value) => {
      const q = map.get(key) || [];
      q.push(value);
      map.set(key, q);
    };
    const dequeue = (map, key) => {
      const q = map.get(key);
      return q && q.length ? q.shift() : undefined;
    };

    prePayResponse.prepayDebits.forEach((item) => {
      const debit = item?.payDebit;
      const baseKey = `${normalizeDateTime(debit?.dateTime)}|${debit?.note}`;
      if (item?.payStatus === "FULL_PAY") {
        enqueue(fullQueueMap, `${baseKey}|${debit?.money?.amount}`, true);
      } else if (item?.payStatus === "PARTIALLY_PAY") {
        enqueue(partialQueueMap, baseKey, debit?.money?.amount || 0);
      }
    });

    const full = new Set();
    const partial = new Set();
    const newPartialAmounts = new Map();
    remainingDebits.forEach((debt, idx) => {
      const baseKey = `${normalizeDateTime(debt.dateTime)}|${debt.note}`;
      if (dequeue(fullQueueMap, `${baseKey}|${debt.money?.amount}`)) {
        full.add(idx);
      } else {
        const appliedAmount = dequeue(partialQueueMap, baseKey);
        if (appliedAmount !== undefined) {
          partial.add(idx);
          newPartialAmounts.set(idx, appliedAmount);
        }
      }
    });
    setSelected(full);
    setPartialSelected(partial);
    setPartialAmounts(newPartialAmounts);
  }, [prePayResponse, remainingDebits]);

  return (
    <>
      <Dialog
        open={show}
        onClose={(event, reason) => {
          if (reason === "backdropClick") return;
          onClose();
        }}
        maxWidth={historyMode ? "sm" : "xs"}
        fullWidth
        PaperComponent={DraggableResizablePaper}
        PaperProps={{ sx: { borderRadius: 3, overflow: "hidden" } }}
      >
        <Box
          className={DIALOG_DRAG_HANDLE}
          sx={{
            cursor: "move",
            bgcolor: "warning.light",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            py: 2.5,
            px: 2,
          }}
        >
          <Box
            sx={{
              bgcolor: "warning.main",
              borderRadius: "50%",
              width: 64,
              height: 64,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              mb: 1,
              boxShadow: "0 4px 14px 0 rgba(0,0,0,0.15)",
            }}
          >
            <WarningAmberIcon sx={{ fontSize: 40, color: "#fff" }} />
          </Box>
          <Typography variant="h6" fontWeight={700} color="#fff">
            {historyMode ? "Lịch sử nợ của" : "Nợ của"} <strong>{playerName}</strong>
          </Typography>
        </Box>

        <DialogContent sx={{ px: 3, py: 2 }}>
          {historyMode ? (
            <>
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", mb: 1.5 }}>
                <TextField
                  type="date"
                  size="small"
                  label="Từ ngày"
                  value={filterInput.from}
                  onChange={(e) => setFilterInput((f) => ({ ...f, from: e.target.value }))}
                  slotProps={{ inputLabel: { shrink: true } }}
                  sx={{ flex: "1 1 140px" }}
                />
                <TextField
                  type="date"
                  size="small"
                  label="Đến ngày"
                  value={filterInput.to}
                  onChange={(e) => setFilterInput((f) => ({ ...f, to: e.target.value }))}
                  slotProps={{ inputLabel: { shrink: true } }}
                  sx={{ flex: "1 1 140px" }}
                />
                <TextField
                  size="small"
                  label="Nợ từ"
                  value={filterInput.amountFrom}
                  onChange={(e) => {
                    const v = e.target.value;
                    if (v === "" || /^\d+$/.test(v))
                      setFilterInput((f) => ({ ...f, amountFrom: v }));
                  }}
                  slotProps={{
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                      ),
                    },
                  }}
                  sx={{ flex: "1 1 120px" }}
                />
                <TextField
                  size="small"
                  label="Nợ đến"
                  value={filterInput.amountTo}
                  onChange={(e) => {
                    const v = e.target.value;
                    if (v === "" || /^\d+$/.test(v))
                      setFilterInput((f) => ({ ...f, amountTo: v }));
                  }}
                  slotProps={{
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                      ),
                    },
                  }}
                  sx={{ flex: "1 1 120px" }}
                />
              </Box>

              {histLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                  <CircularProgress size={22} />
                </Box>
              ) : histData.list.length === 0 ? (
                <Typography
                  variant="body2"
                  color="text.secondary"
                  textAlign="center"
                  sx={{ py: 2 }}
                >
                  Không có lịch sử nợ nào.
                </Typography>
              ) : (
                <>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Tổng cộng {histData.total} khoản
                  </Typography>
                  <List dense disablePadding>
                    {histData.list.map((it, idx) => {
                      const meta = STATUS_META[it.status] || { label: it.status || "—", color: "default" };
                      return (
                        <ListItem
                          key={idx}
                          disableGutters
                          disablePadding
                          sx={{
                            py: 0.75,
                            borderBottom: 1,
                            borderColor: "divider",
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                          }}
                        >
                          <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{ minWidth: 22, textAlign: "center" }}
                          >
                            {(histPage - 1) * HISTORY_PAGE_SIZE + idx + 1}
                          </Typography>
                          <ListItemText
                            primary={
                              <Box
                                sx={{
                                  display: "flex",
                                  justifyContent: "space-between",
                                  alignItems: "center",
                                }}
                              >
                                <Typography variant="body2">{it.note || "Nợ"}</Typography>
                                <Typography
                                  variant="body2"
                                  fontWeight={600}
                                  sx={{ color: "warning.dark" }}
                                >
                                  {formatVND(it.debtAmount)} {it.currency || VN_CURRENCY}
                                </Typography>
                              </Box>
                            }
                            secondary={
                              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                                <Typography variant="caption" color="text.secondary">
                                  {formatVNDateTime(it.debtDateTime)}
                                </Typography>
                                <Chip
                                  label={meta.label}
                                  color={meta.color}
                                  size="small"
                                  variant="outlined"
                                />
                              </Box>
                            }
                          />
                        </ListItem>
                      );
                    })}
                  </List>
                  {histData.totalPage > 1 && (
                    <Box sx={{ display: "flex", justifyContent: "center", mt: 1 }}>
                      <Pagination
                        size="small"
                        count={histData.totalPage}
                        page={histPage}
                        onChange={(_, p) => setHistPage(p)}
                      />
                    </Box>
                  )}
                </>
              )}
            </>
          ) : remainingDebits.length === 0 ? (
            <Typography
              variant="body2"
              color="text.secondary"
              textAlign="center"
              sx={{ py: 2 }}
            >
              Không có khoản nợ nào.
            </Typography>
          ) : (
            <>
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  mb: 1.5,
                }}
              >
                <Typography variant="body2" color="text.secondary">
                  Tổng nợ ({numberDebit} khoản)
                </Typography>
                <Typography variant="body1" fontWeight={700} color="warning">
                  {formatVND(totalDebts)} {debitSummary?.totalDebts?.currency || VN_CURRENCY}
                </Typography>
              </Box>

              {!isReadOnly && (
                <>
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      mb: 1,
                    }}
                  >
                    <Typography variant="body2" color="text.secondary">
                      Tổng thanh toán
                    </Typography>
                    <Typography variant="body1" fontWeight={700} color="success">
                      {formatVND(appliedPayTotal)} {VN_CURRENCY}
                    </Typography>
                  </Box>

                  <Box sx={{ display: "flex", gap: 1, mb: 1.5 }}>
                    <TextField
                      size="small"
                      label="Số tiền thanh toán"
                      placeholder="0"
                      value={payAmount}
                      onChange={handlePayAmountChange}
                      sx={{ flex: 1 }}
                      slotProps={{
                        input: {
                          endAdornment: (
                            <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                          ),
                        },
                      }}
                    />
                    <Select
                      size="small"
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                      aria-label="Phương thức thanh toán"
                      sx={{ minWidth: 130 }}
                    >
                      <MenuItem value="CASH">Tiền mặt</MenuItem>
                      <MenuItem value="TRANSFER">Chuyển khoản</MenuItem>
                    </Select>
                  </Box>

                  <TextField
                    fullWidth
                    size="small"
                    label="Ghi chú"
                    placeholder="Ghi chú thanh toán"
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    sx={{ mb: 1.5 }}
                  />
                </>
              )}

              <Divider sx={{ mb: 1 }} />

              <List dense disablePadding>
                {remainingDebits.map((debt, idx) => (
                  <ListItem
                    key={idx}
                    disableGutters
                    disablePadding
                    sx={{
                      py: 0.75,
                      borderBottom: 1,
                      borderColor: "divider",
                      display: "flex",
                      alignItems: "center",
                      gap: 1,
                    }}
                  >
                    {!isReadOnly && (
                      <Checkbox
                        checked={selected.has(idx)}
                        indeterminate={partialSelected.has(idx)}
                        onChange={() => toggleSelected(idx)}
                        size="small"
                        sx={{ p: 0.5 }}
                      />
                    )}
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ minWidth: 22, textAlign: "center" }}
                    >
                      {idx + 1}
                    </Typography>
                    <ListItemText
                      primary={
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                          }}
                        >
                          <Typography variant="body2">{debt.note || "Nợ"}</Typography>
                          <Typography
                            variant="body2"
                            fontWeight={600}
                            sx={{ color: "warning.dark" }}
                          >
                            {formatVND(debt.money?.amount)} {debt.money?.currency || VN_CURRENCY}
                          </Typography>
                        </Box>
                      }
                      secondary={
                        <Typography variant="caption" color="text.secondary">
                          {formatVNDateTime(debt.dateTime)}
                        </Typography>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2.5, pt: 1.5, gap: 1.5, display: "flex" }}>
          <Button
            variant="outlined"
            color="inherit"
            onClick={onClose}
            sx={{ flex: 1, borderRadius: 2, py: 1.2, fontWeight: 700 }}
          >
            Đóng
          </Button>
          {!isReadOnly && (
            <Button
              variant="contained"
              color="warning"
              onClick={onAddToPayment ? handleAddToPayment : handlePay}
              disabled={paying || (selected.size === 0 && partialSelected.size === 0)}
              sx={{ flex: 1, borderRadius: 2, py: 1.2, fontWeight: 700 }}
              disableElevation
            >
              {onAddToPayment ? "Thêm vào thanh toán" : "Thanh toán"}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={snackbar.autoHideDuration}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
        onClose={(_, reason) => {
          if (reason === "clickaway") return;
          setSnackbar((s) => ({ ...s, open: false }));
        }}
      >
        <Alert
          severity={snackbar.severity}
          variant="filled"
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          sx={{ width: "100%" }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default DebitListDialog;
