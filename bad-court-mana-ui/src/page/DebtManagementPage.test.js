import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
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
    sortField: "PLAYER_NAME",
    sortDirection: "ASC",
  }));
  expect(window.URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
  expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:debt-report");
});

test("exports one player from the expanded history panel", async () => {
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

  await waitFor(() => expect(exportDebtReport).toHaveBeenCalledTimes(1));
  expect(exportDebtReport).toHaveBeenCalledWith(expect.objectContaining({
    mode: "HISTORY",
    scope: "PLAYER",
    playerName: "An",
    playerNameFilter: null,
  }));
});
