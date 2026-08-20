import Papa from "papaparse";
import { createInitialData } from "./domain";

export function downloadJson(data) {
  const blob = new Blob(
    [
      JSON.stringify(
        {
          format: "control_electrico_backup",
          version: 3,
          createdAt: new Date().toISOString(),
          ...data
        },
        null,
        2
      )
    ],
    { type: "application/json" }
  );
  downloadBlob(blob, `ControlElectrico_${timestamp()}.json`);
}

export async function downloadEncryptedJson(data, password) {
  if (!password || password.length < 6) {
    throw new Error("La clave debe tener al menos 6 caracteres.");
  }
  const payload = JSON.stringify(
    {
      format: "control_electrico_backup",
      version: 4,
      createdAt: new Date().toISOString(),
      ...data
    },
    null,
    2
  );
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await deriveKey(password, salt);
  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    new TextEncoder().encode(payload)
  );
  const wrapped = {
    format: "control_electrico_encrypted_backup",
    version: 1,
    algorithm: "AES-GCM",
    kdf: "PBKDF2-SHA256",
    iterations: 180000,
    createdAt: new Date().toISOString(),
    salt: bytesToBase64(salt),
    iv: bytesToBase64(iv),
    payload: bytesToBase64(new Uint8Array(encrypted))
  };
  downloadBlob(
    new Blob([JSON.stringify(wrapped, null, 2)], { type: "application/json" }),
    `ControlElectrico_protegido_${timestamp()}.json`
  );
}

export function downloadCsv(data) {
  const rows = [["seccion", "campo_1", "campo_2", "campo_3", "campo_4", "campo_5", "campo_6", "campo_7", "campo_8", "campo_9", "campo_10", "campo_11", "campo_12"]];
  data.users.forEach((user) =>
    rows.push([
      "usuario",
      user.userId,
      user.name,
      user.internalMeter,
      user.isActive,
      user.isResidual,
      (user.periodStates || [])
        .map((state) => `${state.period}:${state.isActive}:${state.isResidual}`)
        .join("|"),
      user.notes || ""
    ])
  );
  data.receipts.forEach((receipt) =>
    rows.push([
      "recibo",
      receipt.period,
      receipt.externalReadingDate,
      receipt.supplyNumber,
      receipt.externalKwh,
      receipt.monthlyBill,
      receipt.priceKwhUpTo30,
      receipt.priceKwhOver30,
      receipt.fixedCharge,
      receipt.maintenance,
      receipt.publicLighting,
      receipt.ruralElectrification,
      receipt.notes || ""
    ])
  );
  data.readings.forEach((reading) =>
    rows.push([
      "lectura",
      reading.id,
      reading.period,
      reading.userId,
      reading.isResidual,
      reading.internalReadingDate,
      reading.previousReading ?? "",
      reading.currentReading ?? "",
      reading.notes || ""
    ])
  );
  data.services.forEach((service) =>
    rows.push([
      "servicio",
      service.id,
      service.period,
      service.name,
      service.amount,
      service.isActive,
      service.splitCost,
      service.participantCount,
      (service.participantUserIds || []).join("|"),
      service.notes || ""
    ])
  );
  data.payments.forEach((payment) =>
    rows.push([
      "pago",
      payment.id,
      payment.period,
      payment.userId,
      payment.status,
      payment.amountPaid,
      payment.paymentDate,
      payment.notes || ""
    ])
  );
  downloadBlob(
    new Blob([Papa.unparse(rows)], { type: "text/csv;charset=utf-8" }),
    `ControlElectrico_${timestamp()}.csv`
  );
}

