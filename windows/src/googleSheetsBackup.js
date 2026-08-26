import { buildBackupPayload, normalizeBackupPayload } from "./backup.js";

export const GOOGLE_SHEETS_CLIENT_ID =
  "21115880341-74tjtosms2c2urr7955f0g6ekeouhucm.apps.googleusercontent.com";

const GOOGLE_SCOPE = "https://www.googleapis.com/auth/drive.file";
const GOOGLE_IDENTITY_SCRIPT = "https://accounts.google.com/gsi/client";
const SPREADSHEET_TITLE = "Control Electrico";
const BACKUP_SHEET = "Respaldo_JSON";
const CHUNK_SIZE = 45000;

let googleIdentityPromise;
let tokenClient;
let accessToken = "";

export function googleSheetsEnvironmentStatus() {
  if (typeof window === "undefined") {
    return { supported: false, message: "Google Sheets requiere abrir la app en un navegador." };
  }
  const { protocol, hostname } = window.location;
  const supported =
    protocol === "https:" ||
    hostname === "localhost" ||
    hostname === "127.0.0.1";
  return {
    supported,
    message: supported
      ? ""
      : "Google OAuth requiere HTTPS o localhost. Para usar Google Sheets abre la web publicada o ejecuta la app web en local."
  };
}

export function googleSheetUrl(spreadsheetId) {
  return spreadsheetId
    ? `https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit`
    : "";
}

