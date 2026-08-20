import test from "node:test";
import assert from "node:assert/strict";
import {
  isSupabaseConfigured,
  mergeSnapshots,
  snapshotFingerprint
} from "../src/supabaseSync.js";

test("builds the same fingerprint regardless of object key order", () => {
  const first = {
    users: [{ userId: "U01", name: "Ana", isActive: true }],
    settings: { igvRate: 0.18, reminderDay: 25 }
  };
  const second = {
    settings: { reminderDay: 25, igvRate: 0.18 },
    users: [{ isActive: true, name: "Ana", userId: "U01" }]
  };

  assert.equal(snapshotFingerprint(first), snapshotFingerprint(second));
});

test("merges cloud and device records with device priority", () => {
  const merged = mergeSnapshots(
    {
      users: [
        { userId: "U01", name: "Nombre nube" },
        { userId: "U02", name: "Solo nube" }
      ],
      receipts: [{ period: "2026-05", monthlyBill: 90 }]
    },
    {
      users: [{ userId: "U01", name: "Nombre local" }],
      receipts: [{ period: "2026-06", monthlyBill: 100 }]
    }
  );

  assert.equal(merged.users.length, 2);
  assert.equal(merged.users.find((user) => user.userId === "U01").name, "Nombre local");
  assert.deepEqual(
    merged.receipts.map((receipt) => receipt.period).sort(),
    ["2026-05", "2026-06"]
  );
});

test("requires an HTTPS project URL and a public key", () => {
  assert.equal(
    isSupabaseConfigured({
      url: "https://project.supabase.co",
      anonKey: "sb_publishable_abcdefghijklmnopqrstuvwxyz"
    }),
    true
  );
  assert.equal(isSupabaseConfigured({ url: "http://localhost", anonKey: "short" }), false);
});
