import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  createRemoteSnapshot,
  fetchRemoteSnapshot,
  isPayloadEmpty,
  isSupabaseConfigured,
  mergeSnapshots,
  normalizeSession,
  normalizeSupabaseConfig,
  refreshSupabaseSession,
  signInWithPassword,
  signOutSupabase,
  signUpWithPassword,
  snapshotFingerprint,
  updateRemoteSnapshot
} from "./supabaseSync";

const CONFIG_KEY = "control-electrico-supabase-config-v1";
const SESSION_KEY = "control-electrico-supabase-session-v1";
const META_KEY = "control-electrico-supabase-meta-v1";

function loadJson(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key) || "null") || fallback;
  } catch {
    return fallback;
  }
}

function initialConfig() {
  return normalizeSupabaseConfig(
    loadJson(CONFIG_KEY, {
      url: import.meta.env.VITE_SUPABASE_URL || "",
      anonKey: import.meta.env.VITE_SUPABASE_ANON_KEY || ""
    })
  );
}

function emptyMeta() {
  return {
    revision: 0,
    syncedFingerprint: "",
    lastSyncedAt: ""
  };
}

function initialMeta() {
  return loadJson(META_KEY, emptyMeta());
}

export function useSupabaseSync(data, replaceData) {
  const [config, setConfig] = useState(initialConfig);
  const [session, setSession] = useState(() => loadJson(SESSION_KEY, null));
  const [meta, setMeta] = useState(initialMeta);
  const [state, setState] = useState(() => ({
    phase: isSupabaseConfigured(initialConfig()) ? "signed_out" : "unconfigured",
    progress: 0,
    message: isSupabaseConfigured(initialConfig())
      ? "Inicia sesión para sincronizar"
      : "Supabase no configurado",
    error: "",
    conflict: null
  }));
  const dataRef = useRef(data);
  const sessionRef = useRef(session);
  const configRef = useRef(config);
  const metaRef = useRef(meta);
  const syncingRef = useRef(false);
  const autoTimerRef = useRef(null);

  dataRef.current = data;
  sessionRef.current = session;
  configRef.current = config;
  metaRef.current = meta;

  const fingerprint = useMemo(() => snapshotFingerprint(data), [data]);
  const configured = isSupabaseConfigured(config);
  const signedIn = Boolean(session?.accessToken && session?.refreshToken && session?.user?.id);
  const dirty = signedIn && fingerprint !== meta.syncedFingerprint;

  const persistSession = useCallback((nextSession) => {
    sessionRef.current = nextSession;
    setSession(nextSession);
    if (nextSession) localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
    else localStorage.removeItem(SESSION_KEY);
  }, []);

  const persistMeta = useCallback((nextMeta) => {
    metaRef.current = nextMeta;
    setMeta(nextMeta);
    localStorage.setItem(META_KEY, JSON.stringify(nextMeta));
  }, []);

  const ensureFreshSession = useCallback(async () => {
    const current = sessionRef.current;
    if (!current?.refreshToken) throw new Error("Inicia sesión para sincronizar.");
    if (Number(current.expiresAt) > Date.now() + 90_000) return current;
    setState((value) => ({
      ...value,
      phase: "syncing",
      progress: 10,
      message: "Renovando sesión",
      error: ""
    }));
    const refreshed = normalizeSession(
      await refreshSupabaseSession(configRef.current, current.refreshToken)
    );
    if (!refreshed) throw new Error("No se pudo renovar la sesión.");
    persistSession(refreshed);
    return refreshed;
  }, [persistSession]);

  const acceptRemote = useCallback(
    (remote) => {
      replaceData(remote.payload);
      const nextMeta = {
        revision: remote.revision,
        syncedFingerprint: snapshotFingerprint(remote.payload),
        lastSyncedAt: new Date().toISOString()
      };
      persistMeta(nextMeta);
      setState({
        phase: "synced",
        progress: 100,
        message: "Datos descargados y actualizados",
        error: "",
        conflict: null
      });
    },
    [persistMeta, replaceData]
  );

  const finishUpload = useCallback(
    (remote, payload) => {
      const nextMeta = {
        revision: remote.revision,
        syncedFingerprint: snapshotFingerprint(payload),
        lastSyncedAt: new Date().toISOString()
      };
      persistMeta(nextMeta);
      setState({
        phase: "synced",
        progress: 100,
        message: "Sincronización completada",
        error: "",
        conflict: null
      });
    },
    [persistMeta]
  );

  const syncNow = useCallback(async () => {
    if (syncingRef.current) return;
    if (!isSupabaseConfigured(configRef.current)) {
      setState({
        phase: "unconfigured",
        progress: 0,
        message: "Supabase no configurado",
        error: "",
        conflict: null
      });
      return;
    }
    if (!sessionRef.current) {
      setState({
        phase: "signed_out",
        progress: 0,
        message: "Inicia sesión para sincronizar",
        error: "",
        conflict: null
      });
      return;
    }

    syncingRef.current = true;
    setState({
      phase: "syncing",
      progress: 15,
      message: "Comprobando la cuenta",
      error: "",
      conflict: null
    });
    try {
      const activeSession = await ensureFreshSession();
      setState((value) => ({ ...value, progress: 35, message: "Descargando estado de la nube" }));
      const remote = await fetchRemoteSnapshot(configRef.current, activeSession);
      const localPayload = dataRef.current;
      const localFingerprint = snapshotFingerprint(localPayload);
      const currentMeta = metaRef.current;

      setState((value) => ({ ...value, progress: 55, message: "Comparando cambios" }));
      if (!remote) {
        setState((value) => ({ ...value, progress: 75, message: "Creando respaldo en la nube" }));
        const created = await createRemoteSnapshot(configRef.current, activeSession, localPayload);
        finishUpload(created, localPayload);
        return;
      }

      if (currentMeta.revision === 0) {
        if (isPayloadEmpty(localPayload)) {
          acceptRemote(remote);
        } else {
          setState({
            phase: "conflict",
            progress: 100,
            message: "Hay datos distintos en este equipo y en la nube",
            error: "",
            conflict: { remote, local: localPayload }
          });
        }
        return;
      }

      const localDirty = localFingerprint !== currentMeta.syncedFingerprint;
      if (remote.revision > currentMeta.revision) {
        if (!localDirty) {
          acceptRemote(remote);
        } else {
          setState({
            phase: "conflict",
            progress: 100,
            message: "Cambios pendientes en dos dispositivos",
            error: "",
            conflict: { remote, local: localPayload }
          });
        }
        return;
      }

      if (!localDirty && remote.revision === currentMeta.revision) {
        setState({
          phase: "synced",
          progress: 100,
          message: "Todo está actualizado",
          error: "",
          conflict: null
        });
        return;
      }

      if (remote.revision !== currentMeta.revision) {
        setState({
          phase: "conflict",
          progress: 100,
          message: "La revisión local y la nube no coinciden",
          error: "",
          conflict: { remote, local: localPayload }
        });
        return;
      }

      setState((value) => ({ ...value, progress: 78, message: "Subiendo cambios locales" }));
      const updated = await updateRemoteSnapshot(
        configRef.current,
        activeSession,
        localPayload,
        remote.revision
      );
      if (!updated) {
        const latest = await fetchRemoteSnapshot(configRef.current, activeSession);
        setState({
          phase: "conflict",
          progress: 100,
          message: "Otro dispositivo actualizó los datos durante la sincronización",
          error: "",
          conflict: { remote: latest, local: localPayload }
        });
        return;
      }
      finishUpload(updated, localPayload);
    } catch (error) {
      setState({
        phase: "error",
        progress: 0,
        message: "No se pudo sincronizar",
        error: error.message || String(error),
        conflict: null
      });
    } finally {
      syncingRef.current = false;
    }
  }, [acceptRemote, ensureFreshSession, finishUpload]);

  const configure = useCallback(
    async (nextConfig) => {
      const normalized = normalizeSupabaseConfig(nextConfig);
      const previous = configRef.current;
      localStorage.setItem(CONFIG_KEY, JSON.stringify(normalized));
      configRef.current = normalized;
      setConfig(normalized);
      if (
        normalized.url !== previous.url ||
        normalized.anonKey !== previous.anonKey
      ) {
        persistSession(null);
        persistMeta(emptyMeta());
      }
      setState({
        phase: isSupabaseConfigured(normalized) ? "signed_out" : "unconfigured",
        progress: 0,
        message: isSupabaseConfigured(normalized)
          ? "Configuración guardada"
          : "Supabase no configurado",
        error: "",
        conflict: null
      });
    },
    [persistMeta, persistSession]
  );

  const signIn = useCallback(
    async (email, password) => {
      if (!isSupabaseConfigured(configRef.current)) {
        throw new Error("Guarda primero la configuración de Supabase.");
      }
      setState({
        phase: "syncing",
        progress: 10,
        message: "Iniciando sesión",
        error: "",
        conflict: null
      });
      try {
        const nextSession = normalizeSession(
          await signInWithPassword(configRef.current, email, password)
        );
        if (!nextSession) throw new Error("Supabase no devolvió una sesión válida.");
        persistSession(nextSession);
        setState((value) => ({ ...value, progress: 20, message: "Cuenta conectada" }));
        window.setTimeout(syncNow, 0);
        return nextSession;
      } catch (error) {
        setState({
          phase: "error",
          progress: 0,
          message: "No se pudo iniciar sesión",
          error: error.message || String(error),
          conflict: null
        });
        throw error;
      }
    },
    [persistSession, syncNow]
  );

  const signUp = useCallback(
    async (email, password) => {
      if (!isSupabaseConfigured(configRef.current)) {
        throw new Error("Guarda primero la configuración de Supabase.");
      }
      try {
        const response = await signUpWithPassword(configRef.current, email, password);
        const nextSession = normalizeSession(response);
        if (nextSession) {
          persistSession(nextSession);
          window.setTimeout(syncNow, 0);
          return { confirmationRequired: false };
        }
        setState({
          phase: "signed_out",
          progress: 0,
          message: "Revisa tu correo para confirmar la cuenta",
          error: "",
          conflict: null
        });
        return { confirmationRequired: true };
      } catch (error) {
        setState({
          phase: "error",
          progress: 0,
          message: "No se pudo crear la cuenta",
          error: error.message || String(error),
          conflict: null
        });
        throw error;
      }
    },
    [persistSession, syncNow]
  );

  const signOut = useCallback(async () => {
    const current = sessionRef.current;
    if (current?.accessToken && isSupabaseConfigured(configRef.current)) {
      try {
        await signOutSupabase(configRef.current, current.accessToken);
      } catch {
        // La sesión local se elimina aunque el servidor no esté disponible.
      }
    }
    persistSession(null);
    persistMeta(emptyMeta());
    setState({
      phase: "signed_out",
      progress: 0,
      message: "Sesión cerrada; los datos locales se conservan",
      error: "",
      conflict: null
    });
  }, [persistMeta, persistSession]);

  const resolveConflict = useCallback(
    async (choice) => {
      const conflict = state.conflict;
      if (!conflict?.remote) return;
      if (choice === "cloud") {
        acceptRemote(conflict.remote);
        return;
      }
      syncingRef.current = true;
      setState((value) => ({
        ...value,
        phase: "syncing",
        progress: 70,
        message: choice === "merge" ? "Fusionando respaldos" : "Conservando este dispositivo",
        error: ""
      }));
      try {
        const activeSession = await ensureFreshSession();
        const payload =
          choice === "merge"
            ? mergeSnapshots(conflict.remote.payload, conflict.local)
            : conflict.local;
        const updated = await updateRemoteSnapshot(
          configRef.current,
          activeSession,
          payload,
          conflict.remote.revision
        );
        if (!updated) throw new Error("La nube volvió a cambiar. Sincroniza e inténtalo nuevamente.");
        replaceData(payload);
        finishUpload(updated, payload);
      } catch (error) {
        setState({
          phase: "error",
          progress: 0,
          message: "No se pudo resolver el conflicto",
          error: error.message || String(error),
          conflict
        });
      } finally {
        syncingRef.current = false;
      }
    },
    [acceptRemote, ensureFreshSession, finishUpload, replaceData, state.conflict]
  );

  useEffect(() => {
    if (
      !configured ||
      !signedIn ||
      state.phase === "conflict" ||
      state.phase === "error"
    ) return undefined;
    if (fingerprint === meta.syncedFingerprint && meta.revision > 0) return undefined;
    window.clearTimeout(autoTimerRef.current);
    autoTimerRef.current = window.setTimeout(syncNow, 2500);
    return () => window.clearTimeout(autoTimerRef.current);
  }, [configured, fingerprint, meta.revision, meta.syncedFingerprint, signedIn, state.phase, syncNow]);

  useEffect(() => {
    if (configured && signedIn) window.setTimeout(syncNow, 250);
  }, []); // Se ejecuta una vez para recuperar cambios al abrir la aplicación.

  useEffect(() => {
    if (!configured || !signedIn || state.phase !== "synced") return undefined;
    const interval = window.setInterval(syncNow, 60_000);
    return () => window.clearInterval(interval);
  }, [configured, signedIn, state.phase, syncNow]);

  return {
    config,
    session,
    state,
    meta,
    configured,
    signedIn,
    dirty,
    configure,
    signIn,
    signUp,
    signOut,
    syncNow,
    resolveConflict
  };
}
