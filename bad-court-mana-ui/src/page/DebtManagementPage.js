import React, { useState, useEffect, useLayoutEffect, useCallback, useMemo, useRef } from "react";
import { utils as excelUtils, write as writeExcel } from "xlsx";
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
import Fade from "@mui/material/Fade";
import Checkbox from "@mui/material/Checkbox";
import SearchIcon from "@mui/icons-material/Search";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@mui/icons-material/KeyboardArrowUp";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import ToggleButton from "@mui/material/ToggleButton";
import PaymentsIcon from "@mui/icons-material/Payments";
import HistoryIcon from "@mui/icons-material/History";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import Chip from "@mui/material/Chip";
import {
  listAllPlayers,
  getDebitSummary,
  listRemainingDebts,
  getDebitHistorySummary,
  listDebitHistory,
  exportDebtReport,
  REMAINING_DEBTS_PAGE,
  REMAINING_DEBTS_FILTER,
} from "../api/debtApi";
import { emitApiError } from "../api/errorBus";
import DebitListDialog from "./dialog/DebitListDialog";
import { VN_CURRENCY, formatVND } from "./MoneyUtils";
import { formatVNDateTime } from "./DateTimeUtils";

const DEFAULT_QUERY = {
  page: 1,
  pageSize: 10,
  playerName: "",
  sortField: "playerName", // playerName | totalDebt
  sortDir: "asc",
};

const MODE = { CURRENT: "current", HISTORY: "history" };

const getExportFileName = (response, fallback) => {
  const disposition = response?.headers?.["content-disposition"] || "";
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) return decodeURIComponent(encoded.replace(/^"|"$/g, ""));
  return disposition.match(/filename="?([^";]+)"?/i)?.[1] || fallback;
};

export const downloadBlob = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const downloadExport = (response, fallback) => {
  if (!response?.data) throw new Error("Empty export response");
  downloadBlob(response.data, getExportFileName(response, fallback));
};

const safeExcelText = (value) => {
  const text = String(value ?? "");
  return /^[=+@-]/.test(text) ? `'${text}` : text;
};

const exportTableToExcel = (title, headers, rows, fileName) => {
  const worksheet = excelUtils.aoa_to_sheet([[title], [], headers, ...rows]);
  worksheet["!cols"] = headers.map((header, columnIndex) => ({
    wch: Math.max(
      String(header).length + 4,
      ...rows.map((row) => String(row[columnIndex] ?? "").length + 2),
      12
    ),
  }));
  worksheet["!merges"] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: Math.max(headers.length - 1, 0) } },
  ];
  worksheet["!autofilter"] = {
    ref: excelUtils.encode_range({
      s: { r: 2, c: 0 },
      e: { r: Math.max(rows.length + 2, 2), c: headers.length - 1 },
    })
  };

  rows.forEach((row, rowIndex) => {
    row.forEach((cell, columnIndex) => {
      const address = excelUtils.encode_cell({ r: rowIndex + 3, c: columnIndex });
      if (typeof cell === "number") {
        worksheet[address] = { t: "n", v: cell, z: "#,##0" };
      } else {
        worksheet[address] = { t: "s", v: safeExcelText(cell) };
      }
    });
  });

  const workbook = excelUtils.book_new();
  excelUtils.book_append_sheet(workbook, worksheet, "Chi tiết");
  const bytes = writeExcel(workbook, { type: "array", bookType: "xlsx" });
  downloadBlob(
    new Blob([bytes], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    }),
    fileName
  );
};

