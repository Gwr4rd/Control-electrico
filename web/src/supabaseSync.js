const TABLE_NAME = "control_electrico_sync";

export function normalizeSupabaseConfig(config) {
  return {
    url: String(config?.url || "").trim().replace(/\/+$/, ""),
    anonKey: String(config?.anonKey || "").trim()
  };
}

export function isSupabaseConfigured(config) {
  const normalized = normalizeSupabaseConfig(config);
  return /^https:\/\/.+/i.test(normalized.url) && normalized.anonKey.length > 20;
}

export async function signUpWithPassword(config, email, password) {
  return authRequest(config, "/auth/v1/signup", {
    email: email.trim(),
    password
  });
}

export async function signInWithPassword(config, email, password) {
  return authRequest(config, "/auth/v1/token?grant_type=password", {
    email: email.trim(),
    password
  });
}

export async function refreshSupabaseSession(config, refreshToken) {
  return authRequest(config, "/auth/v1/token?grant_type=refresh_token", {
    refresh_token: refreshToken
  });
}

export async function signOutSupabase(config, accessToken) {
  await apiRequest(config, "/auth/v1/logout?scope=local", {
    method: "POST",
    accessToken
  });
}

export function normalizeSession(response) {
  if (!response?.access_token || !response?.refresh_token) return null;
  return {
    accessToken: response.access_token,
    refreshToken: response.refresh_token,
    expiresAt: Date.now() + Math.max(Number(response.expires_in) || 3600, 60) * 1000,
    user: response.user || null
  };
}

export async function fetchRemoteSnapshot(config, session) {
  const userId = session?.user?.id;
  if (!userId) throw new Error("La sesión no contiene un usuario válido.");
  const rows = await apiRequest(
    config,
    `/rest/v1/${TABLE_NAME}?user_id=eq.${encodeURIComponent(userId)}&select=payload,revision,updated_at`,
    { accessToken: session.accessToken }
  );
  const row = Array.isArray(rows) ? rows[0] : null;
  return row
    ? {
        payload: normalizePayload(row.payload),
        revision: Number(row.revision) || 0,
        updatedAt: row.updated_at || ""
      }
    : null;
}

export async function createRemoteSnapshot(config, session, payload) {
  const rows = await apiRequest(config, `/rest/v1/${TABLE_NAME}`, {
    method: "POST",
    accessToken: session.accessToken,
    headers: { Prefer: "return=representation" },
    body: {
      user_id: session.user.id,
      payload: normalizePayload(payload),
      revision: 1
    }
  });
  return normalizeRemoteRow(Array.isArray(rows) ? rows[0] : null);
}

export async function updateRemoteSnapshot(config, session, payload, expectedRevision) {
  const rows = await apiRequest(
    config,
    `/rest/v1/${TABLE_NAME}?user_id=eq.${encodeURIComponent(session.user.id)}&revision=eq.${expectedRevision}`,
    {
      method: "PATCH",
      accessToken: session.accessToken,
      headers: { Prefer: "return=representation" },
      body: {
        payload: normalizePayload(payload),
        revision: expectedRevision + 1
      }
    }
  );
  const row = Array.isArray(rows) ? rows[0] : null;
  return row ? normalizeRemoteRow(row) : null;
}

export function snapshotFingerprint(payload) {
  return stableStringify(normalizePayload(payload));
}

export function normalizePayload(payload) {
  return {
    users: Array.isArray(payload?.users) ? payload.users : [],
    receipts: Array.isArray(payload?.receipts) ? payload.receipts : [],
    readings: Array.isArray(payload?.readings) ? payload.readings : [],
    services: Array.isArray(payload?.services) ? payload.services : [],
    payments: Array.isArray(payload?.payments) ? payload.payments : [],
    settings: payload?.settings && typeof payload.settings === "object" ? payload.settings : {}
  };
}

export function mergeSnapshots(cloudPayload, localPayload) {
  const cloud = normalizePayload(cloudPayload);
  const local = normalizePayload(localPayload);
  return {
    users: mergeRecords(cloud.users, local.users, (item) => item.userId),
    receipts: mergeRecords(cloud.receipts, local.receipts, (item) => item.period),
    readings: mergeRecords(
      cloud.readings,
      local.readings,
      (item) => item.id || `${item.period}|${item.userId}|${item.internalReadingDate}`
    ),
    services: mergeRecords(
      cloud.services,
      local.services,
      (item) => item.id || `${item.period}|${item.name}`
    ),
    payments: mergeRecords(
      cloud.payments,
      local.payments,
      (item) => item.id || `${item.period}|${item.userId}`
    ),
    settings: { ...cloud.settings, ...local.settings }
  };
}

export function isPayloadEmpty(payload) {
  const normalized = normalizePayload(payload);
  return (
    normalized.users.length === 0 &&
    normalized.receipts.length === 0 &&
    normalized.readings.length === 0 &&
    normalized.services.length === 0 &&
    normalized.payments.length === 0
  );
}

function mergeRecords(cloudItems, localItems, keyFor) {
  const records = new Map();
  cloudItems.forEach((item) => records.set(keyFor(item), item));
  localItems.forEach((item) => records.set(keyFor(item), item));
  return [...records.values()];
}

function stableStringify(value) {
  if (value === null || typeof value !== "object") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(",")}]`;
  const keys = Object.keys(value).sort();
  return `{${keys.map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(",")}}`;
}

function normalizeRemoteRow(row) {
  if (!row) throw new Error("Supabase no devolvió el respaldo guardado.");
  return {
    payload: normalizePayload(row.payload),
    revision: Number(row.revision) || 0,
    updatedAt: row.updated_at || ""
  };
}

async function authRequest(config, path, body) {
  const response = await apiRequest(config, path, {
    method: "POST",
    body
  });
  return response;
}

async function apiRequest(config, path, options = {}) {
  const normalized = normalizeSupabaseConfig(config);
  if (!isSupabaseConfigured(normalized)) {
    throw new Error("Configura la URL y la clave pública de Supabase.");
  }
  const headers = {
    apikey: normalized.anonKey,
    "Content-Type": "application/json",
    ...options.headers
  };
  if (options.accessToken) headers.Authorization = `Bearer ${options.accessToken}`;

  let response;
  try {
    response = await fetch(`${normalized.url}${path}`, {
      method: options.method || "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
  } catch {
    throw new Error("No se pudo conectar con Supabase. Revisa Internet y la URL del proyecto.");
  }

  const text = await response.text();
  const parsed = text ? parseJson(text) : null;
  if (!response.ok) {
    const detail =
      parsed?.msg ||
      parsed?.message ||
      parsed?.error_description ||
      parsed?.error ||
      `Error HTTP ${response.status}`;
    throw new Error(String(detail));
  }
  return parsed;
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
