import {
  listDebtPlayersMock as listDebtPlayers,
  getDebitSummaryMock as getDebitSummary,
  listRemainingDebtsMock as listRemainingDebts,
  payDebitsMock as payDebits,
} from "./debtApi";

// Avoid loading axios (ESM) — only the mock paths are exercised here.
jest.mock("./index", () => ({
  __esModule: true,
  default: { post: jest.fn(), get: jest.fn() },
}));

test("debtor list returns paginated PageResponse of distinct players", async () => {
  const res = await listDebtPlayers({
    pagination: { current: 1, pageSize: 10 },
    filter: {},
    sort: { field: "playerName", direction: "ASC" },
  });
  const page = res.data.data;
  expect(page.pagination.current).toBe(1);
  expect(page.pagination.pageSize).toBe(10);
  expect(page.list).toHaveLength(10);
  expect(page.total).toBeGreaterThan(10);
  // distinct names, ASC sorted
  const names = page.list.map((p) => p.playerName);
  expect(new Set(names).size).toBe(names.length);
  expect([...names].sort((a, b) => a.localeCompare(b, "vi"))).toEqual(names);
});

test("debtor list filters by player name", async () => {
  const res = await listDebtPlayers({
    pagination: { current: 1, pageSize: 50 },
    filter: { playerName: "an" },
    sort: { field: "playerName", direction: "ASC" },
  });
  const page = res.data.data;
  expect(page.list.length).toBeGreaterThan(0);
  page.list.forEach((p) =>
    expect(p.playerName.toLowerCase()).toContain("an")
  );
  expect(page.total).toBe(page.list.length);
});

test("debit summary matches unpaid mock data", async () => {
  const res = await getDebitSummary("An");
  const s = res.data;
  expect(s.playerName).toBe("An");
  expect(s.totalDebts.currency).toBe("VND");
  expect(s.numberDebit).toBeGreaterThanOrEqual(0);
});

test("remaining debts returns only that player's debits, DESC", async () => {
  const res = await listRemainingDebts("An", { current: 1, pageSize: 50 }, {});
  const body = res.data;
  expect(body.playerName).toBe("An");
  expect(body.debitSummary.numberDebit).toBeGreaterThanOrEqual(0);
  const debits = body.remainingDebits;
  expect(debits.length).toBeGreaterThan(0);
  for (let i = 1; i < debits.length; i++) {
    expect(debits[i - 1].dateTime >= debits[i].dateTime).toBe(true);
  }
  debits.forEach((d) => expect(d.money.currency).toBe("VND"));
});

test("pay all debts with only totalPayAmount (no listDebitPay)", async () => {
  const before = (await getDebitSummary("An")).data;
  const res = await payDebits({
    playerName: "An",
    totalPayAmount: before.totalDebts.amount,
    paymentMethod: "CASH",
  });
  const data = res.data;
  expect(data.paidDebts).toBe(before.totalDebts.amount);
  expect(data.remainingDebts).toBe(0);
  expect(data.status).toBe("SUCCESS");

  const after = (await getDebitSummary("An")).data;
  expect(after.numberDebit).toBe(0);
  expect(after.totalDebts.amount).toBe(0);
});

test("pay selected debts via listDebitPay", async () => {
  const debts = (await listRemainingDebts("Bình", { current: 1, pageSize: 50 }, {}))
    .data.remainingDebits.filter((d) => d.money.amount > 0);
  expect(debts.length).toBeGreaterThan(0);

  const picked = debts[0];
  const res = await payDebits({
    playerName: "Bình",
    totalPayAmount: picked.money.amount,
    paymentMethod: "CASH",
    listDebitPay: [{ dateTime: picked.dateTime, payAmount: picked.money.amount }],
  });
  const data = res.data;
  expect(data.numPaidDebts).toBe(1);
  expect(data.paidDebts).toBe(picked.money.amount);

  // the paid debit now shows remaining 0 in the refreshed list
  const after = (await listRemainingDebts("Bình", { current: 1, pageSize: 50 }, {}))
    .data.remainingDebits.find((d) => d.dateTime === picked.dateTime);
  expect(after.money.amount).toBe(0);
});
