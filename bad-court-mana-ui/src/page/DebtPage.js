import React, { useState, useEffect, useCallback, useRef } from "react";
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
import TableSortLabel from "@mui/material/TableSortLabel";
import Paper from "@mui/material/Paper";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import CircularProgress from "@mui/material/CircularProgress";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import Collapse from "@mui/material/Collapse";
import Checkbox from "@mui/material/Checkbox";
import SearchIcon from "@mui/icons-material/Search";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@mui/icons-material/KeyboardArrowUp";
import {
  listDebtPlayers,
  getDebitSummary,
  listRemainingDebts,
  payDebits,
  REMAINING_DEBTS_PAGE,
  REMAINING_DEBTS_FILTER,
} from "../api/debtApi";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";
import { formatVNDateTime, toServerDateTimeString } from "./DateTimeUtils";

const DEFAULT_QUERY = {
  page: 1,
  pageSize: 10,
  playerName: "",
  sortField: "playerName",
  sortDir: "asc",
};

const COL_COUNT = 7; // expand + STT + 4 data + action

export default function DebtPage() {
  const [query, setQuery] = useState(DEFAULT_QUERY);
  const [searchInput, setSearchInput] = useState("");
  const [players, setPlayers] = useState([]);
  const [totalRows, setTotalRows] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // playerName -> DebitSummaryResponse | null(error); absent = still loading
  const [summaries, setSummaries] = useState({});
  const requestedSummaries = useRef(new Set());
  // playerName -> { loading, error, data: GetRemainingDebtResponse }
  const [details, setDetails] = useState({});
  const [expandedRows, setExpandedRows] = useState(() => new Set());
  // playerName -> Set<index> of checked rows in the expanded detail table
  const [selections, setSelections] = useState({});
  const [paying, setPaying] = useState({});

  const fetchPlayers = useCallback(async (q) => {
    setLoading(true);
    setError(null);
    try {
      const res = await listDebtPlayers({
        pagination: { current: q.page, pageSize: q.pageSize },
        filter: { playerName: q.playerName || null },
        sort: { field: q.sortField, direction: q.sortDir.toUpperCase() },
      });
      const page = res?.data?.data;
      setPlayers(page?.list || []);
      setTotalRows(page?.total || 0);
      setTotalPages(page?.pagination?.totalPage || 0);
    } catch (e) {
      console.error("Failed to fetch debtor list", e);
      setPlayers([]);
      setTotalRows(0);
      setTotalPages(0);
      setError("Không tải được danh sách nợ. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSummary = useCallback((playerName) => {
    requestedSummaries.current.add(playerName);
    getDebitSummary(playerName)
      .then((res) =>
        setSummaries((s) => ({ ...s, [playerName]: res?.data || null }))
      )
      .catch(() => setSummaries((s) => ({ ...s, [playerName]: null })));
  }, []);

  const fetchDetails = useCallback((playerName) => {
    setDetails((d) => ({ ...d, [playerName]: { loading: true } }));
    listRemainingDebts(playerName, REMAINING_DEBTS_PAGE, REMAINING_DEBTS_FILTER)
      .then((res) =>
        setDetails((d) => ({
          ...d,
          [playerName]: { loading: false, data: res?.data },
        }))
      )
      .catch(() =>
        setDetails((d) => ({
          ...d,
          [playerName]: { loading: false, error: true },
        }))
      );
  }, []);

  // Debounce the player-name search into the query (server-side search).
  useEffect(() => {
    const timer = setTimeout(() => {
      const name = searchInput.trim();
      setQuery((q) => (q.playerName === name ? q : { ...q, playerName: name, page: 1 }));
    }, 500);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    fetchPlayers(query);
  }, [query, fetchPlayers]);

  // Fetch per-player summaries for the visible rows only.
  useEffect(() => {
    players.forEach((p) => {
      if (!requestedSummaries.current.has(p.playerName)) {
        fetchSummary(p.playerName);
      }
    });
  }, [players, fetchSummary]);

  const updateQuery = (patch) => setQuery((q) => ({ ...q, ...patch, page: 1 }));

  const handleSort = (field) =>
    updateQuery({
      sortField: field,
      sortDir:
        query.sortField === field && query.sortDir === "asc" ? "desc" : "asc",
    });

  const toggleExpand = (playerName) => {
    const isOpening = !expandedRows.has(playerName);
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(playerName)) next.delete(playerName);
      else next.add(playerName);
      return next;
    });
    if (isOpening && !details[playerName]) {
      fetchDetails(playerName);
    }
  };

  const refresh = () => fetchPlayers(query);

  const refreshPlayer = (playerName) => {
    requestedSummaries.current.delete(playerName);
    fetchSummary(playerName);
    if (expandedRows.has(playerName)) fetchDetails(playerName);
    refresh();
  };

  // Case 1 — no listDebitPay: backend pays all debts up to totalPayAmount.
  // Case 2 — listDebitPay provided: backend pays exactly the selected debts.
  const payForPlayer = async (playerName, totalPayAmount, listDebitPay) => {
    const label = listDebitPay?.length
      ? `${listDebitPay.length} khoản đã chọn`
      : "toàn bộ nợ";
    if (
      !window.confirm(
        `Xác nhận thu ${formatVND(totalPayAmount)} ${VN_CURRENCY} (${label}) từ ${playerName}?`
      )
    ) {
      return;
    }
    setPaying((p) => ({ ...p, [playerName]: true }));
    try {
      const res = await payDebits({
        playerName,
        totalPayAmount,
        paymentMethod: "CASH",
        note: "",
        listDebitPay,
      });
      const data = res?.data;
      if (data && data.status !== "FAIL") {
        setSelections((s) => ({ ...s, [playerName]: new Set() }));
        refreshPlayer(playerName);
      } else {
        alert(data?.message || "Thanh toán thất bại");
      }
    } catch (e) {
      // api interceptor already alerts on transport/server errors
    } finally {
      setPaying((p) => ({ ...p, [playerName]: false }));
    }
  };

  // Row-level "Thu nợ" — pay everything the player owes.
  const payAll = (playerName) => {
    const total = summaries[playerName]?.totalDebts?.amount || 0;
    if (total <= 0) return;
    payForPlayer(playerName, total, null);
  };

  // Expanded-panel "Thu nợ đã chọn" — pay only the checked debts.
  const paySelected = (playerName) => {
    const debits = details[playerName]?.data?.remainingDebits || [];
    const listDebitPay = [...(selections[playerName] || [])]
      .map((idx) => debits[idx])
      .filter((d) => d?.dateTime && (d?.money?.amount || 0) > 0)
      .map((d) => ({
        dateTime: toServerDateTimeString(d.dateTime),
        payAmount: d.money.amount,
      }));
    if (listDebitPay.length === 0) return;
    payForPlayer(
      playerName,
      listDebitPay.reduce((sum, i) => sum + i.payAmount, 0),
      listDebitPay
    );
  };

  const payableIndexes = (playerName) =>
    (details[playerName]?.data?.remainingDebits || [])
      .map((d, i) => ((d?.money?.amount || 0) > 0 ? i : -1))
      .filter((i) => i >= 0);

  const toggleSelect = (playerName, idx) => {
    setSelections((s) => {
      const next = new Set(s[playerName] || []);
      if (next.has(idx)) next.delete(idx);
      else next.add(idx);
      return { ...s, [playerName]: next };
    });
  };

  const toggleSelectAll = (playerName) => {
    const payable = payableIndexes(playerName);
    setSelections((s) => {
      const cur = s[playerName] || new Set();
      const allChecked = payable.length > 0 && payable.every((i) => cur.has(i));
      return { ...s, [playerName]: allChecked ? new Set() : new Set(payable) };
    });
  };

  const renderSummaryCell = (playerName, field) => {
    if (!(playerName in summaries)) {
      return <CircularProgress size={14} />;
    }
    const s = summaries[playerName];
    if (!s) return "—";
    if (field === "amount") {
      return `${formatVND(s.totalDebts?.amount)} ${s.totalDebts?.currency || VN_CURRENCY}`;
    }
    return s.numberDebit ?? 0;
  };

  const renderDetail = (playerName) => {
    const detail = details[playerName];
    if (!detail || detail.loading) {
      return (
        <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
          <CircularProgress size={22} />
        </Box>
      );
    }
    if (detail.error) {
      return (
        <Box sx={{ textAlign: "center", py: 2 }}>
          <Typography variant="body2" color="error" sx={{ mb: 1 }}>
            Không tải được chi tiết nợ.
          </Typography>
          <Button size="small" variant="outlined" onClick={() => fetchDetails(playerName)}>
            Thử lại
          </Button>
        </Box>
      );
    }
    const debits = detail.data?.remainingDebits || [];
    if (debits.length === 0) {
      return (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ py: 2, textAlign: "center" }}
        >
          Không có khoản nợ nào.
        </Typography>
      );
    }

    const selected = selections[playerName] || new Set();
    const payable = payableIndexes(playerName);
    const allChecked = payable.length > 0 && payable.every((i) => selected.has(i));
    const someChecked = payable.some((i) => selected.has(i));
    const selectedTotal = [...selected].reduce(
      (sum, i) => sum + (debits[i]?.money?.amount || 0),
      0
    );

    return (
      <>
        <Stack direction="row" alignItems="center" spacing={1} sx={{ pb: 0.5 }}>
          <Checkbox
            size="small"
            checked={allChecked}
            indeterminate={!allChecked && someChecked}
            disabled={payable.length === 0}
            onChange={() => toggleSelectAll(playerName)}
            sx={{ p: 0.5 }}
          />
          <Typography variant="body2" color="text.secondary">
            Chọn tất cả ({payable.length} khoản)
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          {selected.size > 0 && (
            <Typography variant="body2" color="text.secondary">
              Đã chọn {selected.size} khoản —{" "}
              <strong>
                {formatVND(selectedTotal)} {VN_CURRENCY}
              </strong>
            </Typography>
          )}
          <Button
            size="small"
            variant="contained"
            color="warning"
            disableElevation
            disabled={selected.size === 0 || paying[playerName]}
            onClick={() => paySelected(playerName)}
          >
            {paying[playerName] ? (
              <CircularProgress size={16} color="inherit" />
            ) : (
              "Thu nợ đã chọn"
            )}
          </Button>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "grey.50" }}>
              <TableCell sx={{ width: 40 }} />
              <TableCell sx={{ width: 50 }}>#</TableCell>
              <TableCell>Ngày ghi nợ</TableCell>
              <TableCell align="right">Số tiền còn lại</TableCell>
              <TableCell>Ghi chú</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {debits.map((debt, idx) => {
              const payableRow = (debt.money?.amount || 0) > 0;
              return (
                <TableRow
                  key={idx}
                  hover
                  onClick={() => payableRow && toggleSelect(playerName, idx)}
                  sx={payableRow ? { cursor: "pointer" } : undefined}
                >
                  <TableCell>
                    <Checkbox
                      size="small"
                      checked={selected.has(idx)}
                      disabled={!payableRow}
                      onChange={() => toggleSelect(playerName, idx)}
                      onClick={(e) => e.stopPropagation()}
                      sx={{ p: 0.5 }}
                    />
                  </TableCell>
                  <TableCell>{idx + 1}</TableCell>
                  <TableCell>{formatVNDateTime(debt.dateTime)}</TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      fontWeight: 600,
                      color: payableRow ? "warning.dark" : "text.secondary",
                    }}
                  >
                    {formatVND(debt.money?.amount)} {debt.money?.currency || VN_CURRENCY}
                  </TableCell>
                  <TableCell
                    title={debt.note || ""}
                    sx={{
                      maxWidth: 260,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {debt.note}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </>
    );
  };

  return (
    <Box sx={{ mt: 2, px: { xs: 1, sm: 2 }, maxWidth: 1400, mx: "auto" }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Quản lý nợ
      </Typography>

      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={2}
        sx={{ mb: 3, alignItems: { md: "center" }, flexWrap: "wrap" }}
      >
        <TextField
          size="small"
          placeholder="Tìm theo tên người chơi..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          sx={{ minWidth: { md: 280 }, flex: { md: "1 1 280px" } }}
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
        <Box sx={{ flexGrow: 1 }} />
        <Typography variant="body2" color="text.secondary">
          Tổng số người nợ: <strong>{totalRows}</strong>
        </Typography>
      </Stack>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "grey.100" }}>
              <TableCell sx={{ width: 40 }} />
              <TableCell sx={{ width: 60 }}>STT</TableCell>
              <TableCell>
                <TableSortLabel
                  active={query.sortField === "playerName"}
                  direction={query.sortField === "playerName" ? query.sortDir : "asc"}
                  onClick={() => handleSort("playerName")}
                >
                  Người chơi
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={query.sortField === "lastDebitDate"}
                  direction={query.sortField === "lastDebitDate" ? query.sortDir : "asc"}
                  onClick={() => handleSort("lastDebitDate")}
                >
                  Ghi nợ gần nhất
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">Tổng nợ còn lại</TableCell>
              <TableCell align="center" sx={{ width: 90 }}>Số khoản</TableCell>
              <TableCell align="center" sx={{ width: 110 }}>Hành động</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={COL_COUNT} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={28} />
                </TableCell>
              </TableRow>
            )}
            {!loading && error && (
              <TableRow>
                <TableCell colSpan={COL_COUNT} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="error" sx={{ mb: 1 }}>
                    {error}
                  </Typography>
                  <Button size="small" variant="outlined" onClick={refresh}>
                    Thử lại
                  </Button>
                </TableCell>
              </TableRow>
            )}
            {!loading && !error && players.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={COL_COUNT}
                  align="center"
                  sx={{ py: 4, color: "text.secondary" }}
                >
                  Không có người nợ nào.
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              !error &&
              players.map((row, index) => {
                const open = expandedRows.has(row.playerName);
                const summary = summaries[row.playerName];
                const canPayAll = (summary?.totalDebts?.amount || 0) > 0;
                return (
                  <React.Fragment key={row.playerName}>
                    <TableRow
                      hover
                      onClick={() => toggleExpand(row.playerName)}
                      sx={{ cursor: "pointer", "& > *": { borderBottom: "unset" } }}
                    >
                      <TableCell>
                        <IconButton size="small" aria-label={open ? "collapse" : "expand"}>
                          {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                        </IconButton>
                      </TableCell>
                      <TableCell>
                        {(query.page - 1) * query.pageSize + index + 1}
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>{row.playerName}</TableCell>
                      <TableCell>{formatVNDateTime(row.lastDebitDate)}</TableCell>
                      <TableCell
                        align="right"
                        sx={{ fontWeight: 600, color: "warning.dark" }}
                      >
                        {renderSummaryCell(row.playerName, "amount")}
                      </TableCell>
                      <TableCell align="center">
                        {renderSummaryCell(row.playerName, "count")}
                      </TableCell>
                      <TableCell align="center">
                        <Button
                          variant="contained"
                          color="warning"
                          size="small"
                          disabled={!canPayAll || paying[row.playerName]}
                          onClick={(e) => {
                            e.stopPropagation();
                            payAll(row.playerName);
                          }}
                        >
                          {paying[row.playerName] ? (
                            <CircularProgress size={16} color="inherit" />
                          ) : (
                            "Thu nợ"
                          )}
                        </Button>
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell colSpan={COL_COUNT} sx={{ py: 0 }}>
                        <Collapse in={open} timeout="auto" unmountOnExit>
                          <Box sx={{ py: 1, px: { xs: 0, sm: 2 } }}>
                            {renderDetail(row.playerName)}
                          </Box>
                        </Collapse>
                      </TableCell>
                    </TableRow>
                  </React.Fragment>
                );
              })}
          </TableBody>
        </Table>
      </TableContainer>

      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ mt: 2, justifyContent: "space-between", alignItems: "center" }}
      >
        <FormControl size="small" sx={{ minWidth: 100 }}>
          <InputLabel id="debt-page-size-label">Số dòng</InputLabel>
          <Select
            labelId="debt-page-size-label"
            label="Số dòng"
            value={query.pageSize}
            onChange={(e) => updateQuery({ pageSize: Number(e.target.value) })}
          >
            {[10, 20, 50].map((n) => (
              <MenuItem key={n} value={n}>
                {n}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Typography variant="body2" color="text.secondary">
          Tổng số người nợ: <strong>{totalRows}</strong>
        </Typography>
        {totalPages > 0 && (
          <Pagination
            count={totalPages}
            page={query.page}
            onChange={(_, page) => setQuery((q) => ({ ...q, page }))}
            color="primary"
            showFirstButton
            showLastButton
          />
        )}
      </Stack>
    </Box>
  );
}
