import { useEffect, useState, useRef } from "react";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
import Chip from "@mui/material/Chip";
import Tooltip from "@mui/material/Tooltip";
import Alert from "@mui/material/Alert";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import api from "../api/index";
import { getBillConfig, updateBillConfig, testPrinter } from "../api/billApi";
import { emitApiError } from "../api/errorBus";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";

const EMPTY_BILL_CONFIG = {
  businessName: "",
  taxCode: "",
  address: "",
  phone: "",
  billPrefix: "BL",
  vatRate: 0,
  billFooter: "",
  printerMode: "BROWSER",
  printerIp: "",
  printerPort: 9100,
  paperWidth: 80,
  autoPrint: true,
  einvoiceEnabled: false,
  einvoiceSeries: "",
  einvoiceTemplate: "",
};

// ---------------------------------------------------------------------------
// Catalog rows: everything about quantity, Giá bán and availability is owned by
// the inventory page (Quản lý kho). These helpers only render what the backend
// enriched (stockOnHand, retailPrice, active, itemId).
// ---------------------------------------------------------------------------
const isRowInactive = (row) => row.active === false && !row.isRestored;
const isRowPendingDelete = (row) => Boolean(row.isDeleted);
const rowActionIcon = (row) =>
  row.isDeleted || isRowInactive(row) ? <RefreshOutlinedIcon /> : <DeleteOutlinedIcon />;
const rowActionTitle = (row) =>
  row.isDeleted ? "Quay lại" : isRowInactive(row) ? "Mở bán lại" : "Xoá";
const rowActionColor = (row) =>
  row.isDeleted || isRowInactive(row) ? "primary" : "error";

const StockCell = ({ row }) => {
  if (row.stockOnHand == null) {
    return (
      <TableCell align="right">
        <Typography variant="body2" color="text.secondary">
          —
        </Typography>
      </TableCell>
    );
  }
  const color =
    row.stockOnHand <= 0 ? "error.main" : row.lowStock ? "warning.main" : "text.primary";
  return (
    <TableCell align="right">
      <Tooltip title={row.stockBreakdown || "Số lượng còn lại trong Quản lý kho"}>
        <Typography variant="body2" fontWeight={600} sx={{ color }}>
          {row.stockOnHand} {row.unit || ""}
        </Typography>
      </Tooltip>
    </TableCell>
  );
};

const PriceCell = ({ row }) => (
  <TableCell align="right">
    {row.retailPrice != null ? (
      <Typography variant="body2">
        {row.retailPriceFormat || formatVND(row.retailPrice)}
      </Typography>
    ) : (
      <Tooltip title="Nhập Giá bán trong Quản lý kho để đồng bộ về đây">
        <Chip size="small" color="warning" variant="outlined" label="Thiếu giá bán" />
      </Tooltip>
    )}
  </TableCell>
);

const RowStatusChip = ({ row }) => {
  if (row.isDeleted || isRowInactive(row)) {
    return (
      <Chip size="small" label="Ngừng bán" sx={{ bgcolor: "grey.300", color: "text.primary" }} />
    );
  }
  if (row.itemId == null) {
    return <Chip size="small" color="info" variant="outlined" label="Chưa liên kết kho" />;
  }
  return <Chip size="small" color="success" variant="outlined" label="Đang bán" />;
};

const LOAD_FAILED_MESSAGE =
  "Không tải được thiết lập sân cầu. Dữ liệu đang hiển thị có thể chưa phải mới nhất.";
const SAVE_FAILED_MESSAGE =
  "Chưa lưu được thiết lập do hệ thống đang bận. Vui lòng thử lại sau vài giây.";

