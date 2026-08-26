import test from "node:test";
import assert from "node:assert/strict";
import { backupFromSheetRows, extractGoogleSheetId, sheetRowsForBackup } from "../src/googleSheetsBackup.js";
import { createInitialData } from "../src/domain.js";

test("round trips a large backup through Google Sheets rows", () => {
  const data = createInitialData();
  data.users = [
    {
      userId: "U01",
      name: "Usuario con notas largas",
      internalMeter: "M-01",
      isActive: true,
      notes: "x".repeat(60000)
    }
  ];
  data.receipts = [{ period: "2026-06", monthlyBill: 110.6, externalKwh: 139.2 }];

  const rows = sheetRowsForBackup(data);
  const restored = backupFromSheetRows(rows);

  assert.equal(rows.filter((row) => String(row[0]).startsWith("part_")).length > 1, true);
  assert.equal(restored.users[0].name, "Usuario con notas largas");
  assert.equal(restored.users[0].notes.length, 60000);
  assert.equal(restored.receipts[0].monthlyBill, 110.6);
});

test("extracts a Google Sheets id from a full URL or plain id", () => {
  const id = "1AbcDefGhijKlmnOPqrsTuvWxYz_1234567890";
  assert.equal(
    extractGoogleSheetId(`https://docs.google.com/spreadsheets/d/${id}/edit#gid=0`),
    id
  );
  assert.equal(extractGoogleSheetId(id), id);
});
