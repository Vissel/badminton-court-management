import React, { useCallback, useEffect, useState } from "react";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
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
import TableFooter from "@mui/material/TableFooter";
import Paper from "@mui/material/Paper";
import CircularProgress from "@mui/material/CircularProgress";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import SearchIcon from "@mui/icons-material/Search";
import { getTaxReport, exportTaxReport } from "../api/billApi";
import { emitApiError } from "../api/errorBus";
import DateInput from "./DateInput";
import { downloadExport } from "./DebtManagementPage";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";

const GROUP_LABEL = { DAY: "Ngày", MONTH: "Tháng", QUARTER: "Quý" };

const firstOfMonth = () => {
    const d = new Date();
    return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1))
        .toISOString()
        .slice(0, 10);
};
const today = () => new Date().toISOString().slice(0, 10);

/**
 * Bảng kê bán ra (G-6): revenue and output VAT per period. Voided bills are
 * excluded server-side so the totals match what was actually declared.
 */
const TaxReportPanel = () => {
    const [fromDate, setFromDate] = useState(firstOfMonth());
    const [toDate, setToDate] = useState(today());
    const [groupBy, setGroupBy] = useState("MONTH");
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(false);
    const [exporting, setExporting] = useState(false);

    const params = useCallback(
        () => ({ fromDate: fromDate || null, toDate: toDate || null, groupBy }),
        [fromDate, toDate, groupBy]
    );

    const load = useCallback(() => {
        setLoading(true);
        getTaxReport(params())
            .then((res) => {
                const result = res?.data;
                if (result?.success && result.data) {
                    setReport(result.data);
                } else {
                    emitApiError(result?.errorMessage || "Không tải được báo cáo thuế.");
                }
            })
            .catch(() => { })
            .finally(() => setLoading(false));
    }, [params]);

    useEffect(() => {
        load();
        // Only on mount — the user re-runs it with the "Xem" button.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleExport = () => {
        setExporting(true);
        exportTaxReport(params())
            .then((res) => {
                if (res?.data) downloadExport(res, "bang-ke-thue.xlsx");
            })
            .catch(() => { })
            .finally(() => setExporting(false));
    };

    const rows = report?.rows || [];

    return (
        <Box>
            <Stack
                direction={{ xs: "column", sm: "row" }}
                spacing={1.5}
                sx={{ mb: 2, flexWrap: "wrap" }}
                useFlexGap
            >
                <DateInput size="small" label="Từ ngày" value={fromDate} onChange={setFromDate} />
                <DateInput size="small" label="Đến ngày" value={toDate} onChange={setToDate} />
                <FormControl size="small" sx={{ minWidth: 140 }}>
                    <InputLabel>Nhóm theo</InputLabel>
                    <Select label="Nhóm theo" value={groupBy} onChange={(e) => setGroupBy(e.target.value)}>
                        <MenuItem value="DAY">Ngày</MenuItem>
                        <MenuItem value="MONTH">Tháng</MenuItem>
                        <MenuItem value="QUARTER">Quý</MenuItem>
                    </Select>
                </FormControl>
                <Button variant="contained" startIcon={<SearchIcon />} onClick={load} disabled={loading}>
                    Xem
                </Button>
                <Button
                    variant="outlined"
                    startIcon={<FileDownloadIcon />}
                    onClick={handleExport}
                    disabled={exporting || !report}
                >
                    Xuất Excel
                </Button>
            </Stack>

            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Kỳ</TableCell>
                            <TableCell align="right">Số hoá đơn</TableCell>
                            <TableCell align="right">Doanh thu (gồm VAT)</TableCell>
                            <TableCell align="right">Giá chưa VAT</TableCell>
                            <TableCell align="right">Tiền VAT</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {loading ? (
                            <TableRow>
                                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                                    <CircularProgress size={28} />
                                </TableCell>
                            </TableRow>
                        ) : rows.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                                    <Typography color="text.secondary">Không có dữ liệu trong kỳ</Typography>
                                </TableCell>
                            </TableRow>
                        ) : (
                            rows.map((row) => (
                                <TableRow key={row.period} hover>
                                    <TableCell>{row.period}</TableCell>
                                    <TableCell align="right">{row.billCount}</TableCell>
                                    <TableCell align="right">{formatVND(row.gross)}</TableCell>
                                    <TableCell align="right">{formatVND(row.net)}</TableCell>
                                    <TableCell align="right">{formatVND(row.vat)}</TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                    {report && rows.length > 0 && (
                        <TableFooter>
                            <TableRow>
                                <TableCell sx={{ fontWeight: 700 }}>Tổng cộng</TableCell>
                                <TableCell align="right" sx={{ fontWeight: 700 }}>
                                    {report.billCount}
                                </TableCell>
                                <TableCell align="right" sx={{ fontWeight: 700 }}>
                                    {formatVND(report.gross)}
                                </TableCell>
                                <TableCell align="right" sx={{ fontWeight: 700 }}>
                                    {formatVND(report.net)}
                                </TableCell>
                                <TableCell align="right" sx={{ fontWeight: 700 }}>
                                    {formatVND(report.vat)} {VN_CURRENCY}
                                </TableCell>
                            </TableRow>
                        </TableFooter>
                    )}
                </Table>
            </TableContainer>

            <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1 }}>
                Đơn vị: {VN_CURRENCY}. Hoá đơn đã huỷ không được tính vào doanh thu. Nhóm:{" "}
                {GROUP_LABEL[report?.groupBy] || ""}.
            </Typography>
        </Box>
    );
};

export default TaxReportPanel;
