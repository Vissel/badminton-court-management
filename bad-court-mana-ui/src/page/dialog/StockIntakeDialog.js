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
import DateInput from "../DateInput";
import { VN_CURRENCY, formatVND, rawNumber } from "../MoneyUtils";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./DraggableResizablePaper";
import { ITEM_TYPES } from "./InventoryItemDialog";
import { recordPurchase } from "../../api/inventoryApi";

/** Default unit config applied by the BE when creating a new SHUTTLE_BALL. */
const NEW_BALL_DEFAULTS = { packageUnit: "ống", unitsPerPackage: 12, unit: "quả" };

const todayStr = () => {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

/**
 * Manual wholesale intake (Nhập sỉ): pick an existing item (autocomplete over
 * `items`) or type a new name + type, define the item's Đơn vị lẻ / Đơn vị sỉ /
 * Quy đổi sỉ sang lẻ, then buy by Đơn vị sỉ or Đơn vị lẻ at the corresponding
 * buy price and optionally set Giá bán lẻ. Posts POST /api/inventory/purchases.
 */
const StockIntakeDialog = ({ open, items, onClose, onSaved }) => {
  const [name, setName] = useState("");
  const [itemType, setItemType] = useState("GOODS");
  const [baseUnit, setBaseUnit] = useState("");
  const [packageUnit, setPackageUnit] = useState("");
  const [unitsPerPackage, setUnitsPerPackage] = useState("");
  const [unit, setUnit] = useState("PACKAGE");
  const [quantity, setQuantity] = useState("");
  const [unitCost, setUnitCost] = useState("");
  const [retailPrice, setRetailPrice] = useState("");
  const [purchaseDate, setPurchaseDate] = useState(todayStr());
  const [supplier, setSupplier] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setName("");
      setItemType("GOODS");
      setBaseUnit("");
      setPackageUnit("");
      setUnitsPerPackage("");
      setUnit("PACKAGE");
      setQuantity("");
      setUnitCost("");
      setRetailPrice("");
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

  // Prefill the unit section from the matched item so the sỉ/lẻ mapping the
  // intake will use is exactly what is saved.
  useEffect(() => {
    if (matchedItem) {
      setItemType(matchedItem.itemType);
      setBaseUnit(matchedItem.unit || "");
      setPackageUnit(matchedItem.packageUnit || "");
      setUnitsPerPackage(
        matchedItem.unitsPerPackage != null ? String(matchedItem.unitsPerPackage) : ""
      );
    }
  }, [matchedItem]);

  const baseUnitVal = baseUnit.trim();
  const packageUnitVal = packageUnit.trim();
  const perPackage = rawNumber(unitsPerPackage || "0");
  const hasPackage = Boolean(packageUnitVal && perPackage > 0);
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
    hasPackage && effectiveUnit === "PACKAGE" ? qtyNum * perPackage : qtyNum;
  const canSave =
    name.trim() && qtyNum > 0 && unitCost !== "" && purchaseDate;

  // Unit the two prices are expressed in, driven by the sỉ/lẻ selection.
  const intakeUnit = hasPackage
    ? effectiveUnit === "PACKAGE"
      ? packageUnitVal
      : baseUnitVal || null
    : baseUnitVal || null;
  const buyPriceLabel =
    hasPackage && effectiveUnit === "PACKAGE" ? "Giá sỉ" : "Giá nhập lẻ";
  const quantityLabel = intakeUnit
    ? `${hasPackage && effectiveUnit === "PACKAGE" ? "Số lượng sỉ" : "Số lượng lẻ"} (${intakeUnit})`
    : "Số lượng";
  const retailUnitLabel = baseUnitVal || "đơn vị lẻ";

  const handleTypeChange = (value) => {
    setItemType(value);
    // New shuttle ball: prefill the standard ống/quả config so the sỉ/lẻ
    // mapping is visible before saving.
    if (value === "SHUTTLE_BALL" && !matchedItem && !baseUnit.trim() && !packageUnit.trim()) {
      setBaseUnit(NEW_BALL_DEFAULTS.unit);
      setPackageUnit(NEW_BALL_DEFAULTS.packageUnit);
      setUnitsPerPackage(String(NEW_BALL_DEFAULTS.unitsPerPackage));
    }
  };

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError(null);
    const payload = {
      ...(matchedItem
        ? { itemId: matchedItem.itemId }
        : { itemName: name.trim(), itemType }),
      // item units: the same section as "Sửa mặt hàng", applied on intake
      ...(baseUnitVal ? { baseUnit: baseUnitVal } : {}),
      packageUnit: packageUnitVal,
      ...(packageUnitVal ? { unitsPerPackage: perPackage } : {}),
      purchaseDate,
      unit: effectiveUnit,
      quantity: qtyNum,
      unitCost: rawNumber(unitCost),
      ...(retailPrice !== "" ? { retailPrice: rawNumber(retailPrice) } : {}),
      ...(supplier.trim() ? { supplier: supplier.trim() } : {}),
      ...(note.trim() ? { note: note.trim() } : {}),
    };
    try {
      const res = await recordPurchase(payload);
      if (res?.data?.success) {
        onSaved?.();
        onClose();
      } else {
        setError(res?.data?.errorMessage || "Nhập sỉ thất bại.");
      }
    } catch (err) {
      console.error("Record purchase failed", err);
      setError("Có lỗi khi nhập sỉ.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={saving ? undefined : onClose}
      maxWidth="sm"
      fullWidth
      PaperComponent={DraggableResizablePaper}
    >
      <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
        Nhập sỉ
      </DialogTitle>
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
                      setBaseUnit(opt.unit || "");
                      setPackageUnit(opt.packageUnit || "");
                      setUnitsPerPackage(
                        opt.unitsPerPackage != null ? String(opt.unitsPerPackage) : ""
                      );
                      if (opt.retailPrice != null) {
                        setRetailPrice(formatVND(opt.retailPrice));
                      }
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

          <FormControl fullWidth>
            <InputLabel id="intake-item-type-label">Loại</InputLabel>
            <Select
              labelId="intake-item-type-label"
              label="Loại"
              value={itemType}
              onChange={(e) => handleTypeChange(e.target.value)}
            >
              <MenuItem value="SHUTTLE_BALL">{ITEM_TYPES.SHUTTLE_BALL}</MenuItem>
              <MenuItem value="GOODS">{ITEM_TYPES.GOODS}</MenuItem>
            </Select>
          </FormControl>

          <TextField
            label="Đơn vị lẻ"
            value={baseUnit}
            onChange={(e) => setBaseUnit(e.target.value)}
            placeholder="quả, chai..."
            helperText="Đơn vị nhỏ nhất để đếm kho và bán lẻ"
            fullWidth
          />
          <Stack direction="row" spacing={1}>
            <TextField
              label="Đơn vị sỉ"
              value={packageUnit}
              onChange={(e) => setPackageUnit(e.target.value)}
              placeholder="ống, thùng..."
              helperText="Bỏ trống nếu chỉ nhập theo đơn vị lẻ"
              fullWidth
            />
            <TextField
              label="Quy đổi sỉ sang lẻ"
              value={unitsPerPackage}
              onChange={(e) =>
                setUnitsPerPackage(
                  e.target.value === "" ? "" : String(rawNumber(e.target.value))
                )
              }
              disabled={!packageUnitVal}
              helperText={
                packageUnitVal
                  ? `1 ${packageUnitVal} = ? ${baseUnitVal || "đơn vị lẻ"}`
                  : " "
              }
              fullWidth
            />
          </Stack>

          {hasPackage && (
            <ToggleButtonGroup
              exclusive
              value={unit}
              onChange={(_, v) => v && setUnit(v)}
              fullWidth
            >
              <ToggleButton value="PACKAGE">
                Đơn vị sỉ: {packageUnitVal} (1 {packageUnitVal} = {perPackage}{" "}
                {baseUnitVal || "đơn vị lẻ"})
              </ToggleButton>
              <ToggleButton value="BASE">
                Đơn vị lẻ: {baseUnitVal || "đơn vị lẻ"}
              </ToggleButton>
            </ToggleButtonGroup>
          )}

          <Stack direction="row" spacing={1}>
            <TextField
              label={quantityLabel}
              value={quantity}
              onChange={(e) =>
                setQuantity(
                  e.target.value === "" ? "" : String(rawNumber(e.target.value))
                )
              }
              fullWidth
            />
            <TextField
              label={buyPriceLabel}
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
                    <InputAdornment position="end">
                      {intakeUnit ? `${VN_CURRENCY}/${intakeUnit}` : VN_CURRENCY}
                    </InputAdornment>
                  ),
                },
              }}
            />
          </Stack>

          <TextField
            label="Giá bán lẻ"
            value={retailPrice}
            onChange={(e) =>
              setRetailPrice(
                e.target.value === "" ? "" : formatVND(rawNumber(e.target.value))
              )
            }
            helperText={`Tính theo 1 ${retailUnitLabel}`}
            fullWidth
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">
                    {VN_CURRENCY}/{retailUnitLabel}
                  </InputAdornment>
                ),
              },
            }}
          />

          <DateInput
            label="Ngày nhập"
            value={purchaseDate}
            onChange={setPurchaseDate}
            fullWidth
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
              Quy đổi sỉ sang lẻ: {qtyNum} {packageUnitVal} = {baseQty}{" "}
              {baseUnitVal || "đơn vị lẻ"}
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
          {saving ? "Đang lưu..." : "Nhập sỉ"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StockIntakeDialog;
