import React, { useState, useEffect, useCallback } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import { listStockMovements } from "../../api/inventoryApi";

const MOVEMENT_LABELS = {
  PURCHASE_IN: "Nhập kho",
  GAME_CONSUMPTION: "Sử dụng (trận)",
  RETAIL_SALE: "Bán lẻ",
  ADJUSTMENT: "Kiểm kê",
  RETURN: "Trả lại",
};

const MOVEMENT_COLORS = {
  PURCHASE_IN: "success",
  GAME_CONSUMPTION: "warning",
  RETAIL_SALE: "info",
  ADJUSTMENT: "default",
  RETURN: "secondary",
};

const REF_LABELS = {
  PURCHASE_LOT: "Nhập hàng",
  GAME: "Trận đấu",
  PAYMENT: "Thanh toán",
};

const formatCreated = (value) => {
  if (value == null) return "-";
  const d = new Date(typeof value === "string" && !value.includes("T") ? value.replace(" ", "T") : value);
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString("vi-VN");
};

/**
 * Per-item stock ledger (stock_movement history).
 */
const StockLedgerDialog = ({ open, item, onClose }) => {
  const [movements, setMovements] = useState([]);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchMovements = useCallback(async () => {
    if (!item) return;
    setLoading(true);
    setError(null);
    try {
      const res = await listStockMovements(item.itemId, from || undefined, to || undefined);
      const body = res?.data;
      if (body?.success) {
        setMovements(body.data || []);
      } else {
        setError(body?.errorMessage || "Không tải được lịch sử kho.");
      }
    } catch (err) {
      console.error("Fetch movements failed", err);
      setError("Có lỗi khi tải lịch sử kho.");
    } finally {
      setLoading(false);
    }
  }, [item, from, to]);

  useEffect(() => {
    if (open) {
      setFrom("");
      setTo("");
      fetchMovements();
    }
    // only reset+fetch on open/item change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, item?.itemId]);

  const stockOnHand = item?.stockOnHand ?? 0;
  const stockColor =
    item?.outOfStock || stockOnHand <= 0
      ? "error"
      : item?.lowStock
        ? "warning"
        : "success";

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction="row" alignItems="center" spacing={1.5}>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography variant="h6" component="div" noWrap>
              {item?.itemName}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Lịch sử biến động kho
            </Typography>
          </Box>
          <Chip
            size="small"
            color={stockColor}
            label={`Tồn: ${stockOnHand} ${item?.unit ?? ""}`}
            sx={{ fontWeight: 700, flexShrink: 0 }}
          />
        </Stack>
      </DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Stack direction="row" spacing={1} alignItems="center">
            <TextField
              label="Từ ngày"
              type="date"
              size="small"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="Đến ngày"
              type="date"
              size="small"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <Button variant="outlined" size="small" onClick={fetchMovements} disabled={loading}>
              Lọc
            </Button>
          </Stack>

          {error && (
            <Typography variant="body2" color="error">
              {error}
            </Typography>
          )}

          {loading ? (
            <CircularProgress size={24} sx={{ alignSelf: "center", my: 2 }} />
          ) : (
            <TableContainer
              component={Paper}
              variant="outlined"
              sx={{ maxHeight: 420, borderRadius: 1.5 }}
            >
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow
                    sx={{
                      "& .MuiTableCell-head": {
                        bgcolor: "grey.100",
                        fontWeight: 700,
                        whiteSpace: "nowrap",
                      },
                    }}
                  >
                    <TableCell>Thời gian</TableCell>
                    <TableCell>Loại</TableCell>
                    <TableCell align="right">Số lượng</TableCell>
                    <TableCell align="right">Đơn giá ({VN_CURRENCY})</TableCell>
                    <TableCell>Tham chiếu</TableCell>
                    <TableCell>Ghi chú</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {movements.map((m) => (
                    <TableRow
                      key={m.movementId}
                      hover
                      sx={{ "&:nth-of-type(odd)": { bgcolor: "grey.50" } }}
                    >
                      <TableCell sx={{ whiteSpace: "nowrap", color: "text.secondary" }}>
                        {formatCreated(m.createdDate)}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          variant="outlined"
                          color={MOVEMENT_COLORS[m.movementType] || "default"}
                          label={MOVEMENT_LABELS[m.movementType] || m.movementType}
                        />
                      </TableCell>
                      <TableCell
                        align="right"
                        sx={{
                          fontWeight: 700,
                          fontVariantNumeric: "tabular-nums",
                          color: m.quantityDelta >= 0 ? "success.main" : "error.main",
                        }}
                      >
                        {m.quantityDelta >= 0 ? "+" : ""}
                        {m.quantityDelta} {item?.unit}
                      </TableCell>
                      <TableCell align="right" sx={{ fontVariantNumeric: "tabular-nums" }}>
                        {m.unitCost != null ? formatVND(m.unitCost) : "-"}
                      </TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>
                        {m.refType
                          ? `${REF_LABELS[m.refType] || m.refType}${m.refId != null ? ` #${m.refId}` : ""}`
                          : "-"}
                      </TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>
                        {m.note || "-"}
                      </TableCell>
                    </TableRow>
                  ))}
                  {movements.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                        <Typography color="text.secondary">
                          Chưa có biến động kho.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Đóng</Button>
      </DialogActions>
    </Dialog>
  );
};

export default StockLedgerDialog;
