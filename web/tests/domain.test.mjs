import assert from "node:assert/strict";
import test from "node:test";
import { buildPaymentLedger, createInitialData, totalOutstandingForPeriod } from "../src/domain.js";

function result(userId, total) {
  return {
    userId,
    finalTotal: total,
    serviceShare: 0
  };
}

function dataWithPayments(payments) {
  return {
    ...createInitialData(),
    payments
  };
}

test("carries unregistered and partial debt into the latest period", () => {
  const summaries = new Map([
    ["2026-01", { results: [result("U01", 100)] }],
    ["2026-02", { results: [result("U01", 80)] }],
    ["2026-03", { results: [result("U01", 70)] }]
  ]);
  const data = dataWithPayments([
    {
      id: "2026-02|U01",
      period: "2026-02",
      userId: "U01",
      status: "PARTIAL",
      amountPaid: 50
    }
  ]);

  data.receipts = [...summaries.keys()].map((period) => ({ period }));
  data.users = [{ userId: "U01", name: "Usuario", isActive: true }];

  const amounts = new Map([
    ["2026-01", 100],
    ["2026-02", 80],
    ["2026-03", 70]
  ]);
  data.receipts = [...amounts].map(([period, monthlyBill]) => ({
    period,
    monthlyBill,
    externalKwh: 1,
    priceKwhUpTo30: monthlyBill / 1.18,
    priceKwhOver30: 0
  }));
  data.readings = [...amounts.keys()].map((period) => ({
    id: `${period}|U01`,
    period,
    userId: "U01",
    previousReading: 0,
    currentReading: 1
  }));
  data.settings.roundUpToTenth = false;

  const ledger = buildPaymentLedger([...amounts.keys()], data);
  const latest = ledger.get("2026-03|U01");

  assert.equal(latest.previousBalance, 130);
  assert.equal(latest.totalDue, 200);
  assert.equal(latest.remainingBalance, 200);
  assert.deepEqual(
    latest.previousDebtItems.map((item) => [item.period, item.remainingAmount]),
    [
      ["2026-01", 50],
      ["2026-02", 80]
    ]
  );
});

test("applies a partial payment to the oldest debt first", () => {
  const data = dataWithPayments([
    {
      id: "2026-02|U01",
      period: "2026-02",
      userId: "U01",
      status: "PARTIAL",
      amountPaid: 120
    }
  ]);
  data.users = [{ userId: "U01", name: "Usuario", isActive: true }];
  data.receipts = [
    { period: "2026-01", monthlyBill: 100, externalKwh: 1, priceKwhUpTo30: 100 / 1.18 },
    { period: "2026-02", monthlyBill: 80, externalKwh: 1, priceKwhUpTo30: 80 / 1.18 }
  ];
  data.readings = data.receipts.map(({ period }) => ({
    id: `${period}|U01`,
    period,
    userId: "U01",
    previousReading: 0,
    currentReading: 1
  }));
  data.settings.roundUpToTenth = false;

  const latest = buildPaymentLedger(["2026-01", "2026-02"], data).get("2026-02|U01");

  assert.equal(latest.remainingBalance, 60);
  assert.deepEqual(
    latest.outstandingDebtItems.map((item) => [item.period, item.remainingAmount]),
    [["2026-02", 60]]
  );
});

test("general outstanding total sums every user balance", () => {
  const results = [result("U01", 100), result("U02", 80)];
  const ledger = new Map([
    ["2026-02|U01", { remainingBalance: 30 }],
    ["2026-02|U02", { remainingBalance: 80 }]
  ]);

  assert.equal(totalOutstandingForPeriod("2026-02", results, ledger), 110);
});
