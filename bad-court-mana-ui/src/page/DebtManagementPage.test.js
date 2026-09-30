import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import DebtManagementPage from "./DebtManagementPage";
import {
  listAllPlayers,
  getDebitSummary,
  listRemainingDebts,
  getDebitHistorySummary,
  listDebitHistory,
  exportDebtReport,
} from "../api/debtApi";

jest.mock("../api/debtApi", () => ({
  listAllPlayers: jest.fn(),
  getDebitSummary: jest.fn(),
  listRemainingDebts: jest.fn(),
  getDebitHistorySummary: jest.fn(),
  listDebitHistory: jest.fn(),
  exportDebtReport: jest.fn(),
  REMAINING_DEBTS_PAGE: { current: 1, pageSize: 50, totalPage: 0 },
  REMAINING_DEBTS_FILTER: { from: "2000-01-01", to: "9999-12-31" },
}));

jest.mock("./dialog/DebitListDialog", () => () => null);

beforeEach(() => {
  jest.clearAllMocks();
  if (!Element.prototype.animate) Element.prototype.animate = jest.fn();
  const players = [
    { playerId: 1, playerName: "An" },
    { playerId: 2, playerName: "Binh" },
  ];
  listAllPlayers.mockResolvedValue({ data: { success: true, data: players } });
  getDebitSummary.mockResolvedValue({
    data: {
      success: true,
      data: { playerName: "An", totalDebts: { amount: 100, currency: "VND" }, numberDebit: 2 },
    },
  });
  getDebitHistorySummary.mockResolvedValue({
    data: {
      success: true,
      data: { playerName: "An", totalDebitAmount: 500, numDebits: 4, numPaidDebits: 2, numUnpaidDebits: 2 },
    },
  });
  listRemainingDebts.mockResolvedValue({ data: { success: true, data: { remainingDebits: [] } } });
  listDebitHistory.mockResolvedValue({ data: { success: true, data: { list: [], total: 0, totalPage: 0 } } });
  exportDebtReport.mockResolvedValue({
    data: new Blob(["xlsx"]),
    headers: { "content-disposition": 'attachment; filename="debt.xlsx"' },
  });
  window.URL.createObjectURL = jest.fn(() => "blob:debt-report");
  window.URL.revokeObjectURL = jest.fn();
  jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => { });
});

test("switching to Lịch sử loads history only for players shown in current mode", async () => {
  // An has unpaid debits, Binh has none → only An is listed in current mode
  getDebitSummary.mockImplementation((name) =>
    Promise.resolve({
      data: {
        success: true,
        data: {
          playerName: name,
          totalDebts: { amount: name === "An" ? 100 : 0, currency: "VND" },
          numberDebit: name === "An" ? 2 : 0,
        },
      },
    })
  );
  render(<DebtManagementPage />);

  // current-mode stream settles: only An ends up displayed
  await screen.findByText("An");
  await waitFor(() => expect(getDebitSummary).toHaveBeenCalledTimes(2));
  expect(getDebitHistorySummary).not.toHaveBeenCalled();

  await userEvent.click(screen.getByRole("button", { name: "Lịch sử" }));

  await waitFor(() => expect(getDebitHistorySummary).toHaveBeenCalledTimes(1));
  expect(getDebitHistorySummary).toHaveBeenCalledWith("An");
  expect(getDebitHistorySummary).not.toHaveBeenCalledWith("Binh");
});

test("history stream still starts when the tab is clicked before the roster lands", async () => {
  let resolvePlayers;
  listAllPlayers.mockImplementation(
    () => new Promise((res) => (resolvePlayers = res))
  );
  render(<DebtManagementPage />);

  // Click history while the roster request is still in flight
  await userEvent.click(screen.getByRole("button", { name: "Lịch sử" }));
  expect(getDebitHistorySummary).not.toHaveBeenCalled();

  // Roster arrives → the stream starts on its own
  resolvePlayers({
    data: { success: true, data: [{ playerId: 1, playerName: "An" }] },
  });
  await waitFor(() => expect(getDebitHistorySummary).toHaveBeenCalledWith("An"));
});

test("retry in history mode reloads a failed roster and starts the stream", async () => {
  listAllPlayers.mockRejectedValueOnce(new Error("boom"));
  render(<DebtManagementPage />);

  await userEvent.click(screen.getByRole("button", { name: "Lịch sử" }));
  await screen.findByText("Không tải được danh sách nợ. Vui lòng thử lại.");
  expect(getDebitHistorySummary).not.toHaveBeenCalled();

  await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
  await waitFor(() => expect(listAllPlayers).toHaveBeenCalledTimes(2));
  await waitFor(() => expect(getDebitHistorySummary).toHaveBeenCalledTimes(2));
});

