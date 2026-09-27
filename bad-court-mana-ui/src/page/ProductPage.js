import { useEffect, useState, useCallback } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Tabs from "@mui/material/Tabs";
import Tab from "@mui/material/Tab";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Stack from "@mui/material/Stack";
import Chip from "@mui/material/Chip";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import DeleteIcon from "@mui/icons-material/Delete";
import RefreshIcon from "@mui/icons-material/Refresh";
import DownloadIcon from "@mui/icons-material/Download";
import UploadIcon from "@mui/icons-material/Upload";
import DescriptionIcon from "@mui/icons-material/Description";
import SearchIcon from "@mui/icons-material/Search";
import ProductImportDialog from "./dialog/ProductImportDialog";
import {
  getSetupServices,
  updateSetupService,
  exportProducts,
  downloadProductTemplate,
} from "../api/productApi";
import { VN_CURRENCY, formatVND, rawNumber } from "./MoneyUtils";
import { normalizeSearchText } from "./TextUtils";

const TABS = [
  { key: "shuttle", label: "Loại cầu", unit: "/trái", nameField: "shuttleName" },
  { key: "service", label: "Đồ uống / Đồ ăn", unit: "", nameField: "serviceName" },
];

const normalizeProducts = (list, type) =>
  (list || []).map((item, idx) => {
    const name = type === "shuttle" ? item.shuttleName : item.serviceName;
    const cost =
      Number(type === "shuttle" ? item.shuttleCost ?? item.cost : item.cost) || 0;
    return {
      id: `${type}-${idx}`,
      name,
      originalName: name,
      cost,
      originalCost: cost,
      costFormat: formatVND(cost),
      isActive: item.isActive !== false,
      isDeleted: false,
      isNew: false,
    };
  });

const buildAddedDeleted = (products, type) => {
  const added = [];
  const deleted = [];

  for (const p of products) {
    if (!p.name) continue;
    const base =
      type === "shuttle"
        ? { shuttleName: p.name.trim() }
        : { serviceName: p.name.trim() };

    if (p.isNew && !p.isDeleted && p.cost > 0) {
      added.push({
        ...base,
        ...(type === "shuttle" ? { shuttleCost: p.cost } : { cost: p.cost }),
      });
      continue;
    }

    if (!p.isNew && p.isDeleted) {
      // Delete the originally-loaded product identity.
      deleted.push({
        ...base,
        ...(type === "shuttle"
          ? { shuttleCost: p.originalCost }
          : { cost: p.originalCost }),
      });
      continue;
    }

    if (!p.isNew && !p.isDeleted && p.cost !== p.originalCost) {
      // Cost change = delete old + add new with the same name.
      deleted.push({
        ...base,
        ...(type === "shuttle"
          ? { shuttleCost: p.originalCost }
          : { cost: p.originalCost }),
      });
      added.push({
        ...base,
        ...(type === "shuttle" ? { shuttleCost: p.cost } : { cost: p.cost }),
      });
    }
  }

  return { added, deleted };
};

