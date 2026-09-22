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
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import api from "../../api/index";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import { formatVNDateTime, parseServerDateTime, toServerDateTimeString } from "../DateTimeUtils";

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

const DebitListDialog = ({ show, playerName, onClose, onPaid }) => {
  const [debtData, setDebtData] = useState({
    remainingDebits: [],
    debitSummary: null,
  });
  const [payAmount, setPayAmount] = useState("");
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

  const normalizeDateTime = (dateTime) => {
    const parsed = parseServerDateTime(dateTime);
    // Truncate to seconds because the server may return fractional seconds in ISO strings.
    return parsed ? String(Math.floor(parsed.getTime() / 1000)) : dateTime;
  };

  useEffect(() => {
    if (!show || !playerName) {
      setDebtData({ remainingDebits: [], debitSummary: null });
      setPayAmount("");
      setSelected(new Set());
      setPartialSelected(new Set());
      setPartialAmounts(new Map());
      setPrePayResponse(null);
      setPaying(false);
      setSnackbar((s) => ({ ...s, open: false }));
      if (successCloseTimerRef.current) {
        clearTimeout(successCloseTimerRef.current);
        successCloseTimerRef.current = null;
      }
      return;
    }
    api
      .post("/api/v1/debit/listRemainingDebts", {
        playerNames: [playerName],
        pagination: DEFAULT_PAGINATION,
        filter: DEFAULT_FILTER,
      })
      .then((res) => {
        if (res?.data) setDebtData(res.data);
      })
      .catch(() => setDebtData({ remainingDebits: [], debitSummary: null }));
  }, [show, playerName]);

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
          paymentMethod: "CASH",
          note: "",
        })
        .then((res) => {
          if (res?.data) setPrePayResponse(res.data);
          else setPrePayResponse(null);
        })
        .catch(() => setPrePayResponse(null));
    }, 500);
    return () => {
      if (prePayTimerRef.current) {
        clearTimeout(prePayTimerRef.current);
      }
    };
  }, [numericPay, playerName]);

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

  const handlePay = () => {
    if (paying) return;
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
        paymentMethod: "CASH",
        note: "",
        listDebitPay,
      })
      .then((res) => {
        const data = res?.data;
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
          setSnackbar({
            open: true,
            severity: "error",
            message: data?.message || "Thanh toán thất bại",
            autoHideDuration: null,
          });
        }
      })
      .catch(() => {
        setSnackbar({
          open: true,
          severity: "error",
          message: "Thanh toán thất bại. Vui lòng thử lại.",
          autoHideDuration: null,
        });
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
        maxWidth="xs"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3, overflow: "hidden" } }}
      >
        <Box
          sx={{
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
            Nợ của <strong>{playerName}</strong>
          </Typography>
        </Box>

        <DialogContent sx={{ px: 3, py: 2 }}>
          {remainingDebits.length === 0 ? (
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
                <Typography variant="body1" fontWeight={700} color="warning.main">
                  {formatVND(totalDebts)} {debitSummary?.totalDebts?.currency || VN_CURRENCY}
                </Typography>
              </Box>

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
                <Typography variant="body1" fontWeight={700} color="success.main">
                  {formatVND(appliedPayTotal)} {VN_CURRENCY}
                </Typography>
              </Box>

              <TextField
                fullWidth
                size="small"
                label="Số tiền thanh toán"
                placeholder="0"
                value={payAmount}
                onChange={handlePayAmountChange}
                sx={{ mb: 1.5 }}
              />

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
                    <Checkbox
                      checked={selected.has(idx)}
                      indeterminate={partialSelected.has(idx)}
                      onChange={() => toggleSelected(idx)}
                      size="small"
                      sx={{ p: 0.5 }}
                    />
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
                            color="warning.dark"
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
          <Button
            variant="contained"
            color="warning"
            onClick={handlePay}
            disabled={paying || (selected.size === 0 && partialSelected.size === 0)}
            sx={{ flex: 1, borderRadius: 2, py: 1.2, fontWeight: 700 }}
            disableElevation
          >
            Thanh toán
          </Button>
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
