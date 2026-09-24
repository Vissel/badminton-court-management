import api from "./index";

/**
 * Debts management API.
 *
 * The page is built on four calls:
 *
 * 1) GET /api/v1/player/all
 *    → Result<List<PlayerResponse>>:
 *    { "success": true, "data": [{ "playerId": 1, "playerName": "An",
 *        "createdDate": "..." }] }
 *
 * 2) GET /api/v1/debit/summary?playerName=...   (per player)
 *    → Result<DebitSummaryResponse>:
 *    { "success": true, "data": { "playerName", "totalDebts": {amount,
 *        currency}, "numberDebit" } }
 *    Failure body (HTTP 400): { "success": false, "data": null,
 *        "errorMessage": "Player is not found", "errorCode": 103 }
 *
 * 3) POST /api/v1/debit/listRemainingDebts
 *    Request: { playerNames: [name], pagination: {current,pageSize,totalPage},
 *               filter: {from, to, amountFrom, amountTo} }
 *    → GetRemainingDebtResponse { playerName, debitSummary,
 *        remainingDebits: [{ dateTime, money: {amount, currency}, note }] }
 *
 * 4) POST /api/v1/debit/pay
 *    Request: { playerName, totalPayAmount, paymentMethod, note, listDebitPay? }
 *    Two cases:
 *      - pay ALL debts  → only totalPayAmount (= player totalDebts), no listDebitPay
 *      - pay SELECTED   → listDebitPay = [{dateTime, payAmount}] per checked debt,
 *                         totalPayAmount = sum(payAmount)
 *    → PayDebitResponse { playerName, paymentAmount, paidDebts, remainingDebts,
 *        numPaidDebts, numRemainingDebts, paymentDate, status, message }
 */

// Wide-open range: the management page wants all remaining debts, not just
// the current-year slice the pay dialog uses.
export const REMAINING_DEBTS_FILTER = {
  from: "2000-01-01",
  to: "9999-12-31",
  amountFrom: 0,
  amountTo: 0,
};

export const REMAINING_DEBTS_PAGE = { current: 1, pageSize: 50, totalPage: 0 };

export const listAllPlayers = () => api.get("/api/v1/player/all");

// POST /api/v1/debit/create → raw Boolean (unwrapped via convertToResponseEntity)
export const createDebit = ({ playerName, debitAmount, currency = "VND", note = "", createdTime }) =>
  api.post("/api/v1/debit/create", {
    playerName,
    debitAmount,
    currency,
    note,
    createdTime,
  });

// PLAYER_NOT_FOUND (103) is an expected per-player outcome — skipErrorToast
// keeps the axios interceptor from popping a global error for each one.
export const getDebitSummary = (playerName) =>
  api.get(`/api/v1/debit/summary?playerName=${encodeURIComponent(playerName)}`, {
    skipErrorToast: true,
  });

export const listRemainingDebts = (playerName, pagination, filter) =>
  api.post("/api/v1/debit/listRemainingDebts", {
    playerNames: [playerName],
    pagination: pagination || REMAINING_DEBTS_PAGE,
    filter: filter || REMAINING_DEBTS_FILTER,
  });

/**
 * Pay debts for a player.
 * @param listDebitPay - omit/empty → backend pays all debts up to totalPayAmount;
 *                       otherwise [{dateTime, payAmount}] for the selected debts.
 * Note: the current backend asserts listDebitPay non-empty, so pay-all needs
 * that assert relaxed (or a dedicated endpoint) before it works for real.
 */
export const payDebits = ({ playerName, totalPayAmount, paymentMethod = "CASH", note = "", listDebitPay }) =>
  api.post("/api/v1/debit/pay", {
    playerName,
    totalPayAmount,
    paymentMethod,
    note,
    ...(listDebitPay && listDebitPay.length > 0 ? { listDebitPay } : {}),
  });
