package com.gerar.controlelectrico.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime

sealed class GoogleSheetsAuthorization {
    data class Ready(val accessToken: String) : GoogleSheetsAuthorization()
    data class NeedsResolution(val pendingIntent: PendingIntent) : GoogleSheetsAuthorization()
}

data class GoogleSheetsBackupResult(
    val spreadsheetId: String,
    val updatedAt: String
)

class GoogleSheetsBackupManager(private val context: Context) {

    suspend fun authorize(activity: Activity): GoogleSheetsAuthorization {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GOOGLE_SCOPE)))
            .build()
        val result = Identity.getAuthorizationClient(activity).authorize(request).await()
        val pendingIntent = result.pendingIntent
        return if (result.hasResolution() && pendingIntent != null) {
            GoogleSheetsAuthorization.NeedsResolution(pendingIntent)
        } else {
            GoogleSheetsAuthorization.Ready(result.accessToken ?: error("Google no devolvio token de acceso."))
        }
    }

    fun authorizationFromIntent(intent: Intent?): GoogleSheetsAuthorization.Ready {
        val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
        return GoogleSheetsAuthorization.Ready(result.accessToken ?: error("Permiso de Google Sheets cancelado."))
    }

    suspend fun exportBackup(
        accessToken: String,
        backupJson: String,
        existingSpreadsheetId: String
    ): GoogleSheetsBackupResult {
        val spreadsheetId = existingSpreadsheetId.ifBlank { createSpreadsheet(accessToken) }
        ensureSheets(accessToken, spreadsheetId)
        clearSheets(accessToken, spreadsheetId)
        writeSheets(accessToken, spreadsheetId, backupJson)
        return GoogleSheetsBackupResult(
            spreadsheetId = spreadsheetId,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    suspend fun importBackup(accessToken: String, spreadsheetId: String): String {
        val response = googleApi(
            method = "GET",
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/${range(BACKUP_SHEET, "A:B").encoded()}",
            accessToken = accessToken
        )
        return backupJsonFromRows(response.optJSONArray("values") ?: JSONArray())
    }

    private suspend fun createSpreadsheet(accessToken: String): String {
        val body = JSONObject()
            .put("properties", JSONObject().put("title", SPREADSHEET_TITLE))
            .put("sheets", JSONArray().put(JSONObject().put("properties", JSONObject().put("title", BACKUP_SHEET))))
        val response = googleApi(
            method = "POST",
            url = "https://sheets.googleapis.com/v4/spreadsheets",
            accessToken = accessToken,
            body = body
        )
        return response.optString("spreadsheetId").ifBlank { error("Google no devolvio el ID de la hoja.") }
    }

    private suspend fun ensureSheets(accessToken: String, spreadsheetId: String) {
        val response = googleApi(
            method = "GET",
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets.properties.title",
            accessToken = accessToken
        )
        val existing = mutableSetOf<String>()
        val sheets = response.optJSONArray("sheets") ?: JSONArray()
        for (index in 0 until sheets.length()) {
            sheets.optJSONObject(index)
                ?.optJSONObject("properties")
                ?.optString("title")
                ?.takeIf { it.isNotBlank() }
                ?.let(existing::add)
        }
        val missing = mutableListOf<String>()
        if (!existing.contains(BACKUP_SHEET)) missing.add(BACKUP_SHEET)
        readableSections().map { it.title }.filterNot(existing::contains).forEach(missing::add)
        if (missing.isEmpty()) return
        googleApi(
            method = "POST",
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate",
            accessToken = accessToken,
            body = JSONObject().put(
                "requests",
                JSONArray(missing.map { title ->
                    JSONObject().put("addSheet", JSONObject().put("properties", JSONObject().put("title", title)))
                })
            )
        )
    }

    private suspend fun clearSheets(accessToken: String, spreadsheetId: String) {
        listOf(BACKUP_SHEET, *readableSections().map { it.title }.toTypedArray()).forEach { title ->
            googleApi(
                method = "POST",
                url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/${range(title, "A:Z").encoded()}:clear",
                accessToken = accessToken,
                body = JSONObject()
            )
        }
    }

    private suspend fun writeSheets(accessToken: String, spreadsheetId: String, backupJson: String) {
        val root = JSONObject(backupJson)
        val data = JSONArray()
            .put(JSONObject().put("range", range(BACKUP_SHEET, "A1:B")).put("values", backupRows(backupJson)))
        readableSections(root).forEach { section ->
            data.put(
                JSONObject()
                    .put("range", range(section.title, "A1:Z"))
                    .put("values", rowsFromArray(section.records, section.headers))
            )
        }
        googleApi(
            method = "POST",
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate",
            accessToken = accessToken,
            body = JSONObject()
                .put("valueInputOption", "RAW")
                .put("data", data)
        )
    }

    private suspend fun googleApi(
        method: String,
        url: String,
        accessToken: String,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        val responseCode = connection.responseCode
        val text = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (responseCode !in 200..299) {
            val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }
                .getOrNull()
                .orEmpty()
                .ifBlank { text.ifBlank { "Google Sheets respondio $responseCode" } }
            error(message)
        }
        if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private data class SheetSection(
        val title: String,
        val records: JSONArray = JSONArray(),
        val headers: List<String>
    )

    private fun readableSections(root: JSONObject = JSONObject()): List<SheetSection> = listOf(
        SheetSection(
            title = "Usuarios",
            records = root.optJSONArray("users") ?: JSONArray(),
            headers = listOf("userId", "name", "internalMeter", "isActive", "isResidual", "periodStates", "notes")
        ),
        SheetSection(
            title = "Recibos",
            records = root.optJSONArray("receipts") ?: JSONArray(),
            headers = listOf(
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
            )
        ),
        SheetSection(
            title = "Lecturas",
            records = root.optJSONArray("readings") ?: JSONArray(),
            headers = listOf(
                "id",
                "period",
                "userId",
                "isResidual",
                "internalReadingDate",
                "previousReading",
                "currentReading",
                "notes"
            )
        ),
        SheetSection(
            title = "Servicios",
            records = root.optJSONArray("services") ?: JSONArray(),
            headers = listOf(
                "id",
                "period",
                "name",
                "amount",
                "isActive",
                "splitCost",
                "participantCount",
                "participantUserIds",
                "notes"
            )
        ),
        SheetSection(
            title = "Pagos",
            records = root.optJSONArray("payments") ?: JSONArray(),
            headers = listOf("id", "period", "userId", "status", "amountPaid", "paymentDate", "notes")
        )
    )

    private fun rowsFromArray(records: JSONArray, preferredHeaders: List<String>): JSONArray {
        val headers = linkedSetOf<String>().apply { addAll(preferredHeaders) }
        for (index in 0 until records.length()) {
            val item = records.optJSONObject(index) ?: continue
            val keys = item.keys()
            while (keys.hasNext()) headers.add(keys.next())
        }
        val headerList = headers.toList()
        val rows = JSONArray().put(JSONArray(headerList))
        for (index in 0 until records.length()) {
            val item = records.optJSONObject(index) ?: JSONObject()
            rows.put(JSONArray(headerList.map { key -> item.opt(key).sheetValue() }))
        }
        return rows
    }

    private fun backupRows(backupJson: String): JSONArray {
        val chunks = backupJson.chunked(CHUNK_SIZE).ifEmpty { listOf("") }
        return JSONArray()
            .put(JSONArray(listOf("clave", "valor")))
            .put(JSONArray(listOf("format", "control_electrico_backup")))
            .put(JSONArray(listOf("version", "5")))
            .put(JSONArray(listOf("createdAt", LocalDateTime.now().toString())))
            .put(JSONArray(listOf("parts", chunks.size.toString())))
            .apply {
                chunks.forEachIndexed { index, chunk ->
                    put(JSONArray(listOf("part_${index + 1}", chunk)))
                }
            }
    }

    private fun backupJsonFromRows(rows: JSONArray): String {
        val values = mutableMapOf<String, String>()
        for (index in 0 until rows.length()) {
            val row = rows.optJSONArray(index) ?: continue
            values[row.optString(0)] = row.optString(1)
        }
        val parts = values["parts"]?.toIntOrNull() ?: 0
        val json = if (parts > 0) {
            (1..parts).joinToString("") { values["part_$it"].orEmpty() }
        } else {
            values.entries
                .filter { it.key.startsWith("part_") }
                .sortedBy { it.key.removePrefix("part_").toIntOrNull() ?: 0 }
                .joinToString("") { it.value }
        }
        return json.ifBlank { error("La hoja no contiene un respaldo compatible.") }
    }

    private fun Any?.sheetValue(): String = when (this) {
        null, JSONObject.NULL -> ""
        is JSONObject, is JSONArray -> toString()
        else -> toString()
    }

    private fun range(sheet: String, address: String): String = "'${sheet.replace("'", "''")}'!$address"

    private fun String.encoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    companion object {
        private const val GOOGLE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val SPREADSHEET_TITLE = "Control Electrico"
        private const val BACKUP_SHEET = "Respaldo_JSON"
        private const val CHUNK_SIZE = 45000

        fun extractSheetId(value: String): String {
            val text = value.trim()
            if (text.isBlank()) return ""
            Regex("/spreadsheets/d/([^/?#]+)").find(text)?.let { return it.groupValues[1] }
            return Regex("[a-zA-Z0-9_-]{20,}").find(text)?.value.orEmpty()
        }
    }
}
