package com.gerar.controlelectrico.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SupabaseConfig(
    val url: String = "",
    val anonKey: String = ""
) {
    fun normalized(): SupabaseConfig = copy(
        url = url.trim().trimEnd('/'),
        anonKey = anonKey.trim()
    )

    fun isValid(): Boolean {
        val value = normalized()
        return value.url.startsWith("https://") && value.anonKey.length > 20
    }
}

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val userId: String,
    val email: String
)

data class SupabaseRemoteSnapshot(
    val payload: JSONObject,
    val revision: Long,
    val updatedAt: String
)

enum class SupabaseSyncPhase {
    UNCONFIGURED,
    SIGNED_OUT,
    SYNCING,
    SYNCED,
    CONFLICT,
    ERROR
}

data class SupabaseConflict(
    val remote: SupabaseRemoteSnapshot,
    val local: JSONObject
)

data class SupabaseSyncState(
    val phase: SupabaseSyncPhase,
    val progress: Int = 0,
    val message: String,
    val error: String = "",
    val conflict: SupabaseConflict? = null
)

class SupabaseSyncManager(
    context: Context,
    private val repository: ElectricRepository
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val syncMutex = Mutex()
    private var autoSyncJob: Job? = null
    private var periodicSyncJob: Job? = null
    private var applyingRemote = false

    val config = mutableStateOf(loadConfig())
    val session = mutableStateOf(loadSession())
    val revision = mutableStateOf(prefs.getLong(KEY_REVISION, 0L))
    val lastSyncedAt = mutableStateOf(prefs.getString(KEY_LAST_SYNCED_AT, "").orEmpty())
    val syncedFingerprint = mutableStateOf(prefs.getString(KEY_FINGERPRINT, "").orEmpty())
    val state = mutableStateOf(initialState())

    init {
        repository.onDataChanged = {
            if (!applyingRemote) scheduleAutoSync()
        }
        if (config.value.isValid() && session.value != null) {
            scope.launch {
                delay(350)
                syncNow()
            }
        }
    }

    fun isConfigured(): Boolean = config.value.isValid()

    fun isSignedIn(): Boolean = session.value != null

    fun hasPendingChanges(): Boolean {
        val current = fingerprint(repository.exportSyncPayload())
        return isSignedIn() && current != syncedFingerprint.value
    }

    fun saveConfig(next: SupabaseConfig) {
        val normalized = next.normalized()
        val changed = normalized != config.value
        config.value = normalized
        prefs.edit()
            .putString(KEY_URL, normalized.url)
            .putString(KEY_ANON_KEY, normalized.anonKey)
            .apply()
        if (changed) {
            clearSessionAndMetadata()
        }
        state.value = if (normalized.isValid()) {
            SupabaseSyncState(SupabaseSyncPhase.SIGNED_OUT, message = "Configuración guardada")
        } else {
            SupabaseSyncState(SupabaseSyncPhase.UNCONFIGURED, message = "Supabase no configurado")
        }
    }

    suspend fun signIn(email: String, password: String) {
        requireConfigured()
        setState(SupabaseSyncPhase.SYNCING, 10, "Iniciando sesión")
        try {
            val response = requestJson(
                path = "/auth/v1/token?grant_type=password",
                method = "POST",
                body = JSONObject().put("email", email.trim()).put("password", password)
            )
            val nextSession = response.toSession()
            saveSession(nextSession)
            setState(SupabaseSyncPhase.SYNCING, 20, "Cuenta conectada")
            syncNow()
        } catch (error: Exception) {
            setState(
                SupabaseSyncPhase.ERROR,
                0,
                "No se pudo iniciar sesión",
                userMessage(error)
            )
            throw error
        }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        requireConfigured()
        setState(SupabaseSyncPhase.SYNCING, 10, "Creando cuenta")
        try {
            val response = requestJson(
                path = "/auth/v1/signup",
                method = "POST",
                body = JSONObject().put("email", email.trim()).put("password", password)
            )
            val hasSession = response.optString("access_token").isNotBlank()
            if (hasSession) {
                saveSession(response.toSession())
                syncNow()
                return false
            }
            setState(
                SupabaseSyncPhase.SIGNED_OUT,
                0,
                "Revisa tu correo para confirmar la cuenta"
            )
            return true
        } catch (error: Exception) {
            setState(
                SupabaseSyncPhase.ERROR,
                0,
                "No se pudo crear la cuenta",
                userMessage(error)
            )
            throw error
        }
    }

    suspend fun signOut() {
        val current = session.value
        if (current != null && config.value.isValid()) {
            runCatching {
                requestJson(
                    path = "/auth/v1/logout?scope=local",
                    method = "POST",
                    accessToken = current.accessToken
                )
            }
        }
        clearSessionAndMetadata()
        setState(
            SupabaseSyncPhase.SIGNED_OUT,
            0,
            "Sesión cerrada; los datos locales se conservan"
        )
    }

    suspend fun syncNow() {
        if (!syncMutex.tryLock()) return
        try {
            if (!config.value.isValid()) {
                setState(SupabaseSyncPhase.UNCONFIGURED, 0, "Supabase no configurado")
                return
            }
            if (session.value == null) {
                setState(SupabaseSyncPhase.SIGNED_OUT, 0, "Inicia sesión para sincronizar")
                return
            }

            setState(SupabaseSyncPhase.SYNCING, 15, "Comprobando la cuenta")
            val activeSession = ensureFreshSession()
            setState(SupabaseSyncPhase.SYNCING, 35, "Descargando estado de la nube")
            val remote = fetchRemote(activeSession)
            val localPayload = repository.exportSyncPayload()
            val localFingerprint = fingerprint(localPayload)

            setState(SupabaseSyncPhase.SYNCING, 55, "Comparando cambios")
            if (remote == null) {
                setState(SupabaseSyncPhase.SYNCING, 75, "Creando respaldo en la nube")
                finishUpload(createRemote(activeSession, localPayload), localPayload)
                return
            }

            if (revision.value == 0L) {
                if (isPayloadEmpty(localPayload)) {
                    acceptRemote(remote)
                } else {
                    setConflict(remote, localPayload, "Hay datos distintos en este dispositivo y en la nube")
                }
                return
            }

            val localDirty = localFingerprint != syncedFingerprint.value
            if (remote.revision > revision.value) {
                if (localDirty) {
                    setConflict(remote, localPayload, "Cambios pendientes en dos dispositivos")
                } else {
                    acceptRemote(remote)
                }
                return
            }

            if (!localDirty && remote.revision == revision.value) {
                setState(SupabaseSyncPhase.SYNCED, 100, "Todo está actualizado")
                return
            }

            if (remote.revision != revision.value) {
                setConflict(remote, localPayload, "La revisión local y la nube no coinciden")
                return
            }

            setState(SupabaseSyncPhase.SYNCING, 78, "Subiendo cambios locales")
            val updated = updateRemote(activeSession, localPayload, remote.revision)
            if (updated == null) {
                val latest = fetchRemote(activeSession)
                    ?: error("No se encontró el respaldo después del conflicto.")
                setConflict(
                    latest,
                    localPayload,
                    "Otro dispositivo actualizó los datos durante la sincronización"
                )
                return
            }
            finishUpload(updated, localPayload)
        } catch (error: Exception) {
            setState(
                SupabaseSyncPhase.ERROR,
                0,
                "No se pudo sincronizar",
                userMessage(error)
            )
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun resolveConflict(choice: String) {
        val conflict = state.value.conflict ?: return
        if (choice == CONFLICT_CLOUD) {
            acceptRemote(conflict.remote)
            return
        }
        if (!syncMutex.tryLock()) return
        try {
            setState(
                SupabaseSyncPhase.SYNCING,
                70,
                if (choice == CONFLICT_MERGE) "Fusionando respaldos" else "Conservando este dispositivo"
            )
            val activeSession = ensureFreshSession()
            val payload = if (choice == CONFLICT_MERGE) {
                mergePayloads(conflict.remote.payload, conflict.local)
            } else {
                conflict.local
            }
            val updated = updateRemote(
                activeSession,
                payload,
                conflict.remote.revision
            ) ?: error("La nube volvió a cambiar. Sincroniza e inténtalo nuevamente.")
            applyingRemote = true
            try {
                repository.replaceFromSyncPayload(payload)
            } finally {
                applyingRemote = false
            }
            finishUpload(updated, payload)
        } catch (error: Exception) {
            state.value = SupabaseSyncState(
                phase = SupabaseSyncPhase.ERROR,
                progress = 0,
                message = "No se pudo resolver el conflicto",
                error = userMessage(error),
                conflict = conflict
            )
        } finally {
            syncMutex.unlock()
        }
    }

    private fun scheduleAutoSync() {
        if (!isConfigured() || !isSignedIn()) return
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            delay(2_500)
            if (state.value.phase != SupabaseSyncPhase.CONFLICT) syncNow()
        }
    }

    private fun schedulePeriodicSync() {
        periodicSyncJob?.cancel()
        if (!isConfigured() || !isSignedIn()) return
        periodicSyncJob = scope.launch {
            delay(60_000)
            if (state.value.phase == SupabaseSyncPhase.SYNCED) syncNow()
        }
    }

    private suspend fun ensureFreshSession(): SupabaseSession {
        val current = session.value ?: error("Inicia sesión para sincronizar.")
        if (current.expiresAtMillis > System.currentTimeMillis() + 90_000) return current
        setState(SupabaseSyncPhase.SYNCING, 10, "Renovando sesión")
        val response = requestJson(
            path = "/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            body = JSONObject().put("refresh_token", current.refreshToken)
        )
        val refreshed = response.toSession()
        saveSession(refreshed)
        return refreshed
    }

    private suspend fun fetchRemote(activeSession: SupabaseSession): SupabaseRemoteSnapshot? {
        val rows = requestArray(
            path = "/rest/v1/$TABLE_NAME?user_id=eq.${activeSession.userId}&select=payload,revision,updated_at",
            accessToken = activeSession.accessToken
        )
        return rows.optJSONObject(0)?.toRemoteSnapshot()
    }

    private suspend fun createRemote(
        activeSession: SupabaseSession,
        payload: JSONObject
    ): SupabaseRemoteSnapshot {
        val rows = requestArray(
            path = "/rest/v1/$TABLE_NAME",
            method = "POST",
            accessToken = activeSession.accessToken,
            body = JSONObject()
                .put("user_id", activeSession.userId)
                .put("payload", payload)
                .put("revision", 1),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        return rows.optJSONObject(0)?.toRemoteSnapshot()
            ?: error("Supabase no devolvió el respaldo guardado.")
    }

    private suspend fun updateRemote(
        activeSession: SupabaseSession,
        payload: JSONObject,
        expectedRevision: Long
    ): SupabaseRemoteSnapshot? {
        val rows = requestArray(
            path = "/rest/v1/$TABLE_NAME?user_id=eq.${activeSession.userId}&revision=eq.$expectedRevision",
            method = "PATCH",
            accessToken = activeSession.accessToken,
            body = JSONObject()
                .put("payload", payload)
                .put("revision", expectedRevision + 1),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        return rows.optJSONObject(0)?.toRemoteSnapshot()
    }

    private fun acceptRemote(remote: SupabaseRemoteSnapshot) {
        applyingRemote = true
        try {
            repository.replaceFromSyncPayload(remote.payload)
        } finally {
            applyingRemote = false
        }
        saveMetadata(remote.revision, fingerprint(remote.payload))
        setState(SupabaseSyncPhase.SYNCED, 100, "Datos descargados y actualizados")
    }

    private fun finishUpload(remote: SupabaseRemoteSnapshot, payload: JSONObject) {
        saveMetadata(remote.revision, fingerprint(payload))
        setState(SupabaseSyncPhase.SYNCED, 100, "Sincronización completada")
    }

    private fun setConflict(
        remote: SupabaseRemoteSnapshot,
        local: JSONObject,
        message: String
    ) {
        state.value = SupabaseSyncState(
            phase = SupabaseSyncPhase.CONFLICT,
            progress = 100,
            message = message,
            conflict = SupabaseConflict(remote, local)
        )
    }

    private fun saveMetadata(nextRevision: Long, nextFingerprint: String) {
        val now = Instant.now().toString()
        revision.value = nextRevision
        syncedFingerprint.value = nextFingerprint
        lastSyncedAt.value = now
        prefs.edit()
            .putLong(KEY_REVISION, nextRevision)
            .putString(KEY_FINGERPRINT, nextFingerprint)
            .putString(KEY_LAST_SYNCED_AT, now)
            .apply()
    }

    private fun saveSession(next: SupabaseSession) {
        session.value = next
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, next.accessToken)
            .putString(KEY_REFRESH_TOKEN, next.refreshToken)
            .putLong(KEY_EXPIRES_AT, next.expiresAtMillis)
            .putString(KEY_USER_ID, next.userId)
            .putString(KEY_EMAIL, next.email)
            .apply()
    }

    private fun clearSessionAndMetadata() {
        autoSyncJob?.cancel()
        periodicSyncJob?.cancel()
        session.value = null
        revision.value = 0L
        syncedFingerprint.value = ""
        lastSyncedAt.value = ""
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_REVISION)
            .remove(KEY_FINGERPRINT)
            .remove(KEY_LAST_SYNCED_AT)
            .apply()
    }

    private fun loadConfig(): SupabaseConfig = SupabaseConfig(
        url = prefs.getString(KEY_URL, "").orEmpty(),
        anonKey = prefs.getString(KEY_ANON_KEY, "").orEmpty()
    ).normalized()

    private fun loadSession(): SupabaseSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        val userId = prefs.getString(KEY_USER_ID, "").orEmpty()
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) return null
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L),
            userId = userId,
            email = prefs.getString(KEY_EMAIL, "").orEmpty()
        )
    }

    private fun initialState(): SupabaseSyncState {
        return when {
            !config.value.isValid() -> SupabaseSyncState(
                SupabaseSyncPhase.UNCONFIGURED,
                message = "Supabase no configurado"
            )
            session.value == null -> SupabaseSyncState(
                SupabaseSyncPhase.SIGNED_OUT,
                message = "Inicia sesión para sincronizar"
            )
            else -> SupabaseSyncState(
                SupabaseSyncPhase.SYNCING,
                progress = 0,
                message = "Preparando sincronización"
            )
        }
    }

    private fun requireConfigured() {
        if (!config.value.isValid()) error("Guarda primero la configuración de Supabase.")
    }

    private fun setState(
        phase: SupabaseSyncPhase,
        progress: Int,
        message: String,
        error: String = ""
    ) {
        state.value = SupabaseSyncState(
            phase = phase,
            progress = progress.coerceIn(0, 100),
            message = message,
            error = error
        )
        if (phase == SupabaseSyncPhase.SYNCED) schedulePeriodicSync()
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        accessToken: String? = null,
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {
        val text = executeRequest(path, method, accessToken, body?.toString(), extraHeaders)
        if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private suspend fun requestArray(
        path: String,
        method: String = "GET",
        accessToken: String? = null,
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONArray = withContext(Dispatchers.IO) {
        val text = executeRequest(path, method, accessToken, body?.toString(), extraHeaders)
        if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    private fun executeRequest(
        path: String,
        method: String,
        accessToken: String?,
        body: String?,
        extraHeaders: Map<String, String>
    ): String {
        val activeConfig = config.value.normalized()
        if (!activeConfig.isValid()) error("Configura la URL y la clave pública de Supabase.")
        val connection = (URL("${activeConfig.url}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", activeConfig.anonKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            extraHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching {
                    val error = JSONObject(text)
                    error.optString("msg")
                        .ifBlank { error.optString("message") }
                        .ifBlank { error.optString("error_description") }
                        .ifBlank { error.optString("error") }
                }.getOrNull().orEmpty().ifBlank { "Error HTTP $status" }
                error(detail)
            }
            text
        } catch (error: Exception) {
            if (error.message?.startsWith("Error HTTP") == true) throw error
            throw IllegalStateException(
                error.message ?: "No se pudo conectar con Supabase. Revisa Internet y la URL."
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toSession(): SupabaseSession {
        val accessToken = optString("access_token")
        val refreshToken = optString("refresh_token")
        val user = optJSONObject("user") ?: JSONObject()
        val userId = user.optString("id")
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) {
            error("Supabase no devolvió una sesión válida.")
        }
        val expiresIn = optLong("expires_in", 3_600L).coerceAtLeast(60L)
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1_000L,
            userId = userId,
            email = user.optString("email")
        )
    }

    private fun JSONObject.toRemoteSnapshot(): SupabaseRemoteSnapshot {
        return SupabaseRemoteSnapshot(
            payload = normalizePayload(optJSONObject("payload") ?: JSONObject()),
            revision = optLong("revision", 0L),
            updatedAt = optString("updated_at")
        )
    }

    private fun fingerprint(payload: JSONObject): String = canonicalJson(normalizePayload(payload))

    private fun canonicalJson(value: Any?): String {
        return when (value) {
            null, JSONObject.NULL -> "null"
            is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { key -> "${JSONObject.quote(key)}:${canonicalJson(value.opt(key))}" }
            is JSONArray -> (0 until value.length()).joinToString(
                prefix = "[",
                postfix = "]",
                separator = ","
            ) { index -> canonicalJson(value.opt(index)) }
            is String -> JSONObject.quote(value)
            is Number -> runCatching {
                BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
            }.getOrElse { value.toString() }
            is Boolean -> value.toString()
            else -> JSONObject.quote(value.toString())
        }
    }

    private fun normalizePayload(payload: JSONObject): JSONObject {
        return JSONObject()
            .put("users", payload.optJSONArray("users") ?: JSONArray())
            .put("receipts", payload.optJSONArray("receipts") ?: JSONArray())
            .put("readings", payload.optJSONArray("readings") ?: JSONArray())
            .put("services", payload.optJSONArray("services") ?: JSONArray())
            .put("payments", payload.optJSONArray("payments") ?: JSONArray())
            .put("settings", payload.optJSONObject("settings") ?: JSONObject())
    }

    private fun isPayloadEmpty(payload: JSONObject): Boolean {
        val normalized = normalizePayload(payload)
        return listOf("users", "receipts", "readings", "services", "payments")
            .all { normalized.optJSONArray(it)?.length() == 0 }
    }

    private fun mergePayloads(cloud: JSONObject, local: JSONObject): JSONObject {
        val remote = normalizePayload(cloud)
        val device = normalizePayload(local)
        return JSONObject()
            .put("users", mergeRecords(remote, device, "users") { it.optString("userId") })
            .put("receipts", mergeRecords(remote, device, "receipts") { it.optString("period") })
            .put(
                "readings",
                mergeRecords(remote, device, "readings") {
                    it.optString("id").ifBlank {
                        "${it.optString("period")}|${it.optString("userId")}|${it.optString("internalReadingDate")}"
                    }
                }
            )
            .put(
                "services",
                mergeRecords(remote, device, "services") {
                    it.optString("id").ifBlank { "${it.optString("period")}|${it.optString("name")}" }
                }
            )
            .put(
                "payments",
                mergeRecords(remote, device, "payments") {
                    it.optString("id").ifBlank { "${it.optString("period")}|${it.optString("userId")}" }
                }
            )
            .put(
                "settings",
                mergeObjects(
                    remote.optJSONObject("settings") ?: JSONObject(),
                    device.optJSONObject("settings") ?: JSONObject()
                )
            )
    }

    private fun mergeRecords(
        cloud: JSONObject,
        local: JSONObject,
        name: String,
        keyFor: (JSONObject) -> String
    ): JSONArray {
        val records = linkedMapOf<String, JSONObject>()
        val cloudItems = cloud.optJSONArray(name) ?: JSONArray()
        val localItems = local.optJSONArray(name) ?: JSONArray()
        for (index in 0 until cloudItems.length()) {
            cloudItems.optJSONObject(index)?.let { records[keyFor(it)] = it }
        }
        for (index in 0 until localItems.length()) {
            localItems.optJSONObject(index)?.let { records[keyFor(it)] = it }
        }
        return JSONArray(records.values.toList())
    }

    private fun mergeObjects(cloud: JSONObject, local: JSONObject): JSONObject {
        val merged = JSONObject(cloud.toString())
        local.keys().forEach { key -> merged.put(key, local.opt(key)) }
        return merged
    }

    private fun userMessage(error: Exception): String {
        val message = error.message.orEmpty()
        return if (
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true)
        ) {
            "No se pudo conectar con Supabase. Revisa Internet y la URL del proyecto."
        } else {
            message.ifBlank { "Error desconocido de sincronización." }
        }
    }

    companion object {
        const val CONFLICT_CLOUD = "cloud"
        const val CONFLICT_MERGE = "merge"
        const val CONFLICT_DEVICE = "device"

        private const val TABLE_NAME = "control_electrico_sync"
        private const val PREFS_NAME = "control_electrico_supabase"
        private const val KEY_URL = "url"
        private const val KEY_ANON_KEY = "anon_key"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_REVISION = "revision"
        private const val KEY_FINGERPRINT = "fingerprint"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at"
    }
}
