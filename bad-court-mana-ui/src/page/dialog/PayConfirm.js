import React, { useState, useEffect } from "react";
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
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import api from "../../api/index";
import { TYPE, ADVANCE_SERVICE_NAME } from "../HomePage";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";

const PayConfirm = ({ show, data, onConfirm, onExit }) => {
  const [debitSummary, setDebitSummary] = useState(null);
  const [showDebitDialog, setShowDebitDialog] = useState(false);

  useEffect(() => {
    if (!show || !data?.playerName) {
      setDebitSummary(null);
      return;
    }
    api
      .get(`/api/v1/debit/summary?playerName=${encodeURIComponent(data.playerName)}`)
      .then((res) => {
        if (res?.data) setDebitSummary(res.data);
      })
      .catch(() => setDebitSummary(null));
  }, [show, data?.playerName]);

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

  // Amount to pay now (0 if remaining is negative, otherwise show remaining)
  const amountToPay = remainingAmount > 0 ? remainingAmount : 0;

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
            py: 3,
            px: 2,
          }}
        >
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
              <Typography variant="body1" fontWeight={600}>
                {data.playerName}
              </Typography>
            </Box>

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
              totalCost: totalCost
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
      />
    </>
  );
};

const DebitListDialog = ({ show, playerName, onClose }) => {
  const [debtData, setDebtData] = useState({
    remainingDebits: [],
    debitSummary: null,
  });

  useEffect(() => {
    if (!show || !playerName) {
      setDebtData({ remainingDebits: [], debitSummary: null });
      return;
    }
    api
      .post("/api/v1/debit/listRemainingDebts", {
        playerNames: [playerName],
        pagination: { current: 0, pageSize: 50, totalPage: 0 },
        filter: { from: null, to: null, amountFrom: 0, amountTo: 0 },
      })
      .then((res) => {
        if (res?.data) setDebtData(res.data);
      })
      .catch(() => setDebtData({ remainingDebits: [], debitSummary: null }));
  }, [show, playerName]);

  const { remainingDebits = [], debitSummary } = debtData;
  const totalDebts = debitSummary?.totalDebts?.amount || 0;
  const numberDebit = debitSummary?.numberDebit || 0;

  return (
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
            <Divider sx={{ mb: 1 }} />
            <List dense disablePadding>
              {remainingDebits.map((debt, idx) => (
                <ListItem
                  key={idx}
                  disableGutters
                  disablePadding
                  sx={{ py: 0.75, borderBottom: 1, borderColor: "divider" }}
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
                        <Typography variant="body2">
                          {debt.note || "Nợ"}
                        </Typography>
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
                        {debt.dateTime}
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5, pt: 1.5 }}>
        <Button
          variant="contained"
          color="warning"
          onClick={onClose}
          fullWidth
          disableElevation
          sx={{ borderRadius: 2, py: 1.2, fontWeight: 700 }}
        >
          Đóng
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default PayConfirm;