// Accent-insensitive match — staff usually type names without diacritics
// ("nguyen" must find "Nguyễn").
const normalizeVN = (s) =>
  (s || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d"); // đ doesn't decompose under NFD

// Small worker pool: walk `items` with `worker`, `concurrency` at a time —
// streams per-player calls without stampeding the backend.
const runPool = (items, worker, concurrency = 6) => {
  let cursor = 0;
  return Promise.all(
    Array.from({ length: Math.min(concurrency, items.length) }, async () => {
      while (cursor < items.length) await worker(items[cursor++]);
    })
  );
};

// Wide page for date-filtered history scans: `total` stays accurate beyond
// it, but the paid/unpaid split is derived from the fetched items.
const FILTERED_HISTORY_PAGE = { current: 1, pageSize: 100, totalPage: 0 };

// Under a date range /listRemainingDebts' debitSummary stays all-time — the
// filtered debit page itself is the source of truth for the row.
const summaryFromDebits = (playerName, debits) => ({
  playerName,
  totalDebts: {
    amount: debits.reduce((sum, d) => sum + (d?.money?.amount || 0), 0),
    currency: debits[0]?.money?.currency || VN_CURRENCY,
  },
  numberDebit: debits.length,
});

// Same idea for history mode: /summaryHistory takes no filter, so derive the
// accumulate fields from the filtered /history page (paid ⇔ remaining = 0).
const histSummaryFromItems = (playerName, data) => {
  const items = data?.list || [];
  const total = data?.total ?? items.length;
  const numPaidDebits = items.filter((i) => (i?.remainingAmount || 0) <= 0).length;
  return {
    playerName,
    numDebits: total,
    numPaidDebits,
    numUnpaidDebits: Math.max(total - numPaidDebits, 0),
    totalDebitAmount: items.reduce((s, i) => s + (i?.debtAmount || 0), 0),
    totalPaidAmount: items.reduce((s, i) => s + (i?.paidAmount || 0), 0),
    totalRemainingAmount: items.reduce((s, i) => s + (i?.remainingAmount || 0), 0),
    currency: items[0]?.currency || VN_CURRENCY,
  };
};

export default function DebtManagementPage() {
  const [query, setQuery] = useState(DEFAULT_QUERY);
  const [searchInput, setSearchInput] = useState("");
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [summariesPending, setSummariesPending] = useState(0);
  const [error, setError] = useState(null);

  // playerName -> DebitSummaryResponse | null(failed / no data)
  const [summaries, setSummaries] = useState({});
  // playerName -> { loading, error, data: GetRemainingDebtResponse }
  const [details, setDetails] = useState({});
  const [expandedRows, setExpandedRows] = useState(() => new Set());
  // playerName -> Set<index> of checked rows in the expanded detail table
  const [selections, setSelections] = useState({});
  const [debitDialog, setDebitDialog] = useState({
    show: false,
    playerName: "",
    preselected: [],
  });
  // Read-only /history browser opened by "Tìm thêm" in the expanded panels
  const [historyDialog, setHistoryDialog] = useState({
    show: false,
    playerName: "",
  });

  // ── History mode (lazy — nothing is fetched until the mode is entered) ──
  const [mode, setMode] = useState(MODE.CURRENT);
  // playerName -> DebitHistorySummaryResponse | null
  const [histSummaries, setHistSummaries] = useState({});
  const [histPending, setHistPending] = useState(0);
  // playerName -> { loading, error, page, data: PageResponse<DebitHistoryItem> }
  const [histDetails, setHistDetails] = useState({});
  // players whose expanded panel shows history while in current mode
  const [historyView, setHistoryView] = useState(() => new Set());
  const histRequestedRef = useRef(new Set());

  // Remote lookup when a completed search finds nothing locally — the typed
  // text is treated as an exact player name and asked to the backend.
  const [remoteSearching, setRemoteSearching] = useState(false);
  const remoteSearchedRef = useRef(new Set()); // `${mode}|${name}` already tried
  const [exportingPage, setExportingPage] = useState(false);

  // ── Date-range filter (history mode only) ──
  // Raw input vs the applied filter sent to the backend. The ref mirrors the
  // state so queued pool workers — which hold stale callback identities —
  // still read the live filter. `filterEpochRef` is bumped on every history
  // reset; responses issued under an older epoch land dead instead of writing
  // stale data into the new scan's maps.
  const [dateInput, setDateInput] = useState({ from: "", to: "" });
  const [activeFilter, setActiveFilterState] = useState(REMAINING_DEBTS_FILTER);
  const activeFilterRef = useRef(REMAINING_DEBTS_FILTER);
  const filterEpochRef = useRef(0);
  // Tất cả / Lọc promote the history stream from the debtor seed to the full
  // roster — tracked so "Thử lại" re-runs the same scan.
  const [histScanAll, setHistScanAll] = useState(false);

  const setActiveFilter = (filter) => {
    activeFilterRef.current = filter;
    setActiveFilterState(filter);
  };

  // FLIP: playerName -> tr element / its last measured natural top / in-flight
  // glide animation. After each render we diff positions and glide rows that
  // moved — but only for user-driven reordering (sort, search, page). While
  // summaries are still streaming, every landing re-sorts the list, so gliding
  // then keeps the whole table in constant motion; during that window we just
  // track positions and let rows pop into place.
  const rowElsRef = useRef(new Map());
  const rowTopsRef = useRef(new Map());
  const rowAnimsRef = useRef(new Map());
  const setRowEl = (playerName) => (el) => {
    if (el) rowElsRef.current.set(playerName, el);
    else rowElsRef.current.delete(playerName);
  };

  useLayoutEffect(() => {
    const streaming =
      loading || remoteSearching || summariesPending > 0 || histPending > 0;
    const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    const next = new Map();
    rowElsRef.current.forEach((el, name) => {
      // Rect with the in-flight transform still applied, then without it —
      // cancel + re-measure happens pre-paint, so nothing visibly snaps.
      const visual = el.getBoundingClientRect().top;
      rowAnimsRef.current.get(name)?.cancel();
      const natural = el.getBoundingClientRect().top;
      // last-seen position = previous natural top + leftover glide offset
      const prev = rowTopsRef.current.get(name) ?? natural;
      const delta = prev + visual - 2 * natural;
      if (!streaming && !reduced && Math.abs(delta) > 2) {
        rowAnimsRef.current.set(
          name,
          el.animate(
            [
              { transform: `translateY(${delta}px)` },
              { transform: "translateY(0px)" },
            ],
            { duration: 200, easing: "ease-out" }
          )
        );
      }
      next.set(name, natural);
    });
    rowTopsRef.current = next;
  });

  // Tracks which player summaries have been requested so the background
  // stream and the search lookup never fetch the same player twice.
  const requestedSummariesRef = useRef(new Set());

  const fetchSummary = useCallback((playerName) => {
    setSummariesPending((n) => n + 1);
    return getDebitSummary(playerName)
      .then((r) => {
        const b = r?.data;
        setSummaries((s) => ({
          ...s,
          [playerName]: b?.success && b.data ? b.data : null,
        }));
      })
      .catch(() => setSummaries((s) => ({ ...s, [playerName]: null })))
      .finally(() => setSummariesPending((n) => n - 1));
  }, []);

  const fetchSummaryOnce = useCallback(
    (playerName) => {
      if (requestedSummariesRef.current.has(playerName)) return;
      requestedSummariesRef.current.add(playerName);
      return fetchSummary(playerName);
    },
    [fetchSummary]
  );

  // Fetches the player roster only — returns the list ([] on failure). Both
  // modes need it, so a retry in history mode can recover a failed load too.
  const loadPlayers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await listAllPlayers();
      const body = res?.data;
      if (!body?.success || !Array.isArray(body?.data)) {
        throw new Error(body?.errorMessage || "Failed to load players");
      }
      setPlayers(body.data);
      setLoading(false); // players known — the table can stream rows in
      return body.data;
    } catch (e) {
      console.error("Failed to fetch player list", e);
      setPlayers([]);
      setSummaries({});
      setError("Không tải được danh sách nợ. Vui lòng thử lại.");
      setLoading(false);
      return [];
    }
  }, []);

  // 1) all players, then 2) a debit summary per player. Summaries stream in:
  // each response updates state immediately, so a player with debts appears in
  // the table as soon as their /summary lands instead of after the whole batch.
  // Requests go through a small worker pool so many players don't stampede
  // the backend with N concurrent calls.
  const loadDebtors = useCallback(async () => {
    const list = await loadPlayers();
    setSummaries({});
    requestedSummariesRef.current = new Set();
    if (list.length) await runPool(list, (p) => fetchSummaryOnce(p.playerName));
  }, [loadPlayers, fetchSummaryOnce]);

  const fetchDetails = useCallback((playerName) => {
    setDetails((d) => ({ ...d, [playerName]: { loading: true } }));
    listRemainingDebts(playerName, REMAINING_DEBTS_PAGE, REMAINING_DEBTS_FILTER)
      .then((res) => {
        const b = res?.data; // Result<GetRemainingDebtResponse>
        setDetails((d) => ({
          ...d,
          [playerName]: {
            loading: false,
            data: b?.success ? b.data : null,
            error: !b?.success,
          },
        }));
      })
      .catch(() =>
        setDetails((d) => ({
          ...d,
          [playerName]: { loading: false, error: true },
        }))
      );
  }, []);

  // ── history fetches (only ever called after entering history mode or
  //    clicking the per-player "Xem lịch sử" link) ──
  const fetchHistSummary = useCallback((playerName) => {
    const epoch = filterEpochRef.current;
    setHistPending((n) => n + 1);
    return getDebitHistorySummary(playerName)
      .then((r) => {
        if (epoch !== filterEpochRef.current) return;
        const b = r?.data;
        setHistSummaries((s) => ({
          ...s,
          [playerName]: b?.success && b.data ? b.data : null,
        }));
      })
      .catch(() => {
        if (epoch !== filterEpochRef.current) return;
        setHistSummaries((s) => ({ ...s, [playerName]: null }));
      })
      .finally(() => setHistPending((n) => n - 1));
  }, []);

  // Date-range mode: /summaryHistory takes no filter, so the stream scans the
  // player's filtered /history page — the response seeds both the row's
  // accumulate fields and the expanded panel.
  const fetchHistFiltered = useCallback((playerName) => {
    const epoch = filterEpochRef.current;
    setHistPending((n) => n + 1);
    return listDebitHistory(playerName, FILTERED_HISTORY_PAGE, activeFilterRef.current)
      .then((res) => {
        if (epoch !== filterEpochRef.current) return;
        const b = res?.data;
        const data = b?.success ? b.data : null;
        setHistSummaries((s) => ({
          ...s,
          [playerName]: data ? histSummaryFromItems(playerName, data) : null,
        }));
        if (data) {
          setHistDetails((d) => ({
            ...d,
            [playerName]: { loading: false, page: 1, data },
          }));
        }
      })
      .catch(() => {
        if (epoch !== filterEpochRef.current) return;
        setHistSummaries((s) => ({ ...s, [playerName]: null }));
      })
      .finally(() => setHistPending((n) => n - 1));
  }, []);

  const fetchHistSummaryOnce = useCallback(
    (playerName) => {
      if (histRequestedRef.current.has(playerName)) return;
      histRequestedRef.current.add(playerName);
      return activeFilterRef.current !== REMAINING_DEBTS_FILTER
        ? fetchHistFiltered(playerName)
        : fetchHistSummary(playerName);
    },
    [fetchHistSummary, fetchHistFiltered]
  );

  const fetchHistory = useCallback((playerName, page = 1) => {
    const epoch = filterEpochRef.current;
    setHistDetails((d) => ({
      ...d,
      [playerName]: { ...(d[playerName] || {}), loading: true },
    }));
    listDebitHistory(
      playerName,
      { current: page, pageSize: 10, totalPage: 0 },
      activeFilterRef.current
    )
      .then((res) => {
        if (epoch !== filterEpochRef.current) return;
        const b = res?.data;
        setHistDetails((d) => ({
          ...d,
          [playerName]: {
            loading: false,
            page,
            data: b?.success && b.data ? b.data : null,
            error: !(b?.success && b.data),
          },
        }));
      })
      .catch(() => {
        if (epoch !== filterEpochRef.current) return;
        setHistDetails((d) => ({
          ...d,
          [playerName]: { loading: false, error: true },
        }));
      });
  }, []);

  const ensurePlayer = useCallback((playerName) => {
    setPlayers((ps) =>
      ps.some((p) => p.playerName === playerName) ? ps : [...ps, { playerName }]
    );
  }, []);

  // current mode → /listRemainingDebts with the typed name
  const remoteLookupCurrent = useCallback(
    (name) => {
      setRemoteSearching(true);
      listRemainingDebts(name, REMAINING_DEBTS_PAGE, REMAINING_DEBTS_FILTER)
        .then((res) => {
          const b = res?.data; // Result<GetRemainingDebtResponse>
          const data = b?.success ? b.data : null;
          const found =
            (data?.remainingDebits?.length || 0) > 0 ||
            (data?.debitSummary?.numberDebit || 0) > 0;
          if (!found) return;
          ensurePlayer(name);
          requestedSummariesRef.current.add(name);
          setSummaries((s) => ({
            ...s,
            [name]: data.debitSummary || summaryFromDebits(name, data.remainingDebits || []),
          }));
          setDetails((d) => ({ ...d, [name]: { loading: false, data } }));
        })
        .catch(() => {
          // player not found / no debts — the empty state stays honest
        })
        .finally(() => setRemoteSearching(false));
    },
    [ensurePlayer]
  );

  // history mode → /history (page 1) for the typed name + /summaryHistory for
  // the row's accumulate fields (derived from the page itself when filtered,
  // since /summaryHistory has no date bounds)
  const remoteLookupHistory = useCallback(
    (name) => {
      setRemoteSearching(true);
      const epoch = filterEpochRef.current;
      const filtered = activeFilterRef.current !== REMAINING_DEBTS_FILTER;
      listDebitHistory(name, { current: 1, pageSize: 10, totalPage: 0 }, activeFilterRef.current)
        .then((res) => {
          if (epoch !== filterEpochRef.current) return;
          const b = res?.data;
          const data = b?.success ? b.data : null;
          if (!data || !(data.list?.length > 0 || data.total > 0)) return;
          ensurePlayer(name);
          histRequestedRef.current.add(name);
          setHistDetails((d) => ({ ...d, [name]: { loading: false, page: 1, data } }));
          if (filtered) {
            setHistSummaries((s) => ({ ...s, [name]: histSummaryFromItems(name, data) }));
            return;
          }
          return getDebitHistorySummary(name).then((r) => {
            if (epoch !== filterEpochRef.current) return;
            const sb = r?.data;
            setHistSummaries((s) => ({
              ...s,
              [name]:
                sb?.success && sb.data
                  ? sb.data
                  : { playerName: name, numDebits: data.total || data.list.length, numPaidDebits: 0, numUnpaidDebits: 0 },
            }));
          });
        })
        .catch(() => { })
        .finally(() => setRemoteSearching(false));
    },
    [ensurePlayer]
  );

  // Debounce the player-name search into the query.
  useEffect(() => {
    const timer = setTimeout(() => {
      const name = searchInput.trim();
      setQuery((q) => (q.playerName === name ? q : { ...q, playerName: name, page: 1 }));
    }, 500);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // A typed name jumps the queue: summaries for matching players are fetched
  // immediately (deduped with the stream), so search works against the full
  // roster — not just rows already loaded.
  useEffect(() => {
    const name = normalizeVN(query.playerName);
    if (!name) return;
    players.forEach((p) => {
      if (normalizeVN(p.playerName).includes(name)) {
        if (mode === MODE.CURRENT) fetchSummaryOnce(p.playerName);
        else fetchHistSummaryOnce(p.playerName);
      }
    });
  }, [players, query.playerName, mode, fetchSummaryOnce, fetchHistSummaryOnce]);

  // History mode's default roster: the players shown in CURRENT mode (current
  // debtors), plus anyone already history-loaded (e.g. remote search hits, so
  // a refresh keeps them). Past-only debtors aren't pre-fetched — the name
  // search still reaches them via remoteLookupHistory.
  const historySeed = useMemo(
    () =>
      players.filter(
        (p) =>
          (summaries[p.playerName]?.numberDebit || 0) > 0 ||
          p.playerName in histSummaries
      ),
    [players, summaries, histSummaries]
  );

  // History mode streams /summaryHistory for the seed. Re-fires as
  // current-mode summaries land (toggling before the stream finishes just
  // means the seed keeps growing); histRequestedRef dedupes the actual
  // requests so nothing is fetched twice.
  useEffect(() => {
    if (mode !== MODE.HISTORY || historySeed.length === 0) return;
    runPool(historySeed, (p) => fetchHistSummaryOnce(p.playerName));
  }, [mode, historySeed, fetchHistSummaryOnce]);

  useEffect(() => {
    loadDebtors();
  }, [loadDebtors]);

  const isHistory = mode === MODE.HISTORY;
  // Mode-aware accessors over the summary map driving the table.
  const rowSummaryOf = (p) =>
    (isHistory ? histSummaries : summaries)[p.playerName];
  const rowDebitCount = (p) => {
    const s = rowSummaryOf(p);
    return s ? (isHistory ? s.numDebits || 0 : s.numberDebit || 0) : 0;
  };
  const rowAmount = (p) => {
    const s = rowSummaryOf(p);
    return s
      ? isHistory
        ? s.totalDebitAmount || 0
        : s.totalDebts?.amount || 0
      : 0;
  };

  // Only players with a successful summary and any debits are listed.
  // Search / sort / pagination are all client-side over that filtered set.
  const debtors = useMemo(() => {
    const name = normalizeVN(query.playerName.trim());
    const list = players.filter((p) => {
      if (rowDebitCount(p) === 0) return false;
      return !name || normalizeVN(p.playerName).includes(name);
    });
    const dir = query.sortDir === "desc" ? -1 : 1;
    return [...list].sort((a, b) => {
      if (query.sortField === "totalDebt") {
        return (rowAmount(a) - rowAmount(b)) * dir || a.playerName.localeCompare(b.playerName, "vi");
      }
      return a.playerName.localeCompare(b.playerName, "vi") * dir;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [players, summaries, histSummaries, query, mode]);

  // A completed search with zero local results asks the backend directly —
  // /listRemainingDebts in current mode, /history in history mode — treating
  // the text as an exact player name. Deduped per mode+name.
  useEffect(() => {
    const name = query.playerName.trim();
    if (!name || loading || debtors.length > 0) return;
    const key = `${mode}|${name.toLowerCase()}`;
    if (remoteSearchedRef.current.has(key)) return;
    remoteSearchedRef.current.add(key);
    if (mode === MODE.CURRENT) remoteLookupCurrent(name);
    else remoteLookupHistory(name);
  }, [query.playerName, mode, loading, debtors.length, remoteLookupCurrent, remoteLookupHistory]);

  const totalRows = debtors.length;
  const summariesLoading =
    (isHistory ? histPending : summariesPending) > 0;
  // expand + STT + name + count + action (+ paid/total in history)
  const COL_COUNT = isHistory ? 6 : 5;
  const totalPages = Math.ceil(totalRows / query.pageSize);
  const page = Math.min(query.page, Math.max(totalPages, 1));
  const pageRows = debtors.slice(
    (page - 1) * query.pageSize,
    page * query.pageSize
  );

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
    if (!isOpening) return;
    if (isHistory || historyView.has(playerName)) {
      if (!histDetails[playerName]) fetchHistory(playerName);
    } else if (!details[playerName]) {
      fetchDetails(playerName);
    }
  };

  // History-mode scan over the whole roster (Tất cả / Lọc theo ngày): find
  // all players, then stream a per-player summary — each row lands as soon as
  // its response arrives instead of waiting for the whole batch.
  const streamAllHistory = async () => {
    const list = await loadPlayers();
    if (list.length) runPool(list, (p) => fetchHistSummaryOnce(p.playerName));
  };

  // Clears history caches so a new scan re-streams cleanly; the epoch bump
  // makes in-flight responses from the old scan land dead. Expanded rows are
  // collapsed so no panel is left spinning on wiped detail data.
  const resetHistoryData = () => {
    filterEpochRef.current += 1;
    setHistSummaries({});
    setHistDetails({});
    setExpandedRows(new Set());
    setHistoryView(new Set());
    histRequestedRef.current = new Set();
    remoteSearchedRef.current = new Set();
  };

  const refresh = () => {
    if (mode === MODE.CURRENT) return loadDebtors();
    if (histScanAll) {
      resetHistoryData();
      return streamAllHistory();
    }
    // Roster missing — reload it together with the current-mode summaries the
    // history seed derives from; the mode effect starts the stream as they land.
    if (players.length === 0) return loadDebtors();
    histRequestedRef.current = new Set();
    setHistSummaries({});
    runPool(historySeed, (p) => fetchHistSummaryOnce(p.playerName));
  };

  const refreshPlayer = (playerName) => {
    fetchSummary(playerName);
    if (expandedRows.has(playerName)) fetchDetails(playerName);
  };

  const applyDateFilter = () => {
    if (!isHistory) return;
    const { from, to } = dateInput;
    if (!from && !to) return;
    setHistScanAll(true);
    resetHistoryData();
    setActiveFilter({
      from: from || REMAINING_DEBTS_FILTER.from,
      // A bare YYYY-MM-DD parses to start-of-day on the backend — push "to" to
      // end-of-day so the picked date itself is included.
      to: to ? `${to}T23:59:59` : REMAINING_DEBTS_FILTER.to,
      amountFrom: 0,
      amountTo: 0,
    });
    streamAllHistory();
  };

  // "Tất cả" — scan every player's full history (no date bounds).
  const clearDateFilter = () => {
    if (!isHistory) return;
    setHistScanAll(true);
    resetHistoryData();
    setDateInput({ from: "", to: "" });
    setActiveFilter(REMAINING_DEBTS_FILTER);
    streamAllHistory();
  };

  const reportRequest = () => ({
    mode: isHistory ? "HISTORY" : "CURRENT",
    scope: "ALL_PLAYERS",
    playerName: null,
    playerNameFilter: query.playerName.trim() || null,
    // The date range is a history-mode concept — current-mode exports stay
    // unfiltered even if a range was applied on the history tab.
    ...(isHistory ? activeFilterRef.current : REMAINING_DEBTS_FILTER),
    sortField: query.sortField === "totalDebt" ? "TOTAL_DEBT" : "PLAYER_NAME",
    sortDirection: query.sortDir.toUpperCase(),
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Ho_Chi_Minh",
  });

  const handleExport = async () => {
    setExportingPage(true);
    try {
      const response = await exportDebtReport(reportRequest());
      const type = isHistory ? "lich-su-cong-no" : "cong-no-hien-tai";
      downloadExport(response, `${type}.xlsx`);
    } catch (e) {
      console.error("Failed to export debt report", e);
      emitApiError("Xuất báo cáo công nợ thất bại. Vui lòng thử lại.");
    } finally {
      setExportingPage(false);
    }
  };

  // Current-mode panels can flip into the player's history via the link next
  // to "Chọn tất cả" — flips back on second click.
  const toggleHistoryView = (playerName) => {
    const entering = !historyView.has(playerName);
    setHistoryView((prev) => {
      const next = new Set(prev);
      if (next.has(playerName)) next.delete(playerName);
      else next.add(playerName);
      return next;
    });
    if (!entering) return;
    if (!histDetails[playerName]) fetchHistory(playerName);
    fetchHistSummaryOnce(playerName); // powers the "Chưa trả" counter in the panel header
  };

  // "Thu nợ" opens DebitListDialog for the player; debts checked in the
  // expanded list are carried over and pre-ticked inside the dialog.
  const openDebitDialog = (playerName) => {
    const debits = details[playerName]?.data?.remainingDebits || [];
    const preselected = [...(selections[playerName] || [])]
      .map((idx) => debits[idx])
      .filter((d) => (d?.money?.amount || 0) > 0 && d?.dateTime);
    setDebitDialog({ show: true, playerName, preselected });
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
    const map = isHistory ? histSummaries : summaries;
    if (!(playerName in map)) {
      return <CircularProgress size={14} />;
    }
    const s = map[playerName];
    if (!s) return "—";
    if (field === "ratio") {
      // "đã trả / tổng" — the accumulate counter for history mode
      return `${s.numPaidDebits ?? 0}/${s.numDebits ?? 0}`;
    }
    return isHistory ? s.numDebits ?? 0 : s.numberDebit ?? 0;
  };

  const STATUS_META = {
    PAID: { label: "Đã trả", color: "success" },
    PARTIALLY_PAID: { label: "Trả một phần", color: "warning" },
    PENDING: { label: "Chưa trả", color: "default" },
  };

  const renderPlayerExportButton = (playerName, exportMode, rows, page = 1) => (
    <Button
      size="small"
      variant="text"
      startIcon={<FileDownloadIcon fontSize="small" />}
      onClick={(e) => {
        e.stopPropagation();
        const slug = normalizeVN(playerName).replace(/[^a-z0-9]+/g, "-");
        if (exportMode === MODE.HISTORY) {
          exportTableToExcel(
            `Lịch sử công nợ - ${playerName}`,
            ["#", "Ngày ghi nợ", "Số tiền nợ (VND)", "Còn lại (VND)", "Ghi chú", "Đã trả (VND)", "Ngày trả", "Trạng thái"],
            rows.slice(0, 10).map((item, index) => [
              (page - 1) * 10 + index + 1,
              formatVNDateTime(item.debtDateTime),
              item.debtAmount || 0,
              item.remainingAmount || 0,
              item.note || "",
              item.paidAmount || 0,
              item.paidDateTime ? formatVNDateTime(item.paidDateTime) : "—",
              STATUS_META[item.status]?.label || item.status || "—",
            ]),
            `lich-su-cong-no_${slug}.xlsx`
          );
        } else {
          exportTableToExcel(
            `Nợ hiện tại - ${playerName}`,
            ["#", "Ngày ghi nợ", "Số tiền còn lại (VND)", "Ghi chú"],
            rows.slice(0, 10).map((debt, index) => [
              index + 1,
              formatVNDateTime(debt.dateTime),
              debt.money?.amount || 0,
              debt.note || "",
            ]),
            `cong-no-hien-tai_${slug}.xlsx`
          );
        }
      }}
      aria-label={`Xuất Excel ${playerName}`}
      sx={{ textTransform: "none", p: 0, minWidth: 0 }}
    >
      Excel
    </Button>
  );

  const renderHistoryDetail = (playerName) => {
    const detail = histDetails[playerName];
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
            Không tải được lịch sử nợ.
          </Typography>
          <Button
            size="small"
            variant="outlined"
            onClick={() => fetchHistory(playerName, detail.page || 1)}
          >
            Thử lại
          </Button>
        </Box>
      );
    }
    const items = detail.data?.list || [];
    if (items.length === 0) {
      return (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ py: 2, textAlign: "center" }}
        >
          Chưa có lịch sử nợ.
        </Typography>
      );
    }
    const totalPage = detail.data?.pagination?.totalPage || 1;
    // Filtered prefetches arrive on a wide page — total, not totalPage, tells
    // whether the slice shown here has more behind it.
    const totalItems = detail.data?.total || 0;

    return (
      <>
        <Box sx={{ pb: 0.5, display: "flex", alignItems: "center", gap: 2 }}>
          {!isHistory && (
            <Button
              size="small"
              variant="text"
              onClick={(e) => {
                e.stopPropagation();
                toggleHistoryView(playerName);
              }}
              sx={{ textTransform: "none", p: 0, minWidth: 0 }}
            >
              ← Quay lại nợ hiện tại
            </Button>
          )}
          {(totalItems > 10 || totalPage > 1) && (
            <Button
              size="small"
              variant="text"
              onClick={(e) => {
                e.stopPropagation();
                setHistoryDialog({ show: true, playerName });
              }}
              sx={{ textTransform: "none", p: 0, minWidth: 0 }}
            >
              Tìm thêm
            </Button>
          )}
          {renderPlayerExportButton(playerName, MODE.HISTORY, items, detail.page)}
          <Box sx={{ flexGrow: 1 }} />
          {histSummaries[playerName] && (
            <Typography variant="body2" color="text.secondary">
              Chưa trả:{" "}
              <strong>
                {histSummaries[playerName].numUnpaidDebits ?? 0}/
                {histSummaries[playerName].numDebits ?? 0}
              </strong>
              khoản nợ
            </Typography>
          )}
        </Box>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "grey.50" }}>
              <TableCell sx={{ width: 50 }}>#</TableCell>
              <TableCell>Ngày ghi nợ</TableCell>
              <TableCell align="right">Số tiền nợ (VND)</TableCell>
              <TableCell align="right">Còn lại (VND)</TableCell>
              <TableCell>Ghi chú</TableCell>
              <TableCell align="right">Đã trả (VND)</TableCell>
              <TableCell>Ngày trả</TableCell>
              <TableCell align="center">Trạng thái</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.slice(0, 10).map((it, idx) => {
              const meta = STATUS_META[it.status] || { label: it.status || "—", color: "default" };
              return (
                <TableRow key={idx} hover>
                  <TableCell>{(detail.page - 1) * 10 + idx + 1}</TableCell>
                  <TableCell>{formatVNDateTime(it.debtDateTime)}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>
                    {formatVND(it.debtAmount)}
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      fontWeight: 600,
                      color: it.remainingAmount > 0 ? "warning.dark" : "text.secondary",
                    }}
                  >
                    {formatVND(it.remainingAmount)}
                  </TableCell>
                  <TableCell
                    title={it.note || ""}
                    sx={{
                      maxWidth: 220,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {it.note}
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{ fontWeight: 600, color: "success.dark" }}
                  >
                    {it.paidAmount > 0 ? formatVND(it.paidAmount) : "—"}
                  </TableCell>
                  <TableCell>
                    {it.paidDateTime ? formatVNDateTime(it.paidDateTime) : "—"}
                  </TableCell>
                  <TableCell align="center">
                    <Chip label={meta.label} color={meta.color} size="small" variant="outlined" />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </>
    );
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
          <Button
            size="small"
            variant="text"
            onClick={(e) => {
              e.stopPropagation();
              toggleHistoryView(playerName);
            }}
            sx={{ textTransform: "none", p: 0, minWidth: 0 }}
          >
            Xem lịch sử
          </Button>
          {debits.length > 10 && (
            <Button
              size="small"
              variant="text"
              onClick={(e) => {
                e.stopPropagation();
                setHistoryDialog({ show: true, playerName });
              }}
              sx={{ textTransform: "none", p: 0, minWidth: 0 }}
            >
              Tìm thêm
            </Button>
          )}
          {renderPlayerExportButton(playerName, MODE.CURRENT, debits)}
          <Box sx={{ flexGrow: 1 }} />
          {selected.size > 0 && (
            <Typography variant="body2" color="text.secondary">
              Đã chọn {selected.size} khoản —{" "}
              <strong>
                {formatVND(selectedTotal)} {VN_CURRENCY}
              </strong>
            </Typography>
          )}
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "grey.50" }}>
              <TableCell sx={{ width: 40 }} />
              <TableCell sx={{ width: 50 }}>#</TableCell>
              <TableCell>Ngày ghi nợ</TableCell>
              <TableCell align="right">Số tiền còn lại (VND)</TableCell>
              <TableCell>Ghi chú</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {debits.slice(0, 10).map((debt, idx) => {
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
                    {formatVND(debt.money?.amount)}
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
        <ToggleButtonGroup
          size="small"
          exclusive
          value={mode}
          onChange={(_, v) => {
            if (!v) return;
            setMode(v);
            // Date filtering is history-only — drop an applied range (and its
            // cached results) so the next history entry re-streams fresh and
            // current mode never runs under a hidden filter.
            if (v === MODE.CURRENT && activeFilterRef.current !== REMAINING_DEBTS_FILTER) {
              resetHistoryData();
              setHistScanAll(false);
              setDateInput({ from: "", to: "" });
              setActiveFilter(REMAINING_DEBTS_FILTER);
            }
          }}
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
          <ToggleButton value={MODE.CURRENT}>
            <PaymentsIcon fontSize="small" />
            Nợ hiện tại
          </ToggleButton>
          <ToggleButton value={MODE.HISTORY}>
            <HistoryIcon fontSize="small" />
            Lịch sử
          </ToggleButton>
        </ToggleButtonGroup>
        {isHistory && (
          <>
            <TextField
              type="date"
              size="small"
              label="Từ ngày"
              value={dateInput.from}
              onChange={(e) => setDateInput((d) => ({ ...d, from: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ width: { xs: "100%", md: 150 } }}
            />
            <TextField
              type="date"
              size="small"
              label="Đến ngày"
              value={dateInput.to}
              onChange={(e) => setDateInput((d) => ({ ...d, to: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ width: { xs: "100%", md: 150 } }}
            />
            <Button
              variant="outlined"
              size="small"
              onClick={applyDateFilter}
              sx={{ whiteSpace: "nowrap" }}
            >
              Lọc
            </Button>
            <Button
              variant="contained"
              size="small"
              onClick={clearDateFilter}
              disabled={!isHistory}
              sx={{ whiteSpace: "nowrap" }}
            >
              Tất cả
            </Button>
          </>
        )}

        {activeFilter !== REMAINING_DEBTS_FILTER && (
          <Chip
            size="small"
            color="primary"
            variant="outlined"
            label={`Lọc: ${activeFilter.from} → ${String(activeFilter.to).slice(0, 10)}`}
            sx={{ alignSelf: "center" }}
          />
        )}
        <TextField
          size="small"
          placeholder="Tìm theo tên người chơi..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          sx={{ minWidth: { md: 200 }, maxWidth:{md:300}, flex: { md: "1 1 280px" } }}
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
        <Button
          variant="contained"
          color="success"
          size="small"
          startIcon={exportingPage ? <CircularProgress size={16} color="inherit" /> : undefined}
          disabled={exportingPage || loading || summariesLoading || remoteSearching || totalRows === 0}
          onClick={() => handleExport()}
          sx={{ whiteSpace: "nowrap" }}
        >
          {exportingPage ? "Đang xuất..." : "Xuất Excel"}
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ display: "flex", alignItems: "center", gap: 1 }}
        >
          {isHistory ? "Số người từng nợ:" : "Tổng số người nợ:"}{" "}
          <strong>{totalRows}</strong>
          {summariesLoading && <CircularProgress size={12} />}
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
              {isHistory && (
                <TableCell align="center" sx={{ width: 110 }}>
                  Đã trả / Tổng
                </TableCell>
              )}
              <TableCell align="center" sx={{ width: 90 }}>Số khoản</TableCell>
              <TableCell align="center" sx={{ width: 110 }}>
                {!isHistory && "Hành động"}
              </TableCell>
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
            {!loading && !error && pageRows.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={COL_COUNT}
                  align="center"
                  sx={{ py: 4, color: "text.secondary" }}
                >
                  {summariesLoading || remoteSearching ? (
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: 1,
                      }}
                    >
                      <CircularProgress size={16} /> Đang tải dữ liệu nợ...
                    </Box>
                  ) : isHistory ? (
                    "Không có người từng nợ nào."
                  ) : (
                    "Không có người nợ nào."
                  )}
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              !error &&
              pageRows.map((row, index) => {
                const open = expandedRows.has(row.playerName);
                const summary = summaries[row.playerName];
                const canPayAll = (summary?.totalDebts?.amount || 0) > 0;
                const selectedCount = (selections[row.playerName] || new Set()).size;
                return (
                  <React.Fragment key={row.playerName}>
                    {/* Fade animates the tr itself — no wrapper node, so table
                        semantics stay valid while streamed rows ease in */}
                    <Fade in timeout={300}>
                      <TableRow
                        ref={setRowEl(row.playerName)}
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
                          {(page - 1) * query.pageSize + index + 1}
                        </TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>{row.playerName}</TableCell>
                        {isHistory && (
                          <TableCell align="center">
                            {renderSummaryCell(row.playerName, "ratio")}
                          </TableCell>
                        )}
                        <TableCell align="center">
                          {renderSummaryCell(row.playerName, "count")}
                        </TableCell>
                        <TableCell align="center">
                          {!isHistory && (
                            <Button
                              variant="contained"
                              color="warning"
                              size="small"
                              disabled={selectedCount === 0 && !canPayAll}
                              onClick={(e) => {
                                e.stopPropagation();
                                openDebitDialog(row.playerName);
                              }}
                            >
                              {selectedCount > 0 ? `Thu nợ (${selectedCount})` : "Thu nợ"}
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    </Fade>
                    <TableRow>
                      <TableCell colSpan={COL_COUNT} sx={{ py: 0 }}>
                        <Collapse in={open} timeout="auto" unmountOnExit>
                          <Box sx={{ py: 1, px: { xs: 0, sm: 2 } }}>
                            {isHistory || historyView.has(row.playerName)
                              ? renderHistoryDetail(row.playerName)
                              : renderDetail(row.playerName)}
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
            page={page}
            onChange={(_, page) => setQuery((q) => ({ ...q, page }))}
            color="primary"
            showFirstButton
            showLastButton
          />
        )}
      </Stack>

      <DebitListDialog
        show={debitDialog.show}
        playerName={debitDialog.playerName}
        preselectedDebits={debitDialog.preselected}
        onClose={() => setDebitDialog((d) => ({ ...d, show: false }))}
        onPaid={() => {
          setSelections((s) => ({ ...s, [debitDialog.playerName]: new Set() }));
          refreshPlayer(debitDialog.playerName);
        }}
      />
      <DebitListDialog
        show={historyDialog.show}
        playerName={historyDialog.playerName}
        historyMode
        onClose={() => setHistoryDialog((d) => ({ ...d, show: false }))}
      />
    </Box>
  );
}
