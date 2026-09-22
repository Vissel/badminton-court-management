import api from "./index";

/**
 * Debts management API.
 *
 * The page is built on three calls:
 *
 * 1) POST /api/v1/debit/list  (PROPOSED — backend not implemented yet; mirrors
 *    /api/v1/manager/reportList which returns Result<PageResponse<T>>).
 *    Returns a paginated list of DISTINCT players having debit records —
 *    never the full debit table, so it stays cheap at any data size.
 *
 *    Request:
 *    {
 *      "pagination": { "current": 1, "pageSize": 10 },
 *      "filter": { "playerName": "" },                          // substring match
 *      "sort": { "field": "playerName", "direction": "ASC" }    // playerName | lastDebitDate
 *    }
 *
 *    Response body (Result<PageResponse<PlayerDebtItem>>):
 *    {
 *      "success": true,
 *      "data": {
 *        "pagination": { "current": 1, "pageSize": 10, "totalPage": 3 },
 *        "total": 24,
 *        "list": [{ "playerName": "An", "lastDebitDate": "2026-09-20 19:30:00" }]
 *      }
 *    }
 *
 * 2) GET /api/v1/debit/summary?playerName=...   (existing)
 *    → DebitSummaryResponse { playerName, totalDebts: {amount, currency}, numberDebit }
 *
 * 3) POST /api/v1/debit/listRemainingDebts      (existing)
 *    Request: { playerNames: [name], pagination: {current,pageSize,totalPage},
 *               filter: {from, to, amountFrom, amountTo} }
 *    → GetRemainingDebtResponse { playerName, debitSummary,
 *        remainingDebits: [{ dateTime, money: {amount, currency}, note }] }
 *
 * 4) POST /api/v1/debit/pay                     (existing)
 *    Request: { playerName, totalPayAmount, paymentMethod, note, listDebitPay? }
 *    Two cases:
 *      - pay ALL debts  → only totalPayAmount (= player totalDebts), no listDebitPay
 *      - pay SELECTED   → listDebitPay = [{dateTime, payAmount}] per checked debt,
 *                         totalPayAmount = sum(payAmount)
 *    → PayDebitResponse { playerName, paymentAmount, paidDebts, remainingDebts,
 *        numPaidDebts, numRemainingDebts, paymentDate, status, message }
 */

export const DEBIT_STATUS = {
  PENDING: "PENDING",
  PARTIALLY_PAID: "PARTIALLY_PAID",
  PAID: "PAID",
};

export const PLAYER_SORT_FIELDS = ["playerName", "lastDebitDate"];

// Wide-open range: the management page wants all remaining debts, not just
// the current-year slice the pay dialog uses.
export const REMAINING_DEBTS_FILTER = {
  from: "2000-01-01",
  to: "9999-12-31",
  amountFrom: 0,
  amountTo: 0,
};

export const REMAINING_DEBTS_PAGE = { current: 1, pageSize: 50, totalPage: 0 };

// Preview the debts UI before the backend endpoint exists.
// Flip to false once /api/v1/debit/list is live — rows 2) and 3) above are real.
const USE_MOCK = true;
const MOCK_LATENCY_MS = 300;

const listDebtPlayersReal = (payload) =>
  api.post("/api/v1/debit/list", payload);

const getDebitSummaryReal = (playerName) =>
  api.get(`/api/v1/debit/summary?playerName=${encodeURIComponent(playerName)}`);

const listRemainingDebtsReal = (playerName, pagination, filter) =>
  api.post("/api/v1/debit/listRemainingDebts", {
    playerNames: [playerName],
    pagination: pagination || REMAINING_DEBTS_PAGE,
    filter: filter || REMAINING_DEBTS_FILTER,
  });

const payDebitsReal = ({ playerName, totalPayAmount, paymentMethod = "CASH", note = "", listDebitPay }) =>
  api.post("/api/v1/debit/pay", {
    playerName,
    totalPayAmount,
    paymentMethod,
    note,
    ...(listDebitPay && listDebitPay.length > 0 ? { listDebitPay } : {}),
  });

/* ---------------------------- mock implementation --------------------------- */