function SetupPage() {
  const [errorMess, setErrorMess] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [totalCourt, setTotalCourt] = useState(7);
  const [costInPerson, setCostInPerson] = useState();
  const [costInPersonFormatted, setcostInPersonFormatted] = useState("");
  const [costInPersonEditing, setCostInPersonEditing] = useState(false);
  const [rentByTime, setRentByTime] = useState();
  const [rentByTimeFormatted, setRentByTimeFormatted] = useState("");
  const [rentByTimeEditing, setRentByTimeEditing] = useState(false);
  const [shuttleBall, setShuttleBall] = useState([]);
  const [services, setServices] = useState([]);
  const [tableShuttleBalls, setTableShuttleBalls] = useState([]);
  const [tableServices, setTableServices] = useState([]);
  const tableRef = useRef(null);
  const tableBallRef = useRef(null);

  const handleAdd = () => {
    setServices([...services, { id: Date.now(), serviceName: "", cost: "" }]);
  };
  const handleRemove = (id) => {
    setServices(services.filter((service) => service.id !== id));
  };

  const [oneBall, setOneBall] = useState(true);
  const addShuttleBall = () => {
    if (oneBall === false) {
      setErrorMess("Chỉ được thêm 1 loại cầu trong 1 lần.");
      return;
    }
    setShuttleBall([
      ...shuttleBall,
      { id: Date.now(), shuttleName: "", cost: "", costFormat: "" },
    ]);
    setOneBall(false);
  };
  const removeShuttleBall = (id) => {
    setShuttleBall(shuttleBall.filter((ball) => ball.id !== id));
    setOneBall(true);
  };

  const handleBallChange = (id, field, value) => {
    setShuttleBall(
      shuttleBall.map((ball) => {
        if (ball.id === id) {
          if (field === "cost") {
            const raw = Number(value.toString().replace(/\D/g, ""));
            return {
              ...ball,
              cost: raw,
              costFormat: formatVND(raw),
            };
          }
          return { ...ball, [field]: value };
        }
        return ball;
      })
    );
  };
  const handleServiceChange = (id, field, value) => {
    setServices(
      services.map((service) => {
        if (service.id === id) {
          if (field === "cost") {
            const raw = Number(value.toString().replace(/\D/g, ""));
            return {
              ...service,
              cost: raw,
              costFormat: formatVND(raw),
            };
          }
          return { ...service, [field]: value };
        }
        return service;
      })
    );
  };
  const handleServiceCostChange = (e, id) => {
    const raw = e.target.value.replace(/\D/g, "");
    handleServiceChange(id, "cost", raw);
  };

  // Billing / VAT / printer config — saved via its own endpoint, not the
  // court-setup payload.
  const [billConfig, setBillConfig] = useState(EMPTY_BILL_CONFIG);
  const [savingBillConfig, setSavingBillConfig] = useState(false);
  const [testingPrinter, setTestingPrinter] = useState(false);

  const fetchBillConfig = async () => {
    try {
      const res = await getBillConfig();
      if (res?.data?.success && res.data.data) {
        setBillConfig({ ...EMPTY_BILL_CONFIG, ...res.data.data });
      }
    } catch (error) {
      console.error(error);
    }
  };

  const setBillField = (field, value) =>
    setBillConfig((prev) => ({ ...prev, [field]: value }));

  const handleSaveBillConfig = async () => {
    const vat = Number(billConfig.vatRate);
    if (isNaN(vat) || vat < 0 || vat >= 100) {
      emitApiError("Thuế suất VAT phải trong khoảng 0–99.");
      return;
    }
    try {
      setSavingBillConfig(true);
      const res = await updateBillConfig({
        businessName: billConfig.businessName,
        taxCode: billConfig.taxCode,
        address: billConfig.address,
        phone: billConfig.phone,
        billPrefix: billConfig.billPrefix,
        vatRate: vat,
        billFooter: billConfig.billFooter,
        printerMode: billConfig.printerMode,
        printerIp: billConfig.printerIp || null,
        printerPort: billConfig.printerPort ? Number(billConfig.printerPort) : null,
        paperWidth: billConfig.paperWidth ? Number(billConfig.paperWidth) : null,
        autoPrint: billConfig.autoPrint,
        einvoiceEnabled: billConfig.einvoiceEnabled,
        einvoiceSeries: billConfig.einvoiceSeries || null,
        einvoiceTemplate: billConfig.einvoiceTemplate || null,
      });
      if (res?.data?.success) {
        alert("Lưu cấu hình hoá đơn thành công!");
        setBillConfig({ ...EMPTY_BILL_CONFIG, ...res.data.data });
      } else if (res?.data) {
        emitApiError(res.data.errorMessage || "Lưu cấu hình hoá đơn thất bại.");
      }
    } catch (error) {
      // interceptor already surfaced the error
    } finally {
      setSavingBillConfig(false);
    }
  };

  // Probes the configured printer over its saved IP:port — uses the SAVED
  // server-side config, so save first after changing IP/port.
  const handleTestPrinter = async () => {
    try {
      setTestingPrinter(true);
      const res = await testPrinter();
      if (res?.data?.success) {
        alert("Kết nối máy in thành công!");
      } else if (res?.data) {
        emitApiError(res.data.errorMessage || "Không kết nối được máy in.");
      }
    } catch (error) {
      // interceptor already surfaced the error
    } finally {
      setTestingPrinter(false);
    }
  };

  const [selectedRow, setSelectedRow] = useState(null);
  const [deletingRow, setDeletingRow] = useState(false);
  const handleRowClick = (index) => {
    setSelectedRow(index === selectedRow ? null : index);
  };

  const handleDeleteService = (serviceToDelete, idx) => {
    try {
      setDeletingRow(true);
      setTableServices((prev) =>
        prev.map((item, i) => {
          if (i !== idx) return item;
          // A row that is already inactive on the server is restored through
          // the add list, an active one is queued for deletion.
          if (item.active === false) {
            return { ...item, isRestored: !item.isRestored, isDeleted: false };
          }
          return { ...item, isDeleted: !item.isDeleted };
        })
      );
      setSelectedRow(null);
    } catch (err) {
      console.error("Error deleting service:", err);
      setErrorMess(`Có lỗi khi xoá dich vụ:${serviceToDelete.serviceName}`);
    } finally {
      setDeletingRow(false);
    }
  };

  const [selectedBall, setSelectedBall] = useState(null);
  const [deletingBall, setDeletingBall] = useState(false);
  const handleBallClick = (index) => {
    setSelectedBall(index === selectedBall ? null : index);
  };

  const handleDeleteBall = (ball, idx) => {
    try {
      setDeletingBall(true);
      setTableShuttleBalls((prev) =>
        prev.map((item, i) => {
          if (i !== idx) return item;
          if (item.active === false) {
            return { ...item, isRestored: !item.isRestored, isDeleted: false };
          }
          return { ...item, isDeleted: !item.isDeleted };
        })
      );
      setSelectedBall(null);
    } catch (err) {
      console.error("Error deleting shuttle ball:", err);
      setErrorMess(`Có lỗi khi xoá loại cầu: ${ball.shuttleName}`);
    } finally {
      setDeletingBall(false);
    }
  };

  const existedInTableService = (newService) => {
    return tableServices.some((s) => {
      return (
        !s.isDeleted &&
        s.active !== false &&
        s.serviceName === newService.serviceName &&
        Number(s.cost) === Number(newService.cost)
      );
    });
  };
  const existedInTableShuttleBall = (newBall) => {
    return tableShuttleBalls.some((b) => {
      return (
        !b.isDeleted &&
        b.active !== false &&
        b.shuttleName === newBall.shuttleName &&
        Number(b.shuttleCost) === Number(newBall.shuttleCost)
      );
    });
  };

  const handleSave = async () => {
    setErrorMess(null);

    const duplicates = [];
    const duplicatesBall = [];
    services.forEach((newService) => {
      if (existedInTableService(newService)) {
        duplicates.push(newService.serviceName);
      }
    });
    shuttleBall.forEach((newBall) => {
      if (existedInTableShuttleBall(newBall)) {
        duplicatesBall.push(newBall.shuttleName);
      }
    });
    let isDuplicated = false;
    let mess = "";
    if (duplicates.length > 0) {
      mess = `Tên của Dịch vụ đang trùng lặp: ${duplicates.join(", ")}`;
      isDuplicated = true;
    }
    if (duplicatesBall.length > 0) {
      mess +=
        "\n." +
        `Tên quả cầu lông đang trùng lặp: ${duplicatesBall.join(", ")}`;
      isDuplicated = true;
    }
    if (isDuplicated) {
      setErrorMess(mess);
      emitApiError("Không thể lưu thiết lập. Kiểm tra lỗi trùng lặp.");
      return;
    }
    const rawNumber = Number(costInPerson.toString().replace(/\D/g, ""));
    const rawRentByTime = Number(rentByTime.toString().replace(/\D/g, ""));
    const restoredServices = tableServices.filter((s) => s.isRestored);
    const restoredBalls = tableShuttleBalls.filter((b) => b.isRestored);
    const payload = {
      totalCourt: totalCourt,
      costInPerson: rawNumber,
      rentByTime: rawRentByTime,
      addedShuttleBalls: [
        ...shuttleBall.map((b) => ({
          shuttleName: b.shuttleName,
          shuttleCost: b.cost,
        })),
        ...restoredBalls.map((b) => ({
          shuttleName: b.shuttleName,
          shuttleCost: Number(b.cost) || 0,
        })),
      ],
      deletedShuttleBalls: tableShuttleBalls
        .filter((b) => b.isDeleted)
        .map(({ shuttleName, cost }) => ({
          shuttleName: shuttleName,
          shuttleCost: cost,
        })),
      addedServices: [
        ...services,
        ...restoredServices.map((s) => ({
          serviceName: s.serviceName,
          cost: Number(s.cost) || 0,
        })),
      ],
      deletedServices: tableServices
        .filter((s) => s.isDeleted)
        .map(({ serviceName, cost }) => ({
          serviceName: serviceName,
          cost: parseFloat(cost),
        })),
    };

    try {
      const response = await api.post(`/api/updateSetupService`, payload, {});
      if (!response) {
        // 5xx: the error popup already explained it, keep the form usable
        setErrorMess(SAVE_FAILED_MESSAGE);
        return;
      }
      if (response.status === 200) {
        // Reload instead of merging locally: quantity, Giá bán and status are
        // now computed by the backend from inventory.
        await fetchEntries();
        setShuttleBall([]);
        setOneBall(true);
        setServices([]);
        setCostInPersonEditing(false);
        setRentByTimeEditing(false);
        alert("Lưu thiết lập thành công!");
      } else {
        setErrorMess(`${response.data.message}`);
      }
    } catch (error) {
      console.error(error);
      setErrorMess(SAVE_FAILED_MESSAGE);
    }
  };

  const handleCostInPerson = (e) => {
    const raw = e.target.value.replace(/\D/g, "");
    setCostInPerson(raw);
    setcostInPersonFormatted(formatVND(raw));
  };
  const handleRentByTime = (e) => {
    const raw = e.target.value.replace(/\D/g, "");
    setRentByTime(raw);
    setRentByTimeFormatted(formatVND(raw));
  };
  const handleCostInput = (e, id) => {
    const raw = e.target.value.replace(/\D/g, "");
    handleBallChange(id, "cost", raw);
  };

  const fetchEntries = async () => {
    try {
      const res = await api.get(`/api/getSetupServices?includeInactive=true`);
      if (!res) {
        // 5xx is already surfaced by the api interceptor; the tables below would
        // otherwise look empty for no visible reason.
        setLoadError(LOAD_FAILED_MESSAGE);
        return;
      }
      if (res.status === 200 && res.data !== "") {
        setTotalCourt(res.data.totalCourt);
        setCostInPerson(res.data.costInPerson);
        setcostInPersonFormatted(formatVND(res.data.costInPerson));
        setRentByTime(res.data.rentByTime);
        setRentByTimeFormatted(formatVND(res.data.rentByTime));
        setTableShuttleBalls(res.data.shuttleBalls);
        setTableServices(res.data.services);
      }
      setLoadError(null);
    } catch (error) {
      console.error(error);
      setLoadError(LOAD_FAILED_MESSAGE);
    }
  };

  useEffect(() => {
    fetchEntries();
    fetchBillConfig();

    const handleClickOutside = (event) => {
      if (tableRef.current && !tableRef.current.contains(event.target)) {
        setSelectedRow(null);
      }

      if (
        tableBallRef.current &&
        !tableBallRef.current.contains(event.target)
      ) {
        setSelectedBall(null);
      }
    };

    document.addEventListener("click", handleClickOutside, true);

    return () => {
      document.removeEventListener("click", handleClickOutside, true);
    };
  }, []);

  const rowSx = (deleted, selected, inactive) => ({
    cursor: deleted ? "not-allowed" : "pointer",
    textDecoration: deleted ? "line-through" : "none",
    opacity: deleted ? 0.5 : 1,
    transition: "all 0.2s ease",
    ...(inactive && !deleted ? { color: "text.secondary" } : {}),
    ...(selected ? { bgcolor: "action.selected" } : {}),
  });

  return (
    <Box sx={{ maxWidth: 1100, mx: "auto", p: { xs: 1, sm: 2 } }}>
      <Grid container spacing={2} alignItems="center" sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Typography variant="h4" component="h1">
            Trang thiết lâp sân cầu
          </Typography>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Button variant="contained" onClick={handleSave}>
            Lưu thiết lập
          </Button>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField
            label="Tổng sân"
            type="number"
            value={totalCourt}
            onChange={(e) => setTotalCourt(e.target.value)}
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">sân</InputAdornment>
                ),
              },
            }}
            sx={{ maxWidth: 280 }}
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField
            label="Tiền sân"
            type="text"
            value={costInPersonEditing ? costInPerson : costInPersonFormatted}
            onChange={costInPersonEditing ? handleCostInPerson : undefined}
            onClick={
              costInPersonEditing ? undefined : () => setCostInPersonEditing(true)
            }
            slotProps={{
              input: {
                readOnly: !costInPersonEditing,
                sx: !costInPersonEditing ? { cursor: "pointer" } : undefined,
                endAdornment: (
                  <InputAdornment position="end">
                    {VN_CURRENCY}/người
                  </InputAdornment>
                ),
              },
            }}
            sx={{ maxWidth: 360 }}
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField
            label="Thuê sân theo giờ"
            type="text"
            value={rentByTimeEditing ? rentByTime : rentByTimeFormatted}
            onChange={rentByTimeEditing ? handleRentByTime : undefined}
            onClick={
              rentByTimeEditing ? undefined : () => setRentByTimeEditing(true)
            }
            slotProps={{
              input: {
                readOnly: !rentByTimeEditing,
                sx: !rentByTimeEditing ? { cursor: "pointer" } : undefined,
                endAdornment: (
                  <InputAdornment position="end">
                    {VN_CURRENCY}/giờ
                  </InputAdornment>
                ),
              },
            }}
            sx={{ maxWidth: 360 }}
          />
        </Grid>

        {loadError && (
          <Grid size={{ xs: 12 }}>
            <Alert
              severity="warning"
              action={
                <Button color="inherit" size="small" onClick={fetchEntries}>
                  Thử lại
                </Button>
              }
            >
              {loadError} Bấm “Thử lại”.
            </Alert>
          </Grid>
        )}

        {errorMess && (
          <Grid size={{ xs: 12 }}>
            <Typography color="error">{errorMess}</Typography>
          </Grid>
        )}

        <Grid size={{ xs: 12 }}>
          <Button
            variant="contained"
            color="success"
            onClick={addShuttleBall}
            sx={{ mr: 1 }}
          >
            + Thêm loại cầu
          </Button>
        </Grid>

        {shuttleBall.map((ball) => (
          <Grid container spacing={2} alignItems="center" key={ball.id} sx={{ pl: 2 }}>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Typography variant="body2">Loại cầu:</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                size="small"
                placeholder="VinaStar"
                value={ball.shuttleName}
                onChange={(e) =>
                  handleBallChange(ball.id, "shuttleName", e.target.value)
                }
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                size="small"
                value={formatVND(ball.cost)}
                onChange={(e) => handleCostInput(e, ball.id)}
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        {VN_CURRENCY}/trái
                      </InputAdornment>
                    ),
                  },
                }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Button
                variant="contained"
                color="error"
                onClick={() => removeShuttleBall(ball.id)}
              >
                -
              </Button>
            </Grid>
          </Grid>
        ))}

        {tableShuttleBalls.length !== 0 && (
          <Grid size={{ xs: 12 }} md={8}>
            <Typography variant="h6" gutterBottom>
              Danh sách Loại cầu
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Số lượng, giá bán và trạng thái lấy từ Quản lý kho.
            </Typography>
            <TableContainer component={Paper} variant="outlined" ref={tableBallRef}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Loại cầu</TableCell>
                    <TableCell sx={{ width: 90 }}>Đơn vị</TableCell>
                    <TableCell sx={{ width: 120 }} align="right">
                      Còn lại
                    </TableCell>
                    <TableCell sx={{ width: 140 }} align="right">
                      Giá bán
                    </TableCell>
                    <TableCell sx={{ width: 160 }}>Trạng thái</TableCell>
                    <TableCell sx={{ width: 72 }}>Xoá</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableShuttleBalls.map((ball, idx) => (
                    <TableRow
                      key={idx}
                      hover={!isRowPendingDelete(ball)}
                      selected={selectedBall === idx}
                      onClick={() => !isRowPendingDelete(ball) && handleBallClick(idx)}
                      sx={rowSx(isRowPendingDelete(ball), selectedBall === idx, isRowInactive(ball))}
                    >
                      <TableCell>{ball.shuttleName}</TableCell>
                      <TableCell>{ball.unit || "—"}</TableCell>
                      <StockCell row={ball} />
                      <PriceCell row={ball} />
                      <TableCell>
                        <RowStatusChip row={ball} />
                      </TableCell>
                      <TableCell>
                        {(selectedBall === idx || ball.isDeleted || ball.isRestored) && (
                          <IconButton
                            size="small"
                            title={rowActionTitle(ball)}
                            color={rowActionColor(ball)}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteBall(ball, idx);
                            }}
                            disabled={deletingBall}
                          >
                            {rowActionIcon(ball)}
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Grid>
        )}

        <Grid size={{ xs: 12 }}>
          <Button variant="contained" color="success" onClick={handleAdd} sx={{ mr: 1 }}>
            + Thêm dich vụ
          </Button>
        </Grid>

        {services.map((service) => (
          <Grid container spacing={2} alignItems="center" key={service.id} sx={{ pl: 2 }}>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Typography variant="body2">Dịch vụ:</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                size="small"
                value={service.serviceName}
                onChange={(e) =>
                  handleServiceChange(service.id, "serviceName", e.target.value)
                }
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                size="small"
                value={formatVND(service.cost)}
                onChange={(e) => handleServiceCostChange(e, service.id)}
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">{VN_CURRENCY}</InputAdornment>
                    ),
                  },
                }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Button
                variant="contained"
                color="error"
                onClick={() => handleRemove(service.id)}
              >
                -
              </Button>
            </Grid>
          </Grid>
        ))}

        {tableServices.length !== 0 && (
          <Grid size={{ xs: 12 }} md={8}>
            <Typography variant="h6" gutterBottom>
              Danh sách Dịch vụ
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Số lượng, giá bán và trạng thái lấy từ Quản lý kho.
            </Typography>
            <TableContainer component={Paper} variant="outlined" ref={tableRef}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Dịch vụ</TableCell>
                    <TableCell sx={{ width: 90 }}>Đơn vị</TableCell>
                    <TableCell sx={{ width: 120 }} align="right">
                      Còn lại
                    </TableCell>
                    <TableCell sx={{ width: 140 }} align="right">
                      Giá bán
                    </TableCell>
                    <TableCell sx={{ width: 160 }}>Trạng thái</TableCell>
                    <TableCell sx={{ width: 72 }}>Xoá</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableServices.map((ser, index) => (
                    <TableRow
                      key={index}
                      hover={!isRowPendingDelete(ser)}
                      selected={selectedRow === index}
                      onClick={() => !isRowPendingDelete(ser) && handleRowClick(index)}
                      sx={rowSx(isRowPendingDelete(ser), selectedRow === index, isRowInactive(ser))}
                    >
                      <TableCell>{ser.serviceName}</TableCell>
                      <TableCell>{ser.unit || "—"}</TableCell>
                      <StockCell row={ser} />
                      <PriceCell row={ser} />
                      <TableCell>
                        <RowStatusChip row={ser} />
                      </TableCell>
                      <TableCell>
                        {(selectedRow === index || ser.isDeleted || ser.isRestored) && (
                          <IconButton
                            size="small"
                            title={rowActionTitle(ser)}
                            color={rowActionColor(ser)}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteService(ser, index);
                            }}
                            disabled={deletingRow}
                          >
                            {rowActionIcon(ser)}
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Grid>
        )}

        <Grid size={{ xs: 12 }}>
          <Button variant="contained" onClick={handleSave}>
            Lưu thiết lập
          </Button>
        </Grid>
      </Grid>

      {/* ── Billing / tax / printer configuration ── */}
      <Paper variant="outlined" sx={{ mt: 4, p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Hoá đơn &amp; Thuế
        </Typography>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              size="small"
              label="Tên đơn vị kinh doanh"
              value={billConfig.businessName}
              onChange={(e) => setBillField("businessName", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Mã số thuế"
              value={billConfig.taxCode}
              onChange={(e) => setBillField("taxCode", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Điện thoại"
              value={billConfig.phone}
              onChange={(e) => setBillField("phone", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              size="small"
              label="Địa chỉ"
              value={billConfig.address}
              onChange={(e) => setBillField("address", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Tiền tố số HĐ"
              value={billConfig.billPrefix}
              onChange={(e) => setBillField("billPrefix", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="number"
              label="Thuế suất VAT (%)"
              value={billConfig.vatRate}
              onChange={(e) => setBillField("vatRate", e.target.value)}
              helperText="Giá bán đã bao gồm VAT"
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField
              fullWidth
              size="small"
              multiline
              minRows={2}
              label="Nội dung cuối hoá đơn"
              value={billConfig.billFooter}
              onChange={(e) => setBillField("billFooter", e.target.value)}
            />
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Typography variant="subtitle2" color="text.secondary">
              Máy in hoá đơn
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              select
              label="Kiểu in"
              value={billConfig.printerMode}
              onChange={(e) => setBillField("printerMode", e.target.value)}
            >
              <MenuItem value="BROWSER">In qua trình duyệt (USB)</MenuItem>
              <MenuItem value="NETWORK">In mạng (ESC/POS)</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="IP máy in"
              value={billConfig.printerIp}
              onChange={(e) => setBillField("printerIp", e.target.value)}
              disabled={billConfig.printerMode !== "NETWORK"}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="number"
              label="Cổng máy in"
              value={billConfig.printerPort}
              onChange={(e) => setBillField("printerPort", e.target.value)}
              disabled={billConfig.printerMode !== "NETWORK"}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="number"
              label="Khổ giấy (mm)"
              value={billConfig.paperWidth}
              onChange={(e) => setBillField("paperWidth", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={!!billConfig.autoPrint}
                  onChange={(e) => setBillField("autoPrint", e.target.checked)}
                />
              }
              label="Mở in sau thanh toán"
            />
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Typography variant="subtitle2" color="text.secondary">
              Hoá đơn điện tử (MISA meInvoice)
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={!!billConfig.einvoiceEnabled}
                  onChange={(e) => setBillField("einvoiceEnabled", e.target.checked)}
                />
              }
              label="Bật HĐĐT"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Ký hiệu HĐĐT"
              value={billConfig.einvoiceSeries}
              onChange={(e) => setBillField("einvoiceSeries", e.target.value)}
              disabled={!billConfig.einvoiceEnabled}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              size="small"
              label="Mẫu số HĐĐT"
              value={billConfig.einvoiceTemplate}
              onChange={(e) => setBillField("einvoiceTemplate", e.target.value)}
              disabled={!billConfig.einvoiceEnabled}
            />
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Button
              variant="contained"
              color="success"
              onClick={handleSaveBillConfig}
              disabled={savingBillConfig}
            >
              Lưu cấu hình hoá đơn
            </Button>
            {billConfig.printerMode === "NETWORK" && (
              <Button
                variant="outlined"
                sx={{ ml: 1 }}
                onClick={handleTestPrinter}
                disabled={testingPrinter}
              >
                Kiểm tra máy in
              </Button>
            )}
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
}

export default SetupPage;