test("exports all filtered players in current mode", async () => {
  render(<DebtManagementPage />);

  await screen.findByText("An");
  await waitFor(() => expect(screen.getByRole("button", { name: "Xuất Excel" })).toBeEnabled());
  await userEvent.type(screen.getByPlaceholderText("Tìm theo tên người chơi..."), "An");
  await waitFor(() => expect(screen.queryByText("Binh")).not.toBeInTheDocument(), { timeout: 1500 });
  await userEvent.click(screen.getByRole("button", { name: "Xuất Excel" }));

  await waitFor(() => expect(exportDebtReport).toHaveBeenCalledTimes(1));
  expect(exportDebtReport).toHaveBeenCalledWith(expect.objectContaining({
    mode: "CURRENT",
    scope: "ALL_PLAYERS",
    playerName: null,
    playerNameFilter: "An",
    from: "2000-01-01",
    to: "9999-12-31",
    sortField: "PLAYER_NAME",
    sortDirection: "ASC",
  }));
  expect(window.URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
  expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:debt-report");
});

test("history mode: Tất cả scans the whole roster via /summaryHistory, Lọc filters via /history", async () => {
  // Only An currently owes money → the entry seed stream covers An alone.
  getDebitSummary.mockImplementation((name) =>
    Promise.resolve({
      data: {
        success: true,
        data: {
          playerName: name,
          totalDebts: { amount: name === "An" ? 100 : 0, currency: "VND" },
          numberDebit: name === "An" ? 2 : 0,
        },
      },
    })
  );
  getDebitHistorySummary.mockImplementation((name) =>
    Promise.resolve({
      data: {
        success: true,
        data: {
          playerName: name,
          numDebits: name === "An" ? 4 : 1,
          numPaidDebits: 1,
          numUnpaidDebits: name === "An" ? 3 : 0,
          totalDebitAmount: 500,
        },
      },
    })
  );
  // Under the date range only An has a history item.
  listDebitHistory.mockImplementation((name) =>
    Promise.resolve({
      data: {
        success: true,
        data: {
          list:
            name === "An"
              ? [{ debtAmount: 100, debtDateTime: "2026-09-05 10:00:00", paidAmount: 0, remainingAmount: 100, status: "PENDING", currency: "VND", note: "" }]
              : [],
          total: name === "An" ? 1 : 0,
          pagination: { totalPage: 1 },
        },
      },
    })
  );
  render(<DebtManagementPage />);

  await screen.findByText("An");

  // The date controls only render in history mode
  expect(screen.queryByLabelText("Từ ngày")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tất cả" })).not.toBeInTheDocument();

  await userEvent.click(screen.getByRole("button", { name: "Lịch sử" }));
  // Seed stream: only the current debtor (An) is history-fetched on entry
  await waitFor(() => expect(getDebitHistorySummary).toHaveBeenCalledWith("An"));
  expect(getDebitHistorySummary).not.toHaveBeenCalledWith("Binh");

  // "Tất cả" = full history scan: roster reload + /summaryHistory per player
  await userEvent.click(screen.getByRole("button", { name: "Tất cả" }));
  await waitFor(() => expect(listAllPlayers).toHaveBeenCalledTimes(2));
  await waitFor(() => expect(getDebitHistorySummary).toHaveBeenCalledWith("Binh"));
  await screen.findByText("Binh");

  // "Lọc" scans the roster through /history with the range;
  // "Đến ngày" is pushed to end-of-day so the picked date is included.
  fireEvent.change(screen.getByLabelText("Từ ngày"), { target: { value: "2026-09-01" } });
  fireEvent.change(screen.getByLabelText("Đến ngày"), { target: { value: "2026-09-30" } });
  await userEvent.click(screen.getByRole("button", { name: "Lọc" }));

  const expectedFilter = { from: "2026-09-01", to: "2026-09-30T23:59:59", amountFrom: 0, amountTo: 0 };
  await waitFor(() => expect(listDebitHistory).toHaveBeenCalledTimes(2));
  expect(listDebitHistory).toHaveBeenCalledWith("An", { current: 1, pageSize: 100, totalPage: 0 }, expectedFilter);
  expect(listDebitHistory).toHaveBeenCalledWith("Binh", { current: 1, pageSize: 100, totalPage: 0 }, expectedFilter);
  await waitFor(() => expect(screen.queryByText("Binh")).not.toBeInTheDocument());
  expect(screen.getByText("An")).toBeInTheDocument();
});

test("exports the visible history table locally without calling the report API", async () => {
  listDebitHistory.mockResolvedValue({
    data: {
      success: true,
      data: {
        list: [
          {
            debtAmount: 100,
            debtDateTime: "2026-09-26 10:00:00",
            paidAmount: 0,
            remainingAmount: 100,
            currency: "VND",
            status: "PENDING",
            note: "no",
          },
        ],
        total: 1,
        pagination: { totalPage: 1 },
      },
    },
  });
  render(<DebtManagementPage />);

  await screen.findByText("An");
  await userEvent.click(screen.getByRole("button", { name: "Lịch sử" }));

  // expand An's row — the per-player export lives in the panel header
  await userEvent.click(await screen.findByText("An"));
  await screen.findByRole("button", { name: "Xuất Excel An" });
  await userEvent.click(screen.getByRole("button", { name: "Xuất Excel An" }));

  expect(exportDebtReport).not.toHaveBeenCalled();
  const historyBlob = window.URL.createObjectURL.mock.calls[0][0];
  expect(historyBlob.type).toBe("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  expect(historyBlob.size).toBeGreaterThan(1000);
  expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:debt-report");
});

test("exports the visible current-debt table locally without calling the report API", async () => {
  listRemainingDebts.mockResolvedValue({
    data: {
      success: true,
      data: {
        remainingDebits: [
          {
            dateTime: "2026-09-26T10:00:00Z",
            money: { amount: 100, currency: "VND" },
            note: "ghi nợ",
          },
        ],
      },
    },
  });
  render(<DebtManagementPage />);

  await screen.findByText("An");
  await userEvent.click(screen.getByText("An"));
  await screen.findByText("ghi nợ");
  await userEvent.click(screen.getByRole("button", { name: "Xuất Excel An" }));

  expect(exportDebtReport).not.toHaveBeenCalled();
  const currentBlob = window.URL.createObjectURL.mock.calls.at(-1)[0];
  expect(currentBlob.type).toBe("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  expect(currentBlob.size).toBeGreaterThan(1000);
  expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:debt-report");
});