const MOCK_PLAYERS = [
  "An", "Bình", "Châu", "Dũng", "Giang", "Hải", "Hùng", "Khoa",
  "Lan", "Linh", "Minh", "Nam", "Ngọc", "Phong", "Quân", "Sơn",
  "Thảo", "Trang", "Tuấn", "Vy",
];
const MOCK_NOTES = [
  "Tiền sân", "Tiền cầu", "Nước uống", "Tiền sân + cầu", "Ăn uống", "",
];
const MOCK_STATUSES = [
  DEBIT_STATUS.PENDING,
  DEBIT_STATUS.PENDING,
  DEBIT_STATUS.PARTIALLY_PAID,
  DEBIT_STATUS.PAID,
];

// Deterministic PRNG so pagination is stable across calls.
const mulberry32 = (seed) => () => {
  seed |= 0;
  seed = (seed + 0x6d2b79f5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

const pad2 = (n) => String(n).padStart(2, "0");

const buildMockDebits = () => {
  const rand = mulberry32(20260922);
  const base = new Date(2026, 5, 1, 8, 0, 0).getTime(); // ~4 months of history
  const rows = [];
  for (let i = 0; i < 135; i++) {
    const status = MOCK_STATUSES[Math.floor(rand() * MOCK_STATUSES.length)];
    const debtAmount = (1 + Math.floor(rand() * 50)) * 10000; // 10k..500k
    const remainingAmount =
      status === DEBIT_STATUS.PAID
        ? 0
        : status === DEBIT_STATUS.PENDING
          ? debtAmount
          : Math.max(10000, Math.floor((debtAmount * rand()) / 10000) * 10000);
    const created = new Date(base + rand() * 110 * 24 * 3600 * 1000);
    rows.push({
      debitId: i + 1,
      playerName: MOCK_PLAYERS[Math.floor(rand() * MOCK_PLAYERS.length)],
      debtAmount,
      remainingAmount,
      currency: "VND",
      status,
      createdDate: `${created.getFullYear()}-${pad2(created.getMonth() + 1)}-${pad2(created.getDate())} ${pad2(created.getHours())}:${pad2(created.getMinutes())}:${pad2(created.getSeconds())}`,
      note: MOCK_NOTES[Math.floor(rand() * MOCK_NOTES.length)],
    });
  }
  return rows;
};

const MOCK_DEBITS = buildMockDebits();

const delay = (data) =>
  new Promise((resolve) => setTimeout(() => resolve(data), MOCK_LATENCY_MS));

const mockSummaryFor = (playerName) => {
  const unpaid = MOCK_DEBITS.filter(
    (d) => d.playerName === playerName && d.remainingAmount > 0
  );
  return {
    playerName,
    totalDebts: {
      amount: unpaid.reduce((s, d) => s + d.remainingAmount, 0),
      currency: "VND",
    },
    numberDebit: unpaid.length,
  };
};

const compareBy = (field, direction) => (a, b) => {
  const va = a[field];
  const vb = b[field];
  const cmp =
    typeof va === "number" && typeof vb === "number"
      ? va - vb
      : String(va ?? "").localeCompare(String(vb ?? ""), "vi");
  return direction === "ASC" ? cmp : -cmp;
};

// Exported so tests can pin the mock contract regardless of USE_MOCK.
export const listDebtPlayersMock = (payload = {}) => {
  const name = (payload.filter?.playerName || "").trim().toLowerCase();
  const byPlayer = new Map();
  MOCK_DEBITS.forEach((d) => {
    const prev = byPlayer.get(d.playerName);
    if (!prev || d.createdDate > prev.lastDebitDate) {
      byPlayer.set(d.playerName, {
        playerName: d.playerName,
        lastDebitDate: d.createdDate,
      });
    }
  });

  let players = [...byPlayer.values()];
  if (name) players = players.filter((p) => p.playerName.toLowerCase().includes(name));

  const field = PLAYER_SORT_FIELDS.includes(payload.sort?.field)
    ? payload.sort.field
    : "playerName";
  players.sort(compareBy(field, payload.sort?.direction === "DESC" ? "DESC" : "ASC"));

  const pageSize = payload.pagination?.pageSize || 10;
  const current = Math.max(1, payload.pagination?.current || 1);
  const total = players.length;
  const totalPage = Math.ceil(total / pageSize) || 1;

  return delay({
    data: {
      success: true,
      data: {
        pagination: { current, pageSize, totalPage },
        total,
        list: players.slice((current - 1) * pageSize, current * pageSize),
      },
    },
  });
};

export const getDebitSummaryMock = (playerName) =>
  delay({ data: mockSummaryFor(playerName) });

export const listRemainingDebtsMock = (playerName, pagination, filter = {}) => {
  const rows = MOCK_DEBITS.filter((d) => {
    if (d.playerName !== playerName) return false;
    const day = d.createdDate.slice(0, 10);
    if (filter.from && day < filter.from) return false;
    if (filter.to && day > filter.to) return false;
    return true;
  }).sort(compareBy("createdDate", "DESC")); // backend orders createdDate DESC

  const pageSize = pagination?.pageSize || 50;
  const current = Math.max(1, pagination?.current || 1);

  return delay({
    data: {
      playerName,
      debitSummary: mockSummaryFor(playerName),
      remainingDebits: rows
        .slice((current - 1) * pageSize, current * pageSize)
        .map((d) => ({
          dateTime: d.createdDate,
          money: { amount: d.remainingAmount, currency: d.currency },
          note: d.note,
        })),
    },
  });
};

// Server sends/matches "yyyy-MM-dd H:mm:ss" (hour not zero-padded) — normalize
// both sides the same way so listDebitPay matching works.
const normDT = (s) => {
  const [datePart, timePart] = String(s || "").split(" ");
  if (!timePart) return String(s || "");
  const [h, m, sec] = timePart.split(":").map(Number);
  return `${datePart} ${h}:${pad2(m)}:${pad2(sec)}`;
};

export const payDebitsMock = (request = {}) => {
  const unpaidAsc = MOCK_DEBITS.filter(
    (d) => d.playerName === request.playerName && d.remainingAmount > 0
  ).sort(compareBy("createdDate", "ASC")); // backend allocates oldest-first

  let targets;
  if (request.listDebitPay?.length) {
    // pay the specific selected debts
    targets = request.listDebitPay
      .map((item) => ({
        debit: unpaidAsc.find((d) => normDT(d.createdDate) === normDT(item.dateTime)),
        amount: item.payAmount || 0,
      }))
      .filter((t) => t.debit && t.amount > 0);
  } else {
    // pay-all: allocate totalPayAmount across unpaid debts, oldest first
    let left = request.totalPayAmount || 0;
    targets = [];
    for (const debit of unpaidAsc) {
      if (left <= 0) break;
      const applied = Math.min(debit.remainingAmount, left);
      targets.push({ debit, amount: applied });
      left -= applied;
    }
  }

  targets.forEach(({ debit, amount }) => {
    debit.remainingAmount = Math.max(0, debit.remainingAmount - amount);
    debit.status =
      debit.remainingAmount === 0 ? DEBIT_STATUS.PAID : DEBIT_STATUS.PARTIALLY_PAID;
  });

  const summary = mockSummaryFor(request.playerName);
  const paidTotal = targets.reduce((s, t) => s + t.amount, 0);
  const now = new Date();

  return delay({
    data: {
      playerName: request.playerName,
      paymentAmount: request.totalPayAmount || 0,
      paymentMethod: request.paymentMethod || "CASH",
      paidDebts: paidTotal,
      remainingDebts: summary.totalDebts.amount,
      numPaidDebts: targets.length,
      numRemainingDebts: summary.numberDebit,
      paymentDate: `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())} ${now.getHours()}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())}`,
      status: summary.numberDebit === 0 ? "SUCCESS" : "PARTIAL",
      message:
        summary.numberDebit === 0
          ? "Payment allocation completed successfully"
          : "Payment allocation completed. Some debts are still remaining",
    },
  });
};

/* ------------------------------ public exports ------------------------------ */

export const listDebtPlayers = (payload) =>
  USE_MOCK ? listDebtPlayersMock(payload) : listDebtPlayersReal(payload);

export const getDebitSummary = (playerName) =>
  USE_MOCK ? getDebitSummaryMock(playerName) : getDebitSummaryReal(playerName);

export const listRemainingDebts = (playerName, pagination, filter) =>
  USE_MOCK
    ? listRemainingDebtsMock(playerName, pagination, filter)
    : listRemainingDebtsReal(playerName, pagination, filter);

/**
 * Pay debts for a player.
 * @param listDebitPay - omit/empty → backend pays all debts up to totalPayAmount;
 *                       otherwise [{dateTime, payAmount}] for the selected debts.
 */
export const payDebits = (request) =>
  USE_MOCK ? payDebitsMock(request) : payDebitsReal(request);
