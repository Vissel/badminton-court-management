import React, { useState, useEffect } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import InputAdornment from "@mui/material/InputAdornment";
import { VN_CURRENCY, formatVND, rawNumber } from "../MoneyUtils";
import { createInventoryItem, updateInventoryItem } from "../../api/inventoryApi";

export const ITEM_TYPES = {
  SHUTTLE_BALL: "Cầu lông",
  GOODS: "Hàng hoá",
};

/**
 * Create / edit an inventory item. `item` null → create mode.
 */
const InventoryItemDialog = ({ open, item, onClose, onSaved }) => {
  const isEdit = Boolean(item);
  const [itemName, setItemName] = useState("");
  const [itemType, setItemType] = useState("GOODS");
  const [unit, setUnit] = useState("");
  const [packageUnit, setPackageUnit] = useState("");
  const [unitsPerPackage, setUnitsPerPackage] = useState("");
  const [retailPrice, setRetailPrice] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setItemName(item?.itemName || "");
      setItemType(item?.itemType || "GOODS");
      setUnit(item?.unit || "");
      setPackageUnit(item?.packageUnit || "");
      setUnitsPerPackage(
        item?.unitsPerPackage != null ? String(item.unitsPerPackage) : ""
      );
      setRetailPrice(item?.retailPrice != null ? formatVND(item.retailPrice) : "");
      setError(null);
    }
  }, [open, item]);

  const canSave = itemName.trim() && itemType && retailPrice !== "";

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError(null);
    const pkgTrim = packageUnit.trim();
    const payload = {
      itemName: itemName.trim(),
      itemType,
      unit: unit.trim(),
      packageUnit: pkgTrim,
      ...(pkgTrim && unitsPerPackage !== ""
        ? { unitsPerPackage: rawNumber(unitsPerPackage) }
        : {}),
      retailPrice: rawNumber(retailPrice || "0"),
      ...(isEdit ? { isActive: item.active } : {}),
    };
    try {
      const res = isEdit
        ? await updateInventoryItem(item.itemId, payload)
        : await createInventoryItem(payload);
      if (res?.data?.success) {
        onSaved?.();
        onClose();
      } else {
        setError(res?.data?.errorMessage || "Lưu mặt hàng thất bại.");
      }
    } catch (err) {
      console.error("Save item failed", err);
      setError("Có lỗi khi lưu mặt hàng.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{isEdit ? "Sửa mặt hàng" : "Thêm mặt hàng"}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          <TextField
            label="Tên mặt hàng"
            value={itemName}
            onChange={(e) => setItemName(e.target.value)}
            fullWidth
            autoFocus
          />
          <FormControl fullWidth>
            <InputLabel>Loại</InputLabel>
            <Select
              label="Loại"
              value={itemType}
              onChange={(e) => setItemType(e.target.value)}
              disabled={isEdit}
            >
              <MenuItem value="SHUTTLE_BALL">{ITEM_TYPES.SHUTTLE_BALL}</MenuItem>
              <MenuItem value="GOODS">{ITEM_TYPES.GOODS}</MenuItem>
            </Select>
          </FormControl>
          <TextField
            label="Đơn vị (đếm kho)"
            value={unit}
            onChange={(e) => setUnit(e.target.value)}
            placeholder="quả, chai..."
            fullWidth
          />
          <Stack direction="row" spacing={1}>
            <TextField
              label="Đơn vị nhập sỉ"
              value={packageUnit}
              onChange={(e) => setPackageUnit(e.target.value)}
              placeholder="ống, thùng..."
              helperText="Bỏ trống nếu chỉ nhập theo đơn vị lẻ"
              fullWidth
            />
            <TextField
              label="Quy đổi"
              value={unitsPerPackage}
              onChange={(e) =>
                setUnitsPerPackage(
                  e.target.value === "" ? "" : String(rawNumber(e.target.value))
                )
              }
              disabled={!packageUnit.trim()}
              helperText={
                packageUnit.trim()
                  ? `1 ${packageUnit.trim()} = ? ${unit.trim() || "đơn vị"}`
                  : " "
              }
              fullWidth
            />
          </Stack>
          <TextField
            label="Giá bán"
            value={retailPrice}
            onChange={(e) =>
              setRetailPrice(
                e.target.value === "" ? "" : formatVND(rawNumber(e.target.value))
              )
            }
            fullWidth
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                ),
              },
            }}
          />
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
          {saving ? "Đang lưu..." : "Lưu"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default InventoryItemDialog;
