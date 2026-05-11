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
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import api from "../api/index";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";

function SetupPage() {
  const [errorMess, setErrorMess] = useState(null);
  const [totalCourt, setTotalCourt] = useState(7);
  const [costInPerson, setCostInPerson] = useState();
  const [costInPersonFormatted, setcostInPersonFormatted] = useState("");
  const [costInPersonEditing, setCostInPersonEditing] = useState(false);
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

  const [selectedRow, setSelectedRow] = useState(null);
  const [deletingRow, setDeletingRow] = useState(false);
  const handleRowClick = (index) => {
    setSelectedRow(index === selectedRow ? null : index);
  };

  const handleDeleteService = (serviceToDelete, idx) => {
    try {
      setDeletingRow(true);
      setTableServices((prev) =>
        prev.map((item, i) =>
          i === idx ? { ...item, isDeleted: !item.isDeleted } : item
        )
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
        prev.map((item, i) =>
          i === idx ? { ...item, isDeleted: !item.isDeleted } : item
        )
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
        s.serviceName === newService.serviceName &&
        Number(s.cost) === Number(newService.cost)
      );
    });
  };
  const existedInTableShuttleBall = (newBall) => {
    return tableShuttleBalls.some((b) => {
      return (
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
      alert("Không thể lưu thiết lập. Kiểm tra lỗi trùng lặp.");
      return;
    }
    const rawNumber = Number(costInPerson.toString().replace(/\D/g, ""));
    const payload = {
      totalCourt: totalCourt,
      costInPerson: rawNumber,
      addedShuttleBalls: shuttleBall.map((b) => ({
        shuttleName: b.shuttleName,
        shuttleCost: b.cost,
      })),
      deletedShuttleBalls: tableShuttleBalls
        .filter((b) => b.isDeleted)
        .map(({ shuttleName, cost }) => ({
          shuttleName: shuttleName,
          shuttleCost: cost,
        })),
      addedServices: services,
      deletedServices: tableServices
        .filter((s) => s.isDeleted)
        .map(({ serviceName, cost }) => ({
          serviceName: serviceName,
          cost: parseFloat(cost),
        })),
    };

    try {
      const response = await api.post(`/api/updateSetupService`, payload, {});
      if (response.status === 200) {
        alert("Lưu thiết lập thành công!");
        setTableShuttleBalls([
          ...tableShuttleBalls.filter((ball) => !ball.isDeleted),
          ...shuttleBall,
        ]);

        setTableServices([
          ...tableServices.filter((ser) => !ser.isDeleted),
          ...services,
        ]);

        setShuttleBall([]);
        setOneBall(true);
        setServices([]);
        setCostInPersonEditing(false);
      } else {
        setErrorMess(`${response.data.message}`);
      }
    } catch (error) {
      alert("Có lỗi khi lưu thiết lập. Kiểm tra lỗi màu đỏ bên dưới.");
    }
  };

  const handleCostInPerson = (e) => {
    const raw = e.target.value.replace(/\D/g, "");
    setCostInPerson(raw);
    setcostInPersonFormatted(formatVND(raw));
  };
  const handleCostInput = (e, id) => {
    const raw = e.target.value.replace(/\D/g, "");
    handleBallChange(id, "cost", raw);
  };

  const fetchEntries = async () => {
    try {
      const res = await api.get(`/api/getSetupServices`);
      if (res.status === 200 && res.data !== "") {
        setTotalCourt(res.data.totalCourt);
        setCostInPerson(res.data.costInPerson);
        setcostInPersonFormatted(formatVND(res.data.costInPerson));
        setTableShuttleBalls(res.data.shuttleBalls);
        setTableServices(res.data.services);
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchEntries();

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

  const rowSx = (deleted, selected) => ({
    cursor: deleted ? "not-allowed" : "pointer",
    textDecoration: deleted ? "line-through" : "none",
    opacity: deleted ? 0.5 : 1,
    transition: "all 0.2s ease",
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
        <Grid size={{ xs: 12 }}>
          <TextField
            label="Tổng sân"
            type="number"
            defaultValue="8"
            onChange={(e) => setTotalCourt(e.target.value)}
            disabled
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

        <Grid size={{ xs: 12 }}>
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
            <TableContainer component={Paper} variant="outlined" ref={tableBallRef}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Loại cầu</TableCell>
                    <TableCell>Giá ({VN_CURRENCY}/trái)</TableCell>
                    <TableCell sx={{ width: 72 }}>Xoá</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableShuttleBalls.map((ball, idx) => (
                    <TableRow
                      key={idx}
                      hover={!ball.isDeleted}
                      selected={selectedBall === idx}
                      onClick={() => !ball.isDeleted && handleBallClick(idx)}
                      sx={rowSx(ball.isDeleted, selectedBall === idx)}
                    >
                      <TableCell>{ball.shuttleName}</TableCell>
                      <TableCell>{ball.costFormat}</TableCell>
                      <TableCell>
                        {(selectedBall === idx || ball.isDeleted) && (
                          <IconButton
                            size="small"
                            title={ball.isDeleted ? "Quay lại" : "Xoá"}
                            color={ball.isDeleted ? "primary" : "error"}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteBall(ball, idx);
                            }}
                            disabled={deletingBall}
                          >
                            {ball.isDeleted ? <RefreshOutlinedIcon /> : <DeleteOutlinedIcon />}
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
            <TableContainer component={Paper} variant="outlined" ref={tableRef}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Dịch vụ</TableCell>
                    <TableCell>Giá ({VN_CURRENCY})</TableCell>
                    <TableCell sx={{ width: 72 }}>Xoá </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableServices.map((ser, index) => (
                    <TableRow
                      key={index}
                      hover={!ser.isDeleted}
                      selected={selectedRow === index}
                      onClick={() => !ser.isDeleted && handleRowClick(index)}
                      sx={rowSx(ser.isDeleted, selectedRow === index)}
                    >
                      <TableCell>{ser.serviceName}</TableCell>
                      <TableCell>{ser.costFormat}</TableCell>
                      <TableCell>
                        {(selectedRow === index || ser.isDeleted) && (
                          <IconButton
                            size="small"
                            title={ser.isDeleted ? "Quay lại" : "Xoá"}
                            color={ser.isDeleted ? "primary" : "error"}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteService(ser, index);
                            }}
                            disabled={deletingRow}
                          >
                            {ser.isDeleted ? <RefreshOutlinedIcon /> : <DeleteOutlinedIcon />}
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
    </Box>
  );
}

export default SetupPage;
