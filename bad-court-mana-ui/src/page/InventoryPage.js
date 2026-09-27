import { useEffect, useState, useCallback } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import InputAdornment from "@mui/material/InputAdornment";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import ToggleButton from "@mui/material/ToggleButton";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Chip from "@mui/material/Chip";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import CircularProgress from "@mui/material/CircularProgress";
import Tooltip from "@mui/material/Tooltip";
import DownloadIcon from "@mui/icons-material/Download";
import UploadIcon from "@mui/icons-material/Upload";
import AddShoppingCartIcon from "@mui/icons-material/AddShoppingCart";
import AddIcon from "@mui/icons-material/Add";
import HistoryIcon from "@mui/icons-material/History";
import FactCheckIcon from "@mui/icons-material/FactCheck";
import EditIcon from "@mui/icons-material/Edit";
import SearchIcon from "@mui/icons-material/Search";
import FastfoodIcon from "@mui/icons-material/Fastfood";
import InventoryItemDialog, { ITEM_TYPES } from "./dialog/InventoryItemDialog";
import PurchaseDialog from "./dialog/PurchaseDialog";
import StockIntakeDialog from "./dialog/StockIntakeDialog";
import StockLedgerDialog from "./dialog/StockLedgerDialog";
import StockAdjustDialog from "./dialog/StockAdjustDialog";
import { listInventoryItems, exportInventory } from "../api/inventoryApi";
import { formatVND } from "./MoneyUtils";
import { normalizeSearchText } from "./TextUtils";

// No badminton icon exists in @mui/icons-material - use the shuttlecock emoji.
const TAB_TYPES = {
  SHUTTLE_BALL: { label: ITEM_TYPES.SHUTTLE_BALL, icon: "🏸" },
  GOODS: { label: ITEM_TYPES.GOODS, icon: <FastfoodIcon fontSize="small" /> },
};
const TAB_KEYS = Object.keys(TAB_TYPES);

