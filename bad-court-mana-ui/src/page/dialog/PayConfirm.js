import React from "react";
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
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import { TYPE } from "../HomePage";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";

const ADVANCE_SERVICE_NAME = "Tr\u1ea3 tr\u01b0\u1edbc";

const PayConfirm = ({ show, data, onConfirm, onExit }) => {
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
  const netTotal = data.expense;

  const services = regularServices;

  return (
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
          {formatVND(data.expense)}{" "}
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
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5 }}>
          <PersonOutlinedIcon fontSize="small" color="action" />
          <Typography variant="body2" color="text.secondary" sx={{ minWidth: 60 }}>
            Người chơi
          </Typography>
          <Typography variant="body1" fontWeight={600}>
            {data.playerName}
          </Typography>
        </Box>

        {/* Service breakdown */}
        {services.length > 0 && (
          <>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}>
              <ReceiptLongIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                Chi tiết dịch vụ
              </Typography>
            </Box>
            <List dense disablePadding sx={{ pl: 4 }}>
              {services.map((svc, idx) => (
                <ListItem key={idx} disableGutters disablePadding sx={{ py: 0.3 }}>
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
      </DialogContent>

      <Divider />

      {/* ── Advance deduction (if any) ── */}
      {advanceAmount > 0 && (
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            px: 3,
            py: 1,
            bgcolor: "info.light",
            borderBottom: 1,
            borderColor: "divider",
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
            <PaymentsOutlinedIcon fontSize="small" color="info.dark" />
            <Typography variant="body2" color="info.dark" fontWeight={600}>
              Đã trả trước
            </Typography>
          </Box>
          <Typography variant="body2" color="info.dark" fontWeight={700}>
            −{formatVND(advanceAmount)} {VN_CURRENCY}
          </Typography>
        </Box>
      )}

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
          {formatVND(netTotal)} {VN_CURRENCY}
        </Typography>
      </Box>

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
          onClick={() => onConfirm(data)}
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
  );
};

export default PayConfirm;
