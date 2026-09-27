import React, { useState, useRef } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import { ITEM_TYPES } from "./InventoryItemDialog";
import { previewStockIntake, commitStockIntake } from "../../api/inventoryApi";

const ACTION_LABELS = {
  ADD: "Thêm mới",
  UPDATE: "Nhập thêm",
  SKIP: "Bỏ qua",
  ERROR: "Lỗi",
};

const ACTION_COLORS = {
  ADD: "success",
  UPDATE: "info",
  SKIP: "default",
  ERROR: "error",
};

/**
 * Bulk stock intake from a StockIntake .xlsx sheet
 * (columns: Tên | Số lượng | Giá nhập | Ngày nhập | Đơn vị - optional,
 * e.g. ống/quả; blank defaults to the item's package unit). Preview → commit.
 */
const StockIntakeDialog = ({ open, onClose, onSuccess }) => {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [committing, setCommitting] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const handleClose = () => {
    setFile(null);
    setPreview(null);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    onClose();
  };

  const handleFileChange = async (e) => {
    const selected = e.target.files?.[0];
    if (!selected) return;

    setFile(selected);
    setPreview(null);
    setError(null);
    setLoading(true);

    try {
      const res = await previewStockIntake(selected);
      const body = res?.data;
      if (body?.success && body?.data) {
        setPreview(body.data);
      } else {
        setError(body?.errorMessage || "Không đọc được file. Vui lòng thử lại.");
      }
    } catch (err) {
      console.error("Preview intake failed", err);
      setError("Có lỗi khi đọc file. Kiểm tra định dạng .xlsx.");
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!preview?.importToken) return;
    setCommitting(true);
    setError(null);
    try {
      const res = await commitStockIntake(preview.importToken);
      if (res?.data?.success) {
        onSuccess?.();
        handleClose();
      } else {
        setError(res?.data?.errorMessage || "Nhập kho thất bại.");
      }
    } catch (err) {
      console.error("Commit intake failed", err);
      setError("Có lỗi khi áp dụng file nhập.");
    } finally {
      setCommitting(false);
    }
  };

  const rows = preview?.rows || [];
  const validRows = rows.filter((r) => r.action !== "ERROR");
  const counts = preview?.counts || {};
  const canCommit = validRows.length > 0 && !loading && !committing;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>Nhập kho từ file Excel</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Button
            variant="outlined"
            component="label"
            startIcon={<CloudUploadIcon />}
            disabled={loading || committing}
            fullWidth
            sx={{ py: 1.5 }}
          >
            {file ? file.name : "Chọn file .xlsx (sheet StockIntake)"}
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              hidden
              onChange={handleFileChange}
            />
          </Button>

          {loading && (
            <Box sx={{ textAlign: "center", py: 2 }}>
              <CircularProgress size={24} sx={{ mr: 1 }} />
              <Typography variant="body2" color="text.secondary" component="span">
                Đang đọc file...
              </Typography>
            </Box>
          )}

          {error && (
            <Typography variant="body2" color="error">
              {error}
            </Typography>
          )}

          {preview && (
            <>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                {counts.added > 0 && (
                  <Chip size="small" color="success" label={`Thêm: ${counts.added}`} />
                )}
                {counts.updated > 0 && (
                  <Chip size="small" color="info" label={`Nhập thêm: ${counts.updated}`} />
                )}
                {counts.skipped > 0 && (
                  <Chip size="small" label={`Bỏ qua: ${counts.skipped}`} />
                )}
                {counts.errors > 0 && (
                  <Chip size="small" color="error" label={`Lỗi: ${counts.errors}`} />
                )}
              </Stack>

              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Dòng</TableCell>
                      <TableCell>Tên</TableCell>
                      <TableCell>Loại</TableCell>
                      <TableCell>Số lượng</TableCell>
                      <TableCell>Giá nhập ({VN_CURRENCY})</TableCell>
                      <TableCell>Ngày nhập</TableCell>
                      <TableCell>Hành động</TableCell>
                      <TableCell>Ghi chú</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row, idx) => (
                      <TableRow
                        key={idx}
                        sx={
                          row.action === "ERROR"
                            ? { bgcolor: "error.50" }
                            : undefined
                        }
                      >
                        <TableCell>{row.rowNumber}</TableCell>
                        <TableCell>{row.name || "-"}</TableCell>
                        <TableCell>
                          {ITEM_TYPES[row.itemType] || row.itemType || "-"}
                        </TableCell>
                        <TableCell>
                          {row.quantity ?? "-"}
                          {row.unit ? ` ${row.unit}` : ""}
                          {row.baseQuantity != null &&
                            row.baseQuantity !== row.quantity &&
                            ` (= ${row.baseQuantity})`}
                        </TableCell>
                        <TableCell>
                          {row.unitCost != null ? formatVND(row.unitCost) : "-"}
                        </TableCell>
                        <TableCell>{row.purchaseDate || "-"}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            color={ACTION_COLORS[row.action] || "default"}
                            label={ACTION_LABELS[row.action] || row.action}
                          />
                        </TableCell>
                        <TableCell sx={row.action === "ERROR" ? { color: "error.main" } : undefined}>
                          {row.message || "-"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <Button onClick={handleClose} disabled={committing}>
          Đóng
        </Button>
        <Button
          variant="contained"
          onClick={handleCommit}
          disabled={!canCommit}
          loading={committing}
        >
          {committing ? "Đang áp dụng..." : `Áp dụng ${validRows.length} dòng hợp lệ`}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StockIntakeDialog;