export default function InventoryPage() {
  const [itemsByType, setItemsByType] = useState({});
  const [activeType, setActiveType] = useState("SHUTTLE_BALL");
  const [searchByTab, setSearchByTab] = useState({ SHUTTLE_BALL: "", GOODS: "" });
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [dialog, setDialog] = useState({ type: null, item: null });

  const showToast = useCallback((message, severity = "success") => {
    setToast({ message, severity });
  }, []);

  const fetchItems = useCallback(
    async (itemType) => {
      setLoading(true);
      try {
        const res = await listInventoryItems(itemType);
        const body = res?.data;
        if (body?.success && Array.isArray(body.data)) {
          setItemsByType((prev) => ({ ...prev, [itemType]: body.data }));
        } else {
          showToast(body?.errorMessage || "Không tải được kho hàng.", "error");
        }
      } catch (err) {
        console.error("Failed to load inventory", err);
        showToast("Không tải được kho hàng. Vui lòng thử lại.", "error");
      } finally {
        setLoading(false);
      }
    },
    [showToast]
  );

  useEffect(() => {
    if (itemsByType[activeType] === undefined) {
      fetchItems(activeType);
    }
  }, [activeType, itemsByType, fetchItems]);

  const closeDialog = () => setDialog({ type: null, item: null });
  const onSaved = () => {
    showToast("Lưu thành công!");
    // A mutation can create items of the other type too (e.g. purchase
    // with a new item name) - drop the cache so both tabs refetch lazily.
    setItemsByType({});
  };

  const handleExport = async () => {
    try {
      await exportInventory();
      showToast("Xuất báo cáo kho thành công!");
    } catch (err) {
      console.error("Inventory export failed", err);
      showToast("Xuất báo cáo thất bại.", "error");
    }
  };

  const items = itemsByType[activeType] || [];
  const searchQuery = searchByTab[activeType] || "";
  const visibleItems = items.filter((i) =>
    normalizeSearchText(i.itemName).includes(normalizeSearchText(searchQuery))
  );

  return (
    <Box sx={{ maxWidth: 1200, mx: "auto", p: { xs: 1, sm: 2 } }}>
      <Stack
        direction="row"
        spacing={1}
        flexWrap="wrap"
        alignItems="center"
        sx={{ mb: 2, gap: 1 }}
      >
        <Typography variant="h4" component="h1" sx={{ flexGrow: 1 }}>
          Quản lý kho
        </Typography>
        <Button
          variant="outlined"
          startIcon={<AddIcon />}
          onClick={() => setDialog({ type: "item" })}
          disabled={loading}
        >
          Thêm mặt hàng
        </Button>
        <Button
          variant="outlined"
          startIcon={<AddShoppingCartIcon />}
          onClick={() => setDialog({ type: "purchase" })}
          disabled={loading}
        >
          Nhập kho
        </Button>
        <Button
          variant="outlined"
          startIcon={<UploadIcon />}
          onClick={() => setDialog({ type: "intake" })}
          disabled={loading}
        >
          Nhập file
        </Button>
        <Button
          variant="outlined"
          startIcon={<DownloadIcon />}
          onClick={handleExport}
          disabled={loading}
        >
          Xuất báo cáo
        </Button>
      </Stack>

      <Stack
        direction="row"
        spacing={1}
        alignItems="center"
        flexWrap="wrap"
        sx={{ mb: 2, gap: 1 }}
      >
        <ToggleButtonGroup
          size="small"
          exclusive
          value={activeType}
          onChange={(_, v) => v && setActiveType(v)}
          sx={{
            p: 0.5,
            gap: 0.5,
            bgcolor: "grey.100",
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 2,
            "& .MuiToggleButtonGroup-grouped": {
              border: 0,
              borderRadius: "8px !important",
              px: 2,
              py: 0.75,
              gap: 0.75,
              textTransform: "none",
              fontWeight: 600,
              color: "text.secondary",
              "&:not(.Mui-selected):hover": {
                bgcolor: "action.hover",
              },
              "&.Mui-selected": {
                bgcolor: "primary.main",
                color: "primary.contrastText",
                boxShadow: 1,
                "&:hover": { bgcolor: "primary.dark" },
              },
            },
          }}
        >
          {TAB_KEYS.map((t) => (
            <ToggleButton key={t} value={t}>
              {TAB_TYPES[t].icon}
              {TAB_TYPES[t].label}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
        <TextField
          size="small"
          placeholder={`Tìm ${TAB_TYPES[activeType].label.toLowerCase()}...`}
          value={searchQuery}
          onChange={(e) =>
            setSearchByTab((prev) => ({ ...prev, [activeType]: e.target.value }))
          }
          sx={{ ml: "auto", minWidth: 220 }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
      </Stack>

      {loading && items.length === 0 ? (
        <Box sx={{ textAlign: "center", py: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ width: 50 }}>#</TableCell>
                <TableCell>Tên mặt hàng</TableCell>
                <TableCell>Thời gian nhập hàng</TableCell>
                {activeType === "SHUTTLE_BALL" ? (
                  <>
                    <TableCell align="right">Nhập (ống)</TableCell>
                    <TableCell align="right">#</TableCell>
                  </>
                ) : (
                  <TableCell align="right">Nhập</TableCell>
                )}
                <TableCell align="right">Còn lại</TableCell>
                <TableCell align="right">Giá bán</TableCell>
                <TableCell sx={{ width: 130 }}></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibleItems.map((item, idx) => {
                const outOfStock = item.outOfStock ?? item.stockOnHand <= 0;
                return (
                  <TableRow
                    key={item.itemId}
                    sx={
                      outOfStock
                        ? { bgcolor: "error.50" }
                        : item.lowStock
                          ? { bgcolor: "warning.50" }
                          : undefined
                    }
                  >
                    <TableCell>{idx + 1}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="body2" fontWeight={600}>
                          {item.itemName}
                        </Typography>
                        {outOfStock ? (
                          <Chip
                            size="small"
                            label="Hết hàng"
                            sx={{ bgcolor: "grey.300", color: "text.primary" }}
                          />
                        ) : (
                          item.lowStock && (
                            <Chip
                              size="small"
                              color="warning"
                              variant="outlined"
                              label="Sắp hết"
                              sx={{ color: "text.primary" }}
                            />
                          )
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell>
                      {item.lastPurchaseDate
                        ? item.lastPurchaseDate.split("-").reverse().join("/")
                        : "-"}
                    </TableCell>
                    {activeType === "SHUTTLE_BALL" && (
                      <TableCell align="right" sx={{ color: "text.secondary" }}>
                        {item.purchasedBreakdown || "-"}
                      </TableCell>
                    )}
                    <TableCell align="right" sx={{ fontWeight: 700 }}>
                      {item.purchasedQuantity ?? item.stockOnHand} {item.unit}
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700 }}>
                      {item.stockOnHand} {item.unit}
                    </TableCell>
                    <TableCell align="right">
                      {item.retailPrice != null ? formatVND(item.retailPrice) : "-"} / {item.unit}
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Lịch sử kho">
                        <IconButton
                          size="small"
                          color="secondary"
                          onClick={() => setDialog({ type: "ledger", item })}
                        >
                          <HistoryIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Kiểm kê">
                        <IconButton
                          size="small"
                          sx={{ color: "#0f805f" }}
                          onClick={() => setDialog({ type: "adjust", item })}
                        >
                          <FactCheckIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Sửa">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => setDialog({ type: "item", item })}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })}
              {visibleItems.length === 0 && (
                <TableRow>
                  <TableCell
                    colSpan={activeType === "SHUTTLE_BALL" ? 8 : 7}
                    align="center"
                    sx={{ py: 4 }}
                  >
                    <Typography color="text.secondary">
                      {searchQuery
                        ? `Không tìm thấy "${searchQuery}".`
                        : `Chưa có ${TAB_TYPES[activeType].label.toLowerCase()} nào. Nhấn "Thêm mặt hàng" hoặc "Nhập file" để bắt đầu.`}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <InventoryItemDialog
        open={dialog.type === "item"}
        item={dialog.item}
        onClose={closeDialog}
        onSaved={onSaved}
      />
      <PurchaseDialog
        open={dialog.type === "purchase"}
        items={items}
        onClose={closeDialog}
        onSaved={onSaved}
      />
      <StockIntakeDialog
        open={dialog.type === "intake"}
        onClose={closeDialog}
        onSuccess={onSaved}
      />
      <StockLedgerDialog
        open={dialog.type === "ledger"}
        item={dialog.item}
        onClose={closeDialog}
      />
      <StockAdjustDialog
        open={dialog.type === "adjust"}
        item={dialog.item}
        onClose={closeDialog}
        onSaved={onSaved}
      />

      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert severity={toast?.severity || "success"} onClose={() => setToast(null)}>
          {toast?.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