export async function importBackup(file, password = "") {
  const text = await file.text();
  if (file.name.toLowerCase().endsWith(".json") || text.trimStart().startsWith("{")) {
    const rawParsed = JSON.parse(text);
    const parsed = rawParsed.format === "control_electrico_encrypted_backup"
      ? JSON.parse(await decryptBackup(rawParsed, password))
      : rawParsed;
    return {
      ...createInitialData(),
      users: parsed.users || [],
      receipts: parsed.receipts || [],
      readings: parsed.readings || [],
      services: parsed.services || [],
      payments: parsed.payments || [],
      auditEvents: parsed.auditEvents || [],
      settings: { ...createInitialData().settings, ...(parsed.settings || {}) }
    };
  }
  const result = Papa.parse(text, { skipEmptyLines: true });
  const data = createInitialData();
  result.data.slice(1).forEach((row) => {
    const section = String(row[0] || "").toLowerCase();
    if (section === "usuario") {
      data.users.push({
        userId: row[1],
        name: row[2],
        internalMeter: row[3],
        isActive: booleanValue(row[4], true),
        isResidual: booleanValue(row[5], false),
        periodStates: String(row[6] || "")
          .split("|")
          .filter(Boolean)
          .map((token) => {
            const [period, isActive, isResidual] = token.split(":");
            return { period, isActive: booleanValue(isActive, true), isResidual: booleanValue(isResidual, false) };
          }),
        notes: row[7] || ""
      });
    } else if (section === "recibo") {
      data.receipts.push({
        period: row[1],
        externalReadingDate: row[2],
        supplyNumber: row[3],
        externalKwh: numberValue(row[4]),
        monthlyBill: numberValue(row[5]),
        priceKwhUpTo30: numberValue(row[6]),
        priceKwhOver30: numberValue(row[7]),
        fixedCharge: numberValue(row[8]),
        maintenance: numberValue(row[9]),
        publicLighting: numberValue(row[10]),
        ruralElectrification: numberValue(row[11]),
        notes: row[12] || ""
      });
    } else if (section === "lectura") {
      data.readings.push({
        id: row[1],
        period: row[2],
        userId: row[3],
        isResidual: booleanValue(row[4], false),
        internalReadingDate: row[5],
        previousReading: nullableNumber(row[6]),
        currentReading: nullableNumber(row[7]),
        notes: row[8] || ""
      });
    } else if (section === "servicio") {
      data.services.push({
        id: row[1],
        period: row[2],
        name: row[3],
        amount: numberValue(row[4]),
        isActive: booleanValue(row[5], true),
        splitCost: booleanValue(row[6], true),
        participantCount: Math.max(Number(row[7]) || 1, 1),
        participantUserIds: String(row[8] || "").split("|").filter(Boolean),
        notes: row[9] || ""
      });
    } else if (section === "pago") {
      data.payments.push({
        id: row[1],
        period: row[2],
        userId: row[3],
        status: row[4],
        amountPaid: numberValue(row[5]),
        paymentDate: row[6],
        notes: row[7] || ""
      });
    }
  });
  return data;
}

async function decryptBackup(payload, password) {
  if (!password) throw new Error("Este respaldo está protegido. Escribe la clave antes de importarlo.");
  const salt = base64ToBytes(payload.salt);
  const iv = base64ToBytes(payload.iv);
  const encrypted = base64ToBytes(payload.payload || payload.data);
  const key = await deriveKey(password, salt);
  try {
    const decrypted = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, encrypted);
    return new TextDecoder().decode(decrypted);
  } catch {
    throw new Error("No se pudo descifrar el respaldo. Revisa la clave.");
  }
}

async function deriveKey(password, salt) {
  const material = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveKey"]
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt, iterations: 180000, hash: "SHA-256" },
    material,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  );
}

function bytesToBase64(bytes) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary);
}

function base64ToBytes(value) {
  return Uint8Array.from(atob(value), (char) => char.charCodeAt(0));
}

function booleanValue(value, fallback) {
  const text = String(value || "").toLowerCase();
  if (["true", "1", "si", "sí", "yes"].includes(text)) return true;
  if (["false", "0", "no"].includes(text)) return false;
  return fallback;
}

function numberValue(value) {
  return Number(String(value || "0").replace(",", ".")) || 0;
}

function nullableNumber(value) {
  return String(value || "").trim() ? numberValue(value) : null;
}

function timestamp() {
  return new Date().toISOString().replaceAll(":", "-").slice(0, 19);
}

function downloadBlob(blob, name) {
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
