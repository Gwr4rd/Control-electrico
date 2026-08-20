import { useCallback, useEffect, useState } from "react";
import { createInitialData } from "./domain";

const STORAGE_KEY = "control-electrico-web-v1";
const KEY_FIELDS = {
  users: "userId",
  receipts: "period",
  readings: "id",
  services: "id",
  payments: "id"
};

function loadLocalData() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    return normalizeData(parsed ? { ...createInitialData(), ...parsed } : createInitialData());
  } catch {
    return createInitialData();
  }
}

function normalizeData(data) {
  const initial = createInitialData();
  return {
    ...initial,
    ...data,
    auditEvents: Array.isArray(data?.auditEvents) ? data.auditEvents : [],
    settings: { ...initial.settings, ...(data?.settings || {}) }
  };
}

function labelFor(name, item) {
  if (!item) return "";
  if (name === "users") return item.name || item.userId;
  if (name === "receipts") return item.period;
  if (name === "readings") return `${item.period} · ${item.userId}`;
  if (name === "services") return `${item.period} · ${item.name}`;
  if (name === "payments") return `${item.period} · ${item.userId}`;
  return item.id || item.period || item.userId || "";
}

function auditEvent(action, collection, label) {
  return {
    id: crypto.randomUUID(),
    action,
    collection,
    label,
    createdAt: new Date().toISOString()
  };
}

function withAudit(data, event) {
  return {
    ...data,
    auditEvents: [event, ...(data.auditEvents || [])].slice(0, 120)
  };
}

export function useElectricData() {
  const [data, setData] = useState(loadLocalData);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }, [data]);

  const upsert = useCallback((name, value) => {
    const keyField = KEY_FIELDS[name];
    setData((current) => {
      const existing = current[name].find((item) => item[keyField] === value[keyField]);
      const records = current[name].filter((item) => item[keyField] !== value[keyField]);
      const next = { ...current, [name]: [...records, value] };
      return withAudit(next, auditEvent(existing ? "edit" : "create", name, labelFor(name, value)));
    });
  }, []);

  const remove = useCallback((name, key) => {
    const keyField = KEY_FIELDS[name];
    setData((current) => {
      const existing = current[name].find((item) => item[keyField] === key);
      const next = {
        ...current,
        [name]: current[name].filter((item) => item[keyField] !== key)
      };
      return withAudit(next, auditEvent("delete", name, labelFor(name, existing) || key));
    });
  }, []);

  const saveSettings = useCallback((settings) => {
    setData((current) => withAudit({ ...current, settings }, auditEvent("settings", "settings", "Configuración")));
  }, []);

  const replaceData = useCallback((nextData) => {
    const normalized = normalizeData(nextData);
    setData((current) =>
      withAudit(normalized, auditEvent("import", "backup", `${normalized.users.length} usuarios, ${normalized.receipts.length} recibos`))
    );
  }, []);

  const markBackup = useCallback((type) => {
    setData((current) =>
      withAudit(
        {
          ...current,
          settings: {
            ...current.settings,
            lastBackupAt: new Date().toISOString()
          }
        },
        auditEvent("backup", "backup", type)
      )
    );
  }, []);

  return {
    data,
    upsert,
    remove,
    saveSettings,
    replaceData,
    markBackup
  };
}