export default function ProductPage() {
  const [activeTab, setActiveTab] = useState(0);
  const [searchByTab, setSearchByTab] = useState({ shuttle: "", service: "" });
  const [settings, setSettings] = useState(null);
  const [shuttleBalls, setShuttleBalls] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, severity = "success") => {
    setToast({ message, severity });
  }, []);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getSetupServices();
      const data = res?.data || {};
      setSettings(data);
      setShuttleBalls(normalizeProducts(data.shuttleBalls, "shuttle"));
      setServices(normalizeProducts(data.services, "service"));
    } catch (err) {
      console.error("Failed to load products", err);
      showToast("Không tải được danh sách hàng. Vui lòng thử lại.", "error");
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const currentProducts = activeTab === 0 ? shuttleBalls : services;
  const currentTab = TABS[activeTab];
  const setCurrentProducts = activeTab === 0 ? setShuttleBalls : setServices;

  const handleNameChange = (id, value) => {
    setCurrentProducts((prev) =>
      prev.map((p) => (p.id === id ? { ...p, name: value } : p))
    );
  };

  const handleCostChange = (id, raw) => {
    const cost = rawNumber(raw);
    setCurrentProducts((prev) =>
      prev.map((p) =>
        p.id === id
          ? { ...p, cost, costFormat: formatVND(cost) }
          : p
      )
    );
  };

  const toggleDelete = (id) => {
    setCurrentProducts((prev) =>
      prev.map((p) =>
        p.id === id ? { ...p, isDeleted: !p.isDeleted } : p
      )
    );
  };

  const handleAdd = () => {
    setCurrentProducts((prev) => [
      ...prev,
      {
        id: `new-${Date.now()}`,
        name: "",
        originalName: "",
        cost: 0,
        originalCost: 0,
        costFormat: "",
        isActive: true,
        isDeleted: false,
        isNew: true,
      },
    ]);
  };

  const handleSave = async () => {
    if (!settings) return;
    setSaving(true);

    const shuttle = buildAddedDeleted(shuttleBalls, "shuttle");
    const service = buildAddedDeleted(services, "service");

    const payload = {
      totalCourt: settings.totalCourt ?? 0,
      costInPerson: settings.costInPerson ?? 0,
      rentByTime: settings.rentByTime ?? 0,
      addedShuttleBalls: shuttle.added,
      deletedShuttleBalls: shuttle.deleted,
      addedServices: service.added,
      deletedServices: service.deleted,
    };

    try {
      const res = await updateSetupService(payload);
      if (res?.status === 200) {
        showToast("Lưu thay đổi thành công!");
        await fetchProducts();
      } else {
        showToast("Lưu thất bại. Vui lòng kiểm tra dữ liệu.", "error");
      }
    } catch (err) {
      console.error("Save products failed", err);
      showToast("Có lỗi khi lưu thay đổi.", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleExport = async () => {
    try {
      await exportProducts();
      showToast("Xuất file thành công!");
    } catch (err) {
      console.error("Export failed", err);
      showToast("Xuất file thất bại.", "error");
    }
  };

  const handleTemplate = async () => {
    try {
      await downloadProductTemplate();
      showToast("Tải mẫu thành công!");
    } catch (err) {
      console.error("Template download failed", err);
      showToast("Tải mẫu thất bại.", "error");
    }
  };

  const hasChanges = [shuttleBalls, services].some((list) =>
    list.some(
      (p) =>
        (p.isNew && !p.isDeleted && p.name && p.cost > 0) ||
        (!p.isNew && p.isDeleted) ||
        (!p.isNew && !p.isDeleted && p.cost !== p.originalCost)
    )
  );

  const searchQuery = searchByTab[currentTab.key] || "";

  // Show non-deleted rows + existing rows marked for deletion.
  // New rows deleted before save are hidden.
  const visibleProducts = currentProducts.filter(
    (p) =>
      (!p.isDeleted || !p.isNew) &&
      normalizeSearchText(p.name).includes(normalizeSearchText(searchQuery))
  );
  const deletedCount = currentProducts.filter(
    (p) => p.isDeleted && !p.isNew
  ).length;

  return (
    <Box sx={{ maxWidth: 1100, mx: "auto", p: { xs: 1, sm: 2 } }}>
      <Stack
        direction="row"
        spacing={1}
        flexWrap="wrap"
        alignItems="center"
        sx={{ mb: 2, gap: 1 }}
      >
        <Typography variant="h4" component="h1" sx={{ flexGrow: 1 }}>
          Quản lý hàng
        </Typography>
        <Button
          variant="outlined"
          startIcon={<DescriptionIcon />}
          onClick={handleTemplate}
          disabled={loading}
        >
          Tải mẫu
        </Button>
        <Button
          variant="outlined"
          startIcon={<UploadIcon />}
          onClick={() => setImportOpen(true)}
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
          Xuất file
        </Button>
        <Button variant="contained" onClick={handleSave} disabled={!hasChanges || saving}>
          {saving ? "Đang lưu..." : "Lưu thay đổi"}
        </Button>
      </Stack>

      <Tabs
        value={activeTab}
        onChange={(_, v) => setActiveTab(v)}
        sx={{ mb: 2 }}
      >
        {TABS.map((t) => (
          <Tab key={t.key} label={t.label} />
        ))}
      </Tabs>

      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
        <Button variant="contained" color="success" onClick={handleAdd}>
          + Thêm {activeTab === 0 ? "loại cầu" : "dịch vụ"}
        </Button>
        <TextField
          size="small"
          placeholder={`Tìm ${activeTab === 0 ? "loại cầu" : "dịch vụ"}...`}
          value={searchQuery}
          onChange={(e) =>
            setSearchByTab((prev) => ({ ...prev, [currentTab.key]: e.target.value }))
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
        {deletedCount > 0 && (
          <Chip
            size="small"
            color="error"
            label={`${deletedCount} sản phẩm đang chờ xoá (Lưu để áp dụng)`}
          />
        )}
      </Stack>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 50 }}>#</TableCell>
              <TableCell>Tên</TableCell>
              <TableCell>Giá ({VN_CURRENCY}{currentTab.unit})</TableCell>
              <TableCell sx={{ width: 80 }}>Trạng thái</TableCell>
              <TableCell sx={{ width: 80 }}></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleProducts.map((p, idx) => (
              <TableRow
                key={p.id}
                sx={{
                  opacity: p.isDeleted ? 0.5 : 1,
                  textDecoration: p.isDeleted ? "line-through" : "none",
                  bgcolor: p.isNew ? "success.50" : "inherit",
                }}
              >
                <TableCell>{idx + 1}</TableCell>
                <TableCell>
                  <TextField
                    size="small"
                    value={p.name}
                    onChange={(e) => handleNameChange(p.id, e.target.value)}
                    disabled={p.isDeleted || (!p.isNew && !p.isDeleted)}
                    fullWidth
                    placeholder={activeTab === 0 ? "VinaStar" : "Coca"}
                  />
                </TableCell>
                <TableCell>
                  <TextField
                    size="small"
                    value={p.costFormat}
                    onChange={(e) => handleCostChange(p.id, e.target.value)}
                    disabled={p.isDeleted}
                    fullWidth
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                        ),
                      },
                    }}
                  />
                </TableCell>
                <TableCell>
                  {p.isDeleted ? (
                    <Chip size="small" color="error" label="Xoá" />
                  ) : p.isNew ? (
                    <Chip size="small" color="success" label="Mới" />
                  ) : (
                    <Chip size="small" color="success" label="Đang bán" />
                  )}
                </TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    color={p.isDeleted ? "primary" : "error"}
                    onClick={() => toggleDelete(p.id)}
                    title={p.isDeleted ? "Khôi phục" : "Xoá"}
                  >
                    {p.isDeleted ? <RefreshIcon /> : <DeleteIcon />}
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {visibleProducts.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    Chưa có {activeTab === 0 ? "loại cầu" : "dịch vụ"} nào.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <ProductImportDialog
        open={importOpen}
        onClose={() => setImportOpen(false)}
        onSuccess={() => {
          showToast("Nhập hàng thành công!");
          fetchProducts();
        }}
      />

      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          severity={toast?.severity || "success"}
          onClose={() => setToast(null)}
        >
          {toast?.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
