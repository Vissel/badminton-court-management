import api from "./index";
import {
  listAllPlayers,
  createDebit,
  getDebitSummary,
  listRemainingDebts,
  payDebits,
  REMAINING_DEBTS_PAGE,
  REMAINING_DEBTS_FILTER,
} from "./debtApi";

jest.mock("./index", () => ({
  __esModule: true,
  default: {
    get: jest.fn(() => Promise.resolve({ data: {} })),
    post: jest.fn(() => Promise.resolve({ data: {} })),
  },
}));

beforeEach(() => jest.clearAllMocks());

test("listAllPlayers calls GET /api/v1/player/all", async () => {
  await listAllPlayers();
  expect(api.get).toHaveBeenCalledWith("/api/v1/player/all");
});

test("getDebitSummary calls GET /api/v1/debit/summary with encoded name and silences error toasts", async () => {
  await getDebitSummary("Nguyễn Văn An");
  expect(api.get).toHaveBeenCalledWith(
    `/api/v1/debit/summary?playerName=${encodeURIComponent("Nguyễn Văn An")}`,
    { skipErrorToast: true }
  );
});

test("createDebit posts the debit payload", async () => {
  await createDebit({
    playerName: "An",
    debitAmount: 30000,
    note: "Tiền sân",
    createdTime: "2026-09-24T10:00:00",
  });
  expect(api.post).toHaveBeenCalledWith("/api/v1/debit/create", {
    playerName: "An",
    debitAmount: 30000,
    currency: "VND",
    note: "Tiền sân",
    createdTime: "2026-09-24T10:00:00",
  });
});

test("listRemainingDebts posts playerNames with default pagination/filter", async () => {
  await listRemainingDebts("An");
  expect(api.post).toHaveBeenCalledWith("/api/v1/debit/listRemainingDebts", {
    playerNames: ["An"],
    pagination: REMAINING_DEBTS_PAGE,
    filter: REMAINING_DEBTS_FILTER,
  });
});

test("payDebits omits listDebitPay for pay-all", async () => {
  await payDebits({ playerName: "An", totalPayAmount: 50000 });
  expect(api.post).toHaveBeenCalledWith("/api/v1/debit/pay", {
    playerName: "An",
    totalPayAmount: 50000,
    paymentMethod: "CASH",
    note: "",
  });
});

test("payDebits includes listDebitPay for selected debts", async () => {
  const listDebitPay = [{ dateTime: "2026-09-20 19:30:00", payAmount: 20000 }];
  await payDebits({ playerName: "An", totalPayAmount: 20000, listDebitPay });
  expect(api.post).toHaveBeenCalledWith("/api/v1/debit/pay", {
    playerName: "An",
    totalPayAmount: 20000,
    paymentMethod: "CASH",
    note: "",
    listDebitPay,
  });
});