export function extractGoogleSheetId(value) {
  const text = String(value || "").trim();
  if (!text) return "";
  const urlMatch = text.match(/\/spreadsheets\/d\/([^/?#]+)/);
  if (urlMatch) return urlMatch[1];
  const idMatch = text.match(/[a-zA-Z0-9_-]{20,}/);
  return idMatch ? idMatch[0] : "";
}

export async function exportToGoogleSheets(data, existingSpreadsheetId = "") {
  ensureSupportedEnvironment();
  await requestAccessToken();
  const spreadsheetId = existingSpreadsheetId || await createSpreadsheet();
  await ensureSheets(spreadsheetId);
  await clearSheets(spreadsheetId);
  await writeSheets(spreadsheetId, data);
  return {
    spreadsheetId,
    url: googleSheetUrl(spreadsheetId),
    updatedAt: new Date().toISOString()
  };
}

export async function importFromGoogleSheets(spreadsheetId) {
  ensureSupportedEnvironment();
  if (!spreadsheetId) {
    throw new Error("Primero sube un respaldo para crear la hoja de Google Sheets.");
  }
  await requestAccessToken();
  const response = await googleApiFetch(
    `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range(BACKUP_SHEET, "A:B"))}`
  );
  const data = backupFromSheetRows(response.values || []);
  return {
    data,
    spreadsheetId,
    url: googleSheetUrl(spreadsheetId)
  };
}

export function sheetRowsForBackup(data) {
  const payload = buildBackupPayload(data);
  const json = JSON.stringify(payload);
  const chunks = chunkText(json, CHUNK_SIZE);
  return [
    ["clave", "valor"],
    ["format", payload.format],
    ["version", String(payload.version)],
    ["createdAt", payload.createdAt],
    ["parts", String(chunks.length)],
    ...chunks.map((chunk, index) => [`part_${index + 1}`, chunk])
  ];
}

export function backupFromSheetRows(rows) {
  const values = new Map((rows || []).map((row) => [String(row[0] || ""), String(row[1] || "")]));
  const partCount = Number(values.get("parts")) || 0;
  const chunks = [];
  if (partCount > 0) {
    for (let index = 1; index <= partCount; index += 1) {
      chunks.push(values.get(`part_${index}`) || "");
    }
  } else {
    [...values.entries()]
      .filter(([key]) => key.startsWith("part_"))
      .sort(([left], [right]) => Number(left.replace("part_", "")) - Number(right.replace("part_", "")))
      .forEach(([, value]) => chunks.push(value));
  }
  const json = chunks.join("");
  if (!json) {
    throw new Error("La hoja no contiene un respaldo compatible.");
  }
  return normalizeBackupPayload(JSON.parse(json));
}

async function createSpreadsheet() {
  const response = await googleApiFetch("https://sheets.googleapis.com/v4/spreadsheets", {
    method: "POST",
    body: JSON.stringify({
      properties: { title: SPREADSHEET_TITLE },
      sheets: [{ properties: { title: BACKUP_SHEET } }]
    })
  });
  return response.spreadsheetId;
}

async function ensureSheets(spreadsheetId) {
  const metadata = await googleApiFetch(
    `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}?fields=sheets.properties.title`
  );
  const existing = new Set((metadata.sheets || []).map((sheet) => sheet.properties?.title).filter(Boolean));
  const missing = readableSections().map((section) => section.title).filter((title) => !existing.has(title));
  if (!existing.has(BACKUP_SHEET)) missing.unshift(BACKUP_SHEET);
  if (!missing.length) return;
  await googleApiFetch(`https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}:batchUpdate`, {
    method: "POST",
    body: JSON.stringify({
      requests: missing.map((title) => ({ addSheet: { properties: { title } } }))
    })
  });
}

async function clearSheets(spreadsheetId) {
  await Promise.all(
    [BACKUP_SHEET, ...readableSections().map((section) => section.title)].map((title) =>
      googleApiFetch(
        `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range(title, "A:Z"))}:clear`,
        { method: "POST", body: "{}" }
      )
    )
  );
}

async function writeSheets(spreadsheetId, data) {
  const sheetData = [
    { range: range(BACKUP_SHEET, "A1:B"), values: sheetRowsForBackup(data) },
    ...readableSections(data).map((section) => ({
      range: range(section.title, "A1:Z"),
      values: rowsFromRecords(section.records, section.headers)
    }))
  ];
  await googleApiFetch(`https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values:batchUpdate`, {
    method: "POST",
    body: JSON.stringify({
      valueInputOption: "RAW",
      data: sheetData
    })
  });
}

function readableSections(data = {}) {
  return [
    {
      title: "Usuarios",
      records: data.users || [],
      headers: ["userId", "name", "internalMeter", "isActive", "isResidual", "periodStates", "notes"]
    },
    {
      title: "Recibos",
      records: data.receipts || [],
      headers: [
        "period",
        "externalReadingDate",
        "supplyNumber",
        "externalKwh",
        "monthlyBill",
        "priceKwhUpTo30",
        "priceKwhOver30",
        "fixedCharge",
        "maintenance",
        "publicLighting",
        "ruralElectrification",
        "notes"
      ]
    },
    {
      title: "Lecturas",
      records: data.readings || [],
      headers: [
        "id",
        "period",
        "userId",
        "isResidual",
        "internalReadingDate",
        "previousReading",
        "currentReading",
        "notes"
      ]
    },
    {
      title: "Servicios",
      records: data.services || [],
      headers: [
        "id",
        "period",
        "name",
        "amount",
        "isActive",
        "splitCost",
        "participantCount",
        "participantUserIds",
        "notes"
      ]
    },
    {
      title: "Pagos",
      records: data.payments || [],
      headers: ["id", "period", "userId", "status", "amountPaid", "paymentDate", "notes"]
    }
  ];
}

function rowsFromRecords(records, preferredHeaders) {
  const headers = [
    ...preferredHeaders,
    ...records.flatMap((record) => Object.keys(record || {})).filter((key) => !preferredHeaders.includes(key))
  ];
  const uniqueHeaders = [...new Set(headers)];
  return [
    uniqueHeaders,
    ...records.map((record) => uniqueHeaders.map((key) => stringifyCell(record?.[key])))
  ];
}

function stringifyCell(value) {
  if (value == null) return "";
  if (Array.isArray(value) || typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function range(sheet, address) {
  return `'${sheet.replaceAll("'", "''")}'!${address}`;
}

function chunkText(text, size) {
  const chunks = [];
  for (let index = 0; index < text.length; index += size) {
    chunks.push(text.slice(index, index + size));
  }
  return chunks.length ? chunks : [""];
}

function ensureSupportedEnvironment() {
  const status = googleSheetsEnvironmentStatus();
  if (!status.supported) throw new Error(status.message);
}

async function requestAccessToken() {
  if (accessToken) return accessToken;
  await loadGoogleIdentity();
  const google = window.google;
  if (!tokenClient) {
    tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_SHEETS_CLIENT_ID,
      scope: GOOGLE_SCOPE,
      callback: () => {}
    });
  }
  return new Promise((resolve, reject) => {
    tokenClient.callback = (response) => {
      if (response.error) {
        reject(new Error(response.error_description || response.error));
        return;
      }
      accessToken = response.access_token;
      resolve(accessToken);
    };
    tokenClient.error_callback = (error) => {
      reject(new Error(error?.message || "No se pudo iniciar sesión con Google."));
    };
    tokenClient.requestAccessToken({ prompt: "consent" });
  });
}

function loadGoogleIdentity() {
  if (window.google?.accounts?.oauth2) return Promise.resolve();
  if (googleIdentityPromise) return googleIdentityPromise;
  googleIdentityPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = GOOGLE_IDENTITY_SCRIPT;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("No se pudo cargar Google Identity Services."));
    document.head.appendChild(script);
  });
  return googleIdentityPromise;
}

async function googleApiFetch(url, options = {}, retry = true) {
  const token = await requestAccessToken();
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {})
    }
  });
  if ((response.status === 401 || response.status === 403) && retry) {
    accessToken = "";
    await requestAccessToken();
    return googleApiFetch(url, options, false);
  }
  if (!response.ok) {
    const text = await response.text();
    let message = text;
    try {
      message = JSON.parse(text).error?.message || text;
    } catch {
      // Keep the raw response text when Google does not return JSON.
    }
    throw new Error(message || `Google Sheets respondió ${response.status}`);
  }
  if (response.status === 204) return {};
  const body = await response.text();
  return body ? JSON.parse(body) : {};
}
