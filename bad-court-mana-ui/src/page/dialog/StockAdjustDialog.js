import React, { useState, useEffect } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import { rawNumber } from "../MoneyUtils";
import { adjustStock } from "../../api/inventoryApi";

/**
 * Stock-take correction: signed quantity delta + mandatory reason.
 */
const StockAdjustDialog = ({ open, item, onClose, onSaved }) => {
  const [direction, setDirection] = useState("in"); // in | out
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setDirection("in");
      setQuantity("");
      setReason("");
      setError(null);
    }
  }, [open]);

  const qtyNum = rawNumber(quantity || "0");
  const delta = direction === "in" ? qtyNum : -qtyNum;
  const resultingStock = (item?.stockOnHand ?? 0) + delta;
  const canSave = qtyNum > 0 && reason.trim().length > 0 && resultingStock >= 0;

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError(null);
    try {
      const res = await adjustStock({
        itemId: item.itemId,
        quantityDelta: delta,
        reason: reason.trim(),
      });
      if (res?.data?.success) {
        onSaved?.();
        onClose();
      } else {
        setError(res?.data?.errorMessage || "Kiểm kê thất bại.");
      }
    } catch (err) {
      console.error("Adjust stock failed", err);
      setError("Có lỗi khi kiểm kê.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Kiểm kê — {item?.itemName}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          <Typography variant="body2" color="text.secondary">
            Tồn hiện tại: {item?.stockOnHand} {item?.unit}
          </Typography>

          <ToggleButtonGroup
            exclusive
            value={direction}
            onChange={(_, v) => v && setDirection(v)}
            fullWidth
          >
            <ToggleButton value="in" color="success">
              Nhập thêm (+)
            </ToggleButton>
            <ToggleButton value="out" color="error">
              Giảm đi (−)
            </ToggleButton>
          </ToggleButtonGroup>

          <TextField
            label={item?.unit ? `Số lượng (${item.unit})` : "Số lượng"}
            value={quantity}
            onChange={(e) =>
              setQuantity(
                e.target.value === "" ? "" : String(rawNumber(e.target.value))
              )
            }
            fullWidth
            autoFocus
          />

          <TextField
            label="Lý do"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            required
            fullWidth
            placeholder="VD: hỏng, mất, đếm lại kho..."
          />

          {qtyNum > 0 && (
            <Typography
              variant="body2"
              color={resultingStock >= 0 ? "text.secondary" : "error"}
            >
              Tồn sau kiểm kê: {resultingStock} {item?.unit}
            </Typography>
          )}

          {error && (
            <Typography variant="body2" color="error">
              {error}
            </Typography>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <Button onClick={onClose} disabled={saving}>
          Huỷ
        </Button>
        <Button variant="contained" onClick={handleSave} disabled={!canSave || saving}>
          {saving ? "Đang lưu..." : "Xác nhận"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StockAdjustDialog;
