import React, { useState, useEffect } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import api, { backendHost } from "../api/index";

export default function ReportPage() {
  const [reports, setReports] = useState([]);
  const [originalReports, setOriginalReports] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [numberRows, setNumberRows] = useState(0);
  const [totalRows, setTotalRows] = useState(0);
  const [loading, setLoading] = useState(false);

  const [sortField, setSortField] = useState("date");
  const [sortDir, setSortDir] = useState("ASC");
  const [month, setMonth] = useState("");
  const [monthOptions, setMonthOptions] = useState([]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setCurrentPage(1);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchText]);

  useEffect(() => {
    fetchReports({
      pagination: {
        current: 0,
        pageSize: 10,
      },
    });

    api
      .get(`/api/v1/manager/getMonthYear`)
      .then((res) => {
        setMonthOptions(res.data);
      })
      .catch((exception) => {
        console.log(`Cannot get month year: ${exception.error}`);
      });
  }, []);

  const fetchReports = async (payload) => {
    try {
      setLoading(true);

      const response = await api.post("/api/v1/manager/reportList", payload);
      const pageResponse = response.data.data;
      const pagination = pageResponse.pagination;
      const list = pageResponse.list;

      setPageSize(pagination.pageSize);
      setTotalPages(pagination.totalPage);

      setReports(list);
      setOriginalReports(list);
      setNumberRows(list.length);
      setTotalRows(pageResponse.total);
    } catch (error) {
      console.error("Failed to fetch reports", error);
    } finally {
      setLoading(false);
    }
  };

  const toggleSort = (field) => {
    if (sortField === field) setSortDir(sortDir === "ASC" ? "DESC" : "ASC");
    else {
      setSortField(field);
      setSortDir("ASC");
    }
  };

  const handlePageSizeChange = (size) => {
    fetchReports({
      pagination: {
        current: 0,
        pageSize: size,
      },
    });
    setPageSize(size);
    setCurrentPage(1);
  };

  const handlePageChange = (_, page) => {
    fetchReports({
      pagination: {
        current: page - 1,
        pageSize: pageSize,
      },
    });
    setCurrentPage(page);
  };

  const handleExport = async (sessionId) => {
    try {
      const response = await api.get(
        `/api/v1/manager/reportExport/${sessionId}`,
        { responseType: "blob" }
      );

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const disposition = response.headers["content-disposition"] || "";
      const match = disposition.match(/filename="?([^"]+)"?/);
      const fileName = match ? match[1] : `report_${sessionId}.xlsx`;

      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();

      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Failed to export report", error);
      alert("Export failed. Please try again.");
    }
  };

  const handleExportReport = async () => {
    try {
      const sessionIds = reports.map((r) => r.sessionId);

      const response = await api.post(`/api/v1/manager/reportToken`, {
        sessionIds: sessionIds,
      });

      window.location.href = `${backendHost}/api/v1/manager/stream/reportExportList/${response.data.reportToken}`;
    } catch (error) {
      console.error("Failed to export report", error);
      alert("Export failed. Please try again.");
    }
  };

  const handleMonthChange = (newMonth) => {
    fetchReports({
      yearMonth: newMonth,
      pagination: {
        current: currentPage - 1,
        pageSize: pageSize,
      },
    });
    setMonth(newMonth);
  };

  const handleFilter = (text) => {
    setSearchText(text);
    const keyword = text.trim().toLowerCase();

    if (!keyword) {
      setReports(originalReports);
      setNumberRows(originalReports.length);
      return;
    }

    const filtered = originalReports.filter((row, index) => {
      const searchString = `
      ${(currentPage - 1) * pageSize + index + 1}
      ${row.date?.viDateString ?? ""}
      ${row.during ?? ""}
      ${row.grossRevenueFormat ?? ""}
    `.toLowerCase();

      return searchString.includes(keyword);
    });

    setReports(filtered);
    setNumberRows(filtered.length);
  };

  const sortIndicator = (field) =>
    sortField === field ? (sortDir === "ASC" ? " ▲" : " ▼") : "";

  return (
    <Box sx={{ mt: 2, px: { xs: 1, sm: 2 }, maxWidth: 1400, mx: "auto" }}>
      <Typography variant="h5" gutterBottom>
        Trang quản lý
      </Typography>

      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={2}
        sx={{ mb: 3, alignItems: { md: "center" }, flexWrap: "wrap" }}
      >
        <TextField
          size="small"
          placeholder="Tìm kiếm bằng ngày, thời gian, số tổng,..."
          value={searchText}
          onChange={(e) => handleFilter(e.target.value)}
          sx={{ minWidth: { md: 260 }, flex: { md: "1 1 260px" } }}
        />
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="month-filter-label">Tháng</InputLabel>
          <Select
            labelId="month-filter-label"
            label="Tháng"
            value={month}
            onChange={(e) => handleMonthChange(e.target.value)}
          >
            {monthOptions.map((m) => (
              <MenuItem key={m.value} value={m.value}>
                {m.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Typography variant="body2" color="text.secondary">
          Tổng hiển thị: <strong>{numberRows}</strong>
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        <Button
          variant="contained"
          color="success"
          size="small"
          onClick={() => handleExportReport()}
        >
          Xuất Excel tất cả hàng hiển thị
        </Button>
      </Stack>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "grey.100" }}>
              <TableCell sx={{ width: 60 }}>STT</TableCell>
              <TableCell
                onClick={() => toggleSort("date")}
                sx={{ cursor: "pointer" }}
              >
                Ngày{sortIndicator("date")}
              </TableCell>
              <TableCell
                onClick={() => toggleSort("fromTo")}
                sx={{ cursor: "pointer" }}
              >
                Khoảng thời gian{sortIndicator("fromTo")}
              </TableCell>
              <TableCell
                onClick={() => toggleSort("total")}
                sx={{ cursor: "pointer" }}
              >
                Tồng tiền đã thanh toán{sortIndicator("total")}
              </TableCell>
              <TableCell sx={{ width: 120 }} align="center">
                Xuất
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  Loading...
                </TableCell>
              </TableRow>
            )}
            {!loading && reports.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ color: "text.secondary" }}>
                  No data found
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              reports.map((row, index) => (
                <TableRow key={row.id} hover>
                  <TableCell title={`index:${index}`}>
                    {(currentPage - 1) * pageSize + index + 1}
                  </TableCell>
                  <TableCell>{row.date.viDateString}</TableCell>
                  <TableCell>{row.during}</TableCell>
                  <TableCell>{row.grossRevenueFormat}</TableCell>
                  <TableCell align="center">
                    <Button
                      variant="contained"
                      color="success"
                      size="small"
                      onClick={() => handleExport(row.sessionId)}
                    >
                      Excel
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ mt: 2, justifyContent: "space-between", alignItems: "center" }}
      >
        <FormControl size="small" sx={{ minWidth: 100 }}>
          <InputLabel id="page-size-label">Số dòng</InputLabel>
          <Select
            labelId="page-size-label"
            label="Số dòng"
            value={pageSize}
            onChange={(e) => handlePageSizeChange(Number(e.target.value))}
          >
            {[10, 20, 30, 50, 100].map((n) => (
              <MenuItem key={n} value={n}>
                {n}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Typography variant="body2" color="text.secondary">
          Tổng số phiên làm việc: <strong>{totalRows}</strong>
        </Typography>
        {totalPages > 0 && (
          <Pagination
            count={totalPages}
            page={currentPage}
            onChange={handlePageChange}
            color="primary"
            showFirstButton
            showLastButton
          />
        )}
      </Stack>
    </Box>
  );
}
