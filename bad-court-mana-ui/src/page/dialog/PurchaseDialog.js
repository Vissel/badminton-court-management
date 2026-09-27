import React, { useState, useEffect, useMemo } from "react";
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
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import InputAdornment from "@mui/material/InputAdornment";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import { VN_CURRENCY, formatVND, rawNumber } from "../MoneyUtils";
import { ITEM_TYPES } from "./InventoryItemDialog";
import { recordPurchase } from "../../api/inventoryApi";

/** Default package config applied by the BE when creating a new SHUTTLE_BALL. */
const NEW_BALL_DEFAULTS = { packageUnit: "ống", unitsPerPackage: 12, unit: "quả" };

const todayStr = () => {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

/**
 * Wholesale purchase intake: pick an existing item (autocomplete over `items`)
 * or type a new name + type. Posts POST /api/inventory/purchases.
 */
const PurchaseDialog = ({ open, items, onClose, onSaved }) => {
  const [name, setName] = useState("");
  const [itemType, setItemType] = useState("GOODS");
  const [unit, setUnit] = useState("PACKAGE");
  const [quantity, setQuantity] = useState("");
  const [unitCost, setUnitCost] = useState("");
  const [purchaseDate, setPurchaseDate] = useState(todayStr());
  const [supplier, setSupplier] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setName("");
      setItemType("GOODS");
      setUnit("PACKAGE");
      setQuantity("");
      setUnitCost("");
      setPurchaseDate(todayStr());
      setSupplier("");
      setNote("");
      setError(null);
    }
  }, [open]);

  const matchedItem = useMemo(
    () =>
      (items || []).find(
        (i) => i.itemName.trim().toLowerCase() === name.trim().toLowerCase()
      ),
    [items, name]
  );
  const isNewItem = name.trim() && !matchedItem;

  // Items with a package unit can be intaken either by package or by base
  // unit; for a brand-new SHUTTLE_BALL the BE applies the ống/quả defaults.
  const unitConfig = matchedItem
    ? matchedItem
    : isNewItem && itemType === "SHUTTLE_BALL"
      ? NEW_BALL_DEFAULTS
      : null;
  const hasPackage = Boolean(unitConfig?.packageUnit && unitConfig?.unitsPerPackage > 0);
  const effectiveUnit = hasPackage ? unit : "BASE";

  const filteredOptions = useMemo(() => {
    const q = name.trim().toLowerCase();
    if (!q) return [];
    return (items || []).filter((i) =>
      i.itemName.toLowerCase().includes(q)
    );
  }, [items, name]);

  const qtyNum = rawNumber(quantity || "0");
  const baseQty =
    hasPackage && effectiveUnit === "PACKAGE"
      ? qtyNum * unitConfig.unitsPerPackage
      : qtyNum;
  const canSave =
    name.trim() && qtyNum > 0 && unitCost !== "" && purchaseDate;

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError(null);
    const payload = {
      ...(matchedItem
        ? { itemId: matchedItem.itemId }
        : { itemName: name.trim(), itemType }),
      purchaseDate,
      unit: effectiveUnit,
      quantity: qtyNum,
      unitCost: rawNumber(unitCost),
      ...(supplier.trim() ? { supplier: supplier.trim() } : {}),
      ...(note.trim() ? { note: note.trim() } : {}),
    };
    try {
      const res = await recordPurchase(payload);
      if (res?.data?.success) {
        onSaved?.();
        onClose();
      } else {
        setError(res?.data?.errorMessage || "Nhập kho thất bại.");
      }
    } catch (err) {
      console.error("Record purchase failed", err);
      setError("Có lỗi khi nhập kho.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Nhập kho</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          <Box sx={{ position: "relative" }}>
            <TextField
              label="Mặt hàng"
              value={name}
              onChange={(e) => setName(e.target.value)}
              fullWidth
              autoFocus
            />
            {filteredOptions.length > 0 && name !== matchedItem?.itemName && (
              <Paper
                sx={{
                  position: "absolute",
                  top: "100%",
                  left: 0,
                  right: 0,
                  zIndex: 20,
                  maxHeight: 180,
                  overflow: "auto",
                  mt: 0.25,
                }}
              >
                {filteredOptions.map((opt) => (
                  <Box
                    key={opt.itemId}
                    onClick={() => {
                      setName(opt.itemName);
                      setItemType(opt.itemType);
                    }}
                    sx={{
                      px: 1.5,
                      py: 0.75,
                      cursor: "pointer",
                      fontSize: "0.85rem",
                      display: "flex",
                      justifyContent: "space-between",
                      "&:hover": { bgcolor: "action.hover" },
                    }}
                  >
                    <span>{opt.itemName}</span>
                    <span>
                      tồn {opt.stockOnHand} {opt.unit}
                      {opt.packageBreakdown ? ` (${opt.packageBreakdown})` : ""}
                    </span>
                  </Box>
                ))}
              </Paper>
            )}
          </Box>

          {isNewItem && (
            <FormControl fullWidth>
              <InputLabel>Loại (mặt hàng mới)</InputLabel>
              <Select
                label="Loại (mặt hàng mới)"
                value={itemType}
                onChange={(e) => setItemType(e.target.value)}
              >
                <MenuItem value="SHUTTLE_BALL">{ITEM_TYPES.SHUTTLE_BALL}</MenuItem>
                <MenuItem value="GOODS">{ITEM_TYPES.GOODS}</MenuItem>
              </Select>
            </FormControl>
          )}

          {hasPackage && (
            <ToggleButtonGroup
              exclusive
              value={unit}
              onChange={(_, v) => v && setUnit(v)}
              fullWidth
            >
              <ToggleButton value="PACKAGE">
                {unitConfig.packageUnit} (×{unitConfig.unitsPerPackage} {unitConfig.unit})
              </ToggleButton>
              <ToggleButton value="BASE">{unitConfig.unit}</ToggleButton>
            </ToggleButtonGroup>
          )}

          <Stack direction="row" spacing={1}>
            <TextField
              label={hasPackage ? `Số lượng (${unitConfig[unit === "PACKAGE" ? "packageUnit" : "unit"]})` : "Số lượng"}
              value={quantity}
              onChange={(e) =>
                setQuantity(
                  e.target.value === "" ? "" : String(rawNumber(e.target.value))
                )
              }
              fullWidth
            />
            <TextField
              label="Giá nhập"
              value={unitCost}
              onChange={(e) =>
                setUnitCost(
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
          </Stack>

          <TextField
            label="Ngày nhập"
            type="date"
            value={purchaseDate}
            onChange={(e) => setPurchaseDate(e.target.value)}
            fullWidth
            slotProps={{ inputLabel: { shrink: true } }}
          />

          <TextField
            label="Nhà cung cấp"
            value={supplier}
            onChange={(e) => setSupplier(e.target.value)}
            fullWidth
          />

          <TextField
            label="Ghi chú"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            fullWidth
          />

          {hasPackage && effectiveUnit === "PACKAGE" && qtyNum > 0 && (
            <Typography variant="body2" color="text.secondary">
              = {baseQty} {unitConfig.unit}
            </Typography>
          )}

          {qtyNum > 0 && unitCost !== "" && (
            <Typography variant="body2" color="text.secondary">
              Tổng nhập: {formatVND(qtyNum * rawNumber(unitCost))} {VN_CURRENCY}
              {hasPackage && effectiveUnit === "PACKAGE" && (
                <> (≈ {formatVND(rawNumber(unitCost) / unitConfig.unitsPerPackage)}{VN_CURRENCY}/{unitConfig.unit})</>
              )}
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
          {saving ? "Đang lưu..." : "Nhập kho"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default PurchaseDialog;
