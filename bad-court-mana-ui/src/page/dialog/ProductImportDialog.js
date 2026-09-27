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
import {
  previewImportProducts,
  commitImportProducts,
} from "../../api/productApi";

const ACTION_LABELS = {
  ADD: "Thêm mới",
  UPDATE: "Cập nhật giá",
  REACTIVATE: "Khôi phục",
  SKIP: "Bỏ qua",
  ERROR: "Lỗi",
};

const ACTION_COLORS = {
  ADD: "success",
  UPDATE: "info",
  REACTIVATE: "warning",
  SKIP: "default",
  ERROR: "error",
};

const ProductImportDialog = ({ open, onClose, onSuccess }) => {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [committing, setCommitting] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const handleReset = () => {
    setFile(null);
    setPreview(null);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleClose = () => {
    handleReset();
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
      const res = await previewImportProducts(selected);
      const body = res?.data;
      if (body?.success && body?.data) {
        setPreview(body.data);
      } else {
        setError(body?.errorMessage || "Không đọc được file. Vui lòng thử lại.");
      }
    } catch (err) {
      console.error("Preview import failed", err);
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
      const res = await commitImportProducts(preview.importToken);
      const body = res?.data;
      if (body?.success) {
        onSuccess?.();
        handleClose();
      } else {
        setError(body?.errorMessage || "Nhập hàng thất bại.");
      }
    } catch (err) {
      console.error("Commit import failed", err);
      setError("Có lỗi khi áp dụng file nhập.");
    } finally {
      setCommitting(false);
    }
  };

  const validRows = preview?.rows?.filter((r) => r.action !== "ERROR") || [];
  const errorRows = preview?.rows?.filter((r) => r.action === "ERROR") || [];
  const counts = preview?.counts || {};
  const canCommit = validRows.length > 0 && !loading && !committing;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>Nhập hàng từ file Excel</DialogTitle>
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
            {file ? file.name : "Chọn file .xlsx"}
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
                  <Chip size="small" color="info" label={`Cập nhật: ${counts.updated}`} />
                )}
                {counts.reactivated > 0 && (
                  <Chip size="small" color="warning" label={`Khôi phục: ${counts.reactivated}`} />
                )}
                {counts.skipped > 0 && (
                  <Chip size="small" label={`Bỏ qua: ${counts.skipped}`} />
                )}
                {counts.errors > 0 && (
                  <Chip size="small" color="error" label={`Lỗi: ${counts.errors}`} />
                )}
              </Stack>

              {validRows.length > 0 && (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Dòng</TableCell>
                        <TableCell>Sheet</TableCell>
                        <TableCell>Tên</TableCell>
                        <TableCell>Giá ({VN_CURRENCY})</TableCell>
                        <TableCell>Hành động</TableCell>
                        <TableCell>Ghi chú</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {validRows.map((row, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{row.row}</TableCell>
                          <TableCell>{row.sheet}</TableCell>
                          <TableCell>{row.name}</TableCell>
                          <TableCell>{formatVND(row.cost)}</TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              color={ACTION_COLORS[row.action] || "default"}
                              label={ACTION_LABELS[row.action] || row.action}
                            />
                          </TableCell>
                          <TableCell>{row.message || "-"}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}

              {errorRows.length > 0 && (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Dòng</TableCell>
                        <TableCell>Sheet</TableCell>
                        <TableCell>Tên</TableCell>
                        <TableCell>Giá ({VN_CURRENCY})</TableCell>
                        <TableCell>Lỗi</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {errorRows.map((row, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{row.row}</TableCell>
                          <TableCell>{row.sheet}</TableCell>
                          <TableCell>{row.name || "-"}</TableCell>
                          <TableCell>{row.cost != null ? formatVND(row.cost) : "-"}</TableCell>
                          <TableCell sx={{ color: "error.main" }}>{row.message}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
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

export default ProductImportDialog;
