import React, { useState, useEffect, useCallback } from "react";
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
import IconButton from "@mui/material/IconButton";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CloseIcon from "@mui/icons-material/Close";
import api from "../../api/index";
import { TYPE, ADVANCE_SERVICE_NAME } from "../HomePage";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import DebitListDialog from "./DebitListDialog";

const PayConfirm = ({ show, data, onConfirm, onExit }) => {
  const [debitSummary, setDebitSummary] = useState(null);
  const [showDebitDialog, setShowDebitDialog] = useState(false);
  const [debtInput, setDebtInput] = useState(null);
  const [debtList, setDebtList] = useState([]);
  const [showDebtInput, setShowDebtInput] = useState(false);

  useEffect(() => {
    if (show) {
      setDebtInput(null);
      setDebtList([]);
      setShowDebtInput(false);
    }
  }, [show]);

  const fetchDebitSummary = useCallback(() => {
    api
      .get(`/api/v1/debit/summary?playerName=${encodeURIComponent(data?.playerName)}`)
      .then((res) => {
        if (res?.data) setDebitSummary(res.data);
      })
      .catch(() => setDebitSummary(null));
  }, [data?.playerName]);

  useEffect(() => {
    if (!show || !data?.playerName) {
      setDebitSummary(null);
      return;
    }
    fetchDebitSummary();
  }, [show, data?.playerName, fetchDebitSummary]);

  if (!show || !data) return null;

  const isPayment = data?.type === TYPE.PAY;
  const headerColor = isPayment ? "success.main" : "error.main";
  const headerBg = isPayment ? "success.light" : "error.light";
  const headerIcon = isPayment ? (
    <CheckCircleIcon sx={{ fontSize: 48, color: "#fff" }} />
  ) : (
    <CancelOutlinedIcon sx={{ fontSize: 48, color: "#fff" }} />
  );
  const actionLabel = isPayment ? "Xác nhận thanh toán" : "Xác nhận huỷ";

  const allServices = data.services || [];
  // Separate advance (Trả trước) from regular services
  const advanceItem = allServices.find(
    (s) => s.serviceName === ADVANCE_SERVICE_NAME
  );
  const regularServices = allServices.filter(
    (s) => s.serviceName !== ADVANCE_SERVICE_NAME
  );
  const advanceAmount = advanceItem ? Math.abs(advanceItem.cost || 0) : 0;

  // Calculate total of regular services
  const regularTotal = regularServices.reduce((sum, item) => sum + (item.cost || 0), 0);

  // Calculate remaining amount to pay (can be negative if advance > total)
  const remainingAmount = regularTotal - advanceAmount;

  // Amount payable before recording any debt
  const payableAmount = remainingAmount > 0 ? remainingAmount : 0;

  const totalRecordedDebt = debtList.reduce((sum, d) => sum + d, 0);
  const remainingPayable = Math.max(payableAmount - totalRecordedDebt, 0);

  const debtInputVal = debtInput === null ? String(remainingPayable) : debtInput;
  const debtNum = /^\d+$/.test(debtInputVal) ? Number(debtInputVal) : 0;
  const canRecordDebt = debtNum > 0 && debtNum <= remainingPayable;

  // A debt being typed previews below the total until committed or cancelled
  const pendingDebt = showDebtInput && debtInput !== null && canRecordDebt ? debtNum : 0;
  const totalDebitAmount = totalRecordedDebt + pendingDebt;

  // Amount to pay now (reduced by recorded and pending debts)
  const amountToPay = Math.max(remainingPayable - pendingDebt, 0);

  const handleDebtInputChange = (e) => {
    const val = e.target.value;
    if (val === "" || /^\d+$/.test(val)) {
      setDebtInput(val);
    }
  };

  const handleRecordDebt = () => {
    if (canRecordDebt) {
      setDebtList([...debtList, debtNum]);
      setDebtInput(null);
      setShowDebtInput(false);
    }
  };

  const handleCloseDebtInput = () => {
    setDebtInput(null);
    setShowDebtInput(false);
  };

  const removeDebtAt = (idx) => {
    setDebtList(debtList.filter((_, i) => i !== idx));
  };

  // Total cost (this is what the player has to pay in total)
  const totalCost = regularTotal;

  // Amount to return to customer (if advance > total)
  const returnAmount = advanceAmount > regularTotal ? advanceAmount - regularTotal : 0;

  return (
    <>
      <Dialog
        open={show}
        onClose={(event, reason) => {
          if (reason === "backdropClick") return;
          onExit();
        }}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: { borderRadius: 3, overflow: "hidden" },
        }}
      >
        {/* ── Colored header banner ── */}
        <Box
          sx={{
            bgcolor: headerBg,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            position: "relative",
            py: 3,
            px: 2,
          }}
        >
          <IconButton
            aria-label="Đóng"
            onClick={onExit}
            sx={{
              position: "absolute",
              top: 8,
              right: 8,
              color: "#fff",
              bgcolor: "rgba(255, 255, 255, 0.25)",
              border: "1px solid rgba(255, 255, 255, 0.4)",
              "&:hover": {
                bgcolor: "rgba(255, 255, 255, 0.4)",
              },
            }}
          >
            <CloseIcon />
          </IconButton>
          <Box
            sx={{
              bgcolor: headerColor,
              borderRadius: "50%",
              width: 72,
              height: 72,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              mb: 1.5,
              boxShadow: "0 4px 14px 0 rgba(0,0,0,0.15)",
            }}
          >
            {headerIcon}
          </Box>
          <Typography variant="h6" fontWeight={700} color="#fff">
            {isPayment ? "Xác nhận thanh toán" : "Xác nhận huỷ dịch vụ"}
          </Typography>
        </Box>

        {/* ── Amount hero ── */}
        <Box sx={{ textAlign: "center", py: 2.5, px: 3 }}>
          <Typography variant="caption" color="text.secondary" sx={{ letterSpacing: 1 }}>
            SỐ TIỀN
          </Typography>
          <Typography
            variant="h3"
            fontWeight={700}
            color={headerColor}
            sx={{ mt: 0.5, lineHeight: 1.2 }}
          >
            {formatVND(amountToPay)}{" "}
            <Typography
              component="span"
              variant="h6"
              color="text.secondary"
              fontWeight={500}
            >
              {VN_CURRENCY}
            </Typography>
          </Typography>
        </Box>

        <Divider />

        {/* ── Details ── */}
        <DialogContent sx={{ px: 3, py: 2 }}>
          {/* Player name */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              mb: 1.5,
              justifyContent: "space-between",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <PersonOutlinedIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary" sx={{ minWidth: 60 }}>
                Người chơi
              </Typography>
              <Typography variant="subtitle1" fontWeight={700} color="primary.main">
                {data.playerName}
              </Typography>
            </Box>

            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              {isPayment && remainingPayable > 0 && !showDebtInput && (
                <Chip
                  label="+ Ghi nợ"
                  size="small"
                  color="warning"
                  onClick={() => setShowDebtInput(true)}
                  sx={{ cursor: "pointer", fontWeight: 600 }}
                />
              )}
              {debitSummary && (debitSummary.numberDebit || 0) > 0 ? (
                <Chip
                  icon={<WarningAmberIcon fontSize="small" />}
                  label={`Nợ: ${formatVND(debitSummary.totalDebts?.amount)} ${debitSummary.totalDebts?.currency || VN_CURRENCY} (${debitSummary.numberDebit})`}
                  size="small"
                  color="warning"
                  onClick={() => setShowDebitDialog(true)}
                  sx={{ cursor: "pointer", fontWeight: 600 }}
                />
              ) : (
                <Typography variant="caption" color="text.disabled">
                  Không có nợ
                </Typography>
              )}
            </Box>
          </Box>

          {/* Record debt input row */}
          {isPayment && remainingPayable > 0 && showDebtInput && (
            <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5 }}>
              <Typography variant="body2" color="text.secondary" sx={{ minWidth: 60 }}>
                Ghi nợ
              </Typography>
              <TextField
                fullWidth
                size="small"
                placeholder="0"
                value={debtInputVal}
                onChange={handleDebtInputChange}
                autoFocus
              />
              <Button
                variant="contained"
                color="warning"
                size="small"
                onClick={handleRecordDebt}
                disabled={!canRecordDebt}
                sx={{ whiteSpace: "nowrap" }}
                disableElevation
              >
                Ghi nợ
              </Button>
              <IconButton
                size="small"
                onClick={handleCloseDebtInput}
                sx={{ p: 0.25 }}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          )}

          {/* Service breakdown */}
          {regularServices.length > 0 && (
            <>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}>
                <ReceiptLongIcon fontSize="small" color="action" />
                <Typography variant="body2" color="text.secondary">
                  Chi tiết dịch vụ
                </Typography>
              </Box>
              <List dense disablePadding sx={{ pl: 4 }}>
                {regularServices.map((svc, idx) => (
                  <ListItem
                    key={idx}
                    disableGutters
                    disablePadding
                    sx={{
                      py: 0.5,
                      borderBottom: 1,
                      borderColor: "divider",
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                          }}
                        >
                          <Typography variant="body2">{svc.serviceName}</Typography>
                          <Typography variant="body2" fontWeight={600}>
                            {formatVND(svc.cost)} {VN_CURRENCY}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </>
          )}

          {/* Advance service display (aligned with ServiceDialog) */}
          {advanceItem && (
            <List dense disablePadding sx={{ pl: 4, mt: 1 }}>
              <ListItem
                disableGutters
                disablePadding
                sx={{
                  py: 1,
                  pl: 2,
                  borderLeft: 4,
                  borderLeftColor: "success.main",
                  bgcolor: "success.50",
                  borderBottom: 1,
                  borderColor: "divider",
                }}
              >
                <ListItemText
                  primary={
                    <Stack direction="row" alignItems="center" spacing={1}>
                      <Chip
                        label="Đã trả"
                        size="small"
                        color="success"
                        variant="outlined"
                        sx={{ fontSize: "0.65rem", height: 20, fontWeight: 500 }}
                      />
                      <Typography variant="body2" fontWeight={600}>
                        {ADVANCE_SERVICE_NAME}
                      </Typography>
                    </Stack>
                  }
                  secondary={
                    <Typography
                      variant="body2"
                      sx={{
                        color: "success.dark",
                        fontWeight: 600,
                        mt: 0.5
                      }}
                    >
                      {formatVND(advanceAmount)} {VN_CURRENCY}
                    </Typography>
                  }
                />
              </ListItem>
            </List>
          )}
        </DialogContent>

        <Divider />

        {/* ── Total line ── */}
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            px: 3,
            py: 1.5,
            bgcolor: "grey.50",
          }}
        >
          <Typography variant="subtitle1" fontWeight={700}>
            Tổng cộng
          </Typography>
          <Typography variant="subtitle1" fontWeight={700} color={headerColor}>
            {formatVND(totalCost)} {VN_CURRENCY}
          </Typography>
        </Box>

        {/* ── Recorded debt lines ── */}
        {debtList.map((debt, i) => (
          <Box
            key={i}
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              px: 3,
              py: 1.5,
              bgcolor: "warning.light",
              borderTop: 1,
              borderColor: "divider",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
              <WarningAmberIcon fontSize="small" color="warning.dark" />
              <Typography variant="body2" color="warning.dark" fontWeight={600}>
                Ghi nợ
              </Typography>
            </Box>
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
              <Typography variant="body2" color="warning.dark" fontWeight={700}>
                {formatVND(debt)} {VN_CURRENCY}
              </Typography>
              <IconButton
                size="small"
                onClick={() => removeDebtAt(i)}
                sx={{ p: 0.25, color: "warning.dark" }}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>
        ))}
        {pendingDebt > 0 && (
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              px: 3,
              py: 1.5,
              bgcolor: "warning.light",
              borderTop: 1,
              borderColor: "divider",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
              <WarningAmberIcon fontSize="small" color="warning.dark" />
              <Typography variant="body2" color="warning.dark" fontWeight={600}>
                Ghi nợ
              </Typography>
            </Box>
            <Typography variant="body2" color="warning.dark" fontWeight={700}>
              {formatVND(pendingDebt)} {VN_CURRENCY}
            </Typography>
          </Box>
        )}

        {/* ── Return amount line (if advance > total) ── */}
        {returnAmount > 0 && (
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              px: 3,
              py: 1.5,
              bgcolor: "warning.light",
              borderTop: 1,
              borderColor: "divider",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
              <ArrowBackIcon fontSize="small" color="warning.dark" />
              <Typography variant="body2" color="warning.dark" fontWeight={600}>
                Tiền trả lại
              </Typography>
            </Box>
            <Typography variant="body2" color="warning.dark" fontWeight={700}>
              {formatVND(returnAmount)} {VN_CURRENCY}
            </Typography>
          </Box>
        )}

        {/* ── Actions ── */}
        <DialogActions sx={{ px: 3, pb: 2.5, pt: 1.5, gap: 1.5 }}>
          <Button
            variant="outlined"
            color="inherit"
            onClick={onExit}
            fullWidth
            size="large"
            sx={{
              borderRadius: 2,
              borderColor: "divider",
              color: "text.secondary",
              py: 1.2,
            }}
          >
            Huỷ
          </Button>
          <Button
            variant="contained"
            color={isPayment ? "success" : "error"}
            onClick={() => onConfirm({
              ...data,
              returnAmount: returnAmount,
              amountToPay: amountToPay,
              totalCost: totalCost,
              debitAmount: totalDebitAmount
            })}
            fullWidth
            size="large"
            disableElevation
            sx={{
              borderRadius: 2,
              py: 1.2,
              fontWeight: 700,
            }}
          >
            {actionLabel}
          </Button>
        </DialogActions>
      </Dialog>
      <DebitListDialog
        show={showDebitDialog}
        playerName={data.playerName}
        onClose={() => setShowDebitDialog(false)}
        onPaid={fetchDebitSummary}
      />
    </>
  );
};

export default PayConfirm;
