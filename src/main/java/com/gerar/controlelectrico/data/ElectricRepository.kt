package com.gerar.controlelectrico.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import com.gerar.controlelectrico.domain.AppSettings
import com.gerar.controlelectrico.domain.ElectricUser
import com.gerar.controlelectrico.domain.MeterReading
import com.gerar.controlelectrico.domain.MonthlyReceipt
import com.gerar.controlelectrico.domain.PaymentStatus
import com.gerar.controlelectrico.domain.ServiceExpense
import com.gerar.controlelectrico.domain.UserPayment
import com.gerar.controlelectrico.domain.UserPeriodState
import com.gerar.controlelectrico.domain.isActiveInPeriod
import com.gerar.controlelectrico.domain.isResidualInPeriod
import com.gerar.controlelectrico.domain.withStateForPeriod
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth
import java.util.UUID

class ElectricRepository(context: Context) {
    private val prefs = context.getSharedPreferences("electric_control_data", Context.MODE_PRIVATE)
    private val dao = ControlElectricoDatabase.get(context).electricDao()

    val users = mutableStateListOf<ElectricUser>()
    val receipts = mutableStateListOf<MonthlyReceipt>()
    val readings = mutableStateListOf<MeterReading>()
    val serviceExpenses = mutableStateListOf<ServiceExpense>()
    val payments = mutableStateListOf<UserPayment>()
    val isAmoled = mutableStateOf(prefs.getBoolean(KEY_AMOLED, false))
    val settings = mutableStateOf(loadSettings())
    var onDataChanged: (() -> Unit)? = null

    init {
        load()
        if (users.isEmpty() && receipts.isEmpty() && readings.isEmpty()) {
            seedInitialData()
            save()
        }
        seedDefaultServicesIfNeeded()
    }

    fun saveUser(user: ElectricUser, statePeriod: String = currentPeriod()) {
        val normalizedPeriod = statePeriod.ifBlank { currentPeriod() }
        val normalizedUser = user.withStateForPeriod(
            period = normalizedPeriod,
            active = user.isActive,
            residual = user.isResidual && user.isActive
        )

        if (normalizedUser.isResidualInPeriod(normalizedPeriod)) {
            users.indices.forEach { index ->
                val existing = users[index]
                if (existing.userId != normalizedUser.userId && existing.isResidualInPeriod(normalizedPeriod)) {
                    users[index] = existing.withStateForPeriod(
                        period = normalizedPeriod,
                        active = existing.isActiveInPeriod(normalizedPeriod),
                        residual = false
                    )
                }
            }
        }
        val index = users.indexOfFirst { it.userId == normalizedUser.userId }
        if (index >= 0) users[index] = normalizedUser else users.add(normalizedUser)
        save()
    }

    fun deleteUser(user: ElectricUser) {
        val period = currentPeriod()
        saveUser(
            user = user.copy(isActive = false, isResidual = false),
            statePeriod = period
        )
        save()
    }

    fun saveReceipt(receipt: MonthlyReceipt, originalPeriod: String? = null) {
        val periodToReplace = originalPeriod?.takeIf { it.isNotBlank() } ?: receipt.period
        val index = receipts.indexOfFirst { it.period == periodToReplace }
        if (index >= 0) receipts[index] = receipt else receipts.add(receipt)
        receipts.sortByDescending { it.period }
        save()
    }

    fun deleteReceipt(receipt: MonthlyReceipt) {
        receipts.removeAll { it.period == receipt.period }
        save()
    }

    fun saveReading(reading: MeterReading) {
        val normalized = if (reading.id.isBlank()) {
            reading.copy(id = UUID.randomUUID().toString())
        } else {
            reading
        }
        val index = readings.indexOfFirst { it.id == normalized.id }
        if (index >= 0) readings[index] = normalized else readings.add(normalized)
        readings.sortWith(compareByDescending<MeterReading> { it.period }.thenBy { it.userId })
        save()
    }

    fun deleteReading(reading: MeterReading) {
        readings.removeAll { it.id == reading.id }
        save()
    }

    fun saveServiceExpense(expense: ServiceExpense) {
        val normalized = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString())
        } else {
            expense
        }
        val index = serviceExpenses.indexOfFirst { it.id == normalized.id }
        if (index >= 0) serviceExpenses[index] = normalized else serviceExpenses.add(normalized)
        serviceExpenses.sortWith(compareByDescending<ServiceExpense> { it.period }.thenBy { it.name })
        save()
    }

    fun deleteServiceExpense(expense: ServiceExpense) {
        serviceExpenses.removeAll { it.id == expense.id }
        save()
    }

    fun savePayment(payment: UserPayment) {
        val normalized = payment.copy(
            id = payment.id.ifBlank { "${payment.period}|${payment.userId}" },
            amountPaid = payment.amountPaid.coerceAtLeast(0.0)
        )
        val index = payments.indexOfFirst {
            it.period == normalized.period && it.userId == normalized.userId
        }
        if (index >= 0) payments[index] = normalized else payments.add(normalized)
        payments.sortWith(compareByDescending<UserPayment> { it.period }.thenBy { it.userId })
        save()
    }

    fun deletePayment(period: String, userId: String) {
        payments.removeAll { it.period == period && it.userId == userId }
        save()
    }

    fun setAmoled(enabled: Boolean) {
        isAmoled.value = enabled
        prefs.edit().putBoolean(KEY_AMOLED, enabled).apply()
    }

    fun saveSettings(next: AppSettings) {
        settings.value = next.copy(reminderDay = next.reminderDay.coerceIn(1, 28))
        prefs.edit()
            .putFloat(KEY_IGV_RATE, settings.value.igvRate.toFloat())
            .putBoolean(KEY_ROUND_UP_TO_TENTH, settings.value.roundUpToTenth)
            .putString(KEY_SUPPLY_ALIAS, settings.value.supplyAlias)
            .putString(KEY_ACCOUNT_HOLDER, settings.value.accountHolder)
            .putBoolean(KEY_MONTHLY_REMINDER_ENABLED, settings.value.monthlyReminderEnabled)
            .putInt(KEY_REMINDER_DAY, settings.value.reminderDay)
            .putString(KEY_GOOGLE_SHEET_ID, settings.value.googleSheetId)
            .putString(KEY_GOOGLE_SHEET_NAME, settings.value.googleSheetName)
            .putString(KEY_GOOGLE_SHEET_UPDATED_AT, settings.value.googleSheetUpdatedAt)
            .putString(KEY_UPDATE_REPOSITORY_URL, settings.value.updateRepositoryUrl)
            .apply()
        onDataChanged?.invoke()
    }

    fun exportSyncPayload(): JSONObject = JSONObject()
        .put("users", JSONArray(users.map { it.toJson() }))
        .put("receipts", JSONArray(receipts.map { it.toJson() }))
        .put("readings", JSONArray(readings.map { it.toJson() }))
        .put("services", JSONArray(serviceExpenses.map { it.toJson() }))
        .put("payments", JSONArray(payments.map { it.toJson() }))
        .put("settings", settings.value.toJson())

    fun replaceFromSyncPayload(payload: JSONObject) {
        val importedUsers = parseObjects(payload.optJSONArray("users")).map { it.toElectricUser() }
        val importedReceipts = parseObjects(payload.optJSONArray("receipts")).map { it.toMonthlyReceipt() }
        val importedReadings = parseObjects(payload.optJSONArray("readings")).map { it.toMeterReading() }
        val importedServices = parseObjects(payload.optJSONArray("services")).map { it.toServiceExpense() }
        val importedPayments = parseObjects(payload.optJSONArray("payments")).mapNotNull { it.toUserPaymentOrNull() }
        replaceAllData(
            importedUsers = importedUsers,
            importedReceipts = importedReceipts,
            importedReadings = importedReadings,
            importedServices = importedServices,
            importedPayments = importedPayments
        )
        payload.optJSONObject("settings")?.let { next ->
            saveSettings(
                AppSettings(
                    igvRate = next.optDouble("igvRate", 0.18),
                    roundUpToTenth = next.optBoolean("roundUpToTenth", true),
                    supplyAlias = next.optString("supplyAlias"),
                    accountHolder = next.optString("accountHolder"),
                    monthlyReminderEnabled = next.optBoolean("monthlyReminderEnabled", false),
                    reminderDay = next.optInt("reminderDay", 25).coerceIn(1, 28),
                    googleSheetId = next.optString("googleSheetId"),
                    googleSheetName = next.optString("googleSheetName"),
                    googleSheetUpdatedAt = next.optString("googleSheetUpdatedAt"),
                    updateRepositoryUrl = next.optString("updateRepositoryUrl", DEFAULT_UPDATE_REPOSITORY_URL)
                )
            )
        }
    }

    fun replaceAllData(
        importedUsers: List<ElectricUser>,
        importedReceipts: List<MonthlyReceipt>,
        importedReadings: List<MeterReading>,
        importedServices: List<ServiceExpense>,
        importedPayments: List<UserPayment> = emptyList()
    ) {
        users.clear()
        receipts.clear()
        readings.clear()
        serviceExpenses.clear()
        payments.clear()

        users.addAll(importedUsers)
        receipts.addAll(importedReceipts.sortedByDescending { it.period })
        readings.addAll(importedReadings.sortedWith(compareByDescending<MeterReading> { it.period }.thenBy { it.userId }))
        serviceExpenses.addAll(
            importedServices
                .filter { it.amount > 0.0 }
                .sortedWith(compareByDescending<ServiceExpense> { it.period }.thenBy { it.name })
        )
        payments.addAll(
            importedPayments
                .filter { it.period.isNotBlank() && it.userId.isNotBlank() }
                .sortedWith(compareByDescending<UserPayment> { it.period }.thenBy { it.userId })
        )
        save()
    }

    fun mergeAllData(
        importedUsers: List<ElectricUser>,
        importedReceipts: List<MonthlyReceipt>,
        importedReadings: List<MeterReading>,
        importedServices: List<ServiceExpense>,
        importedPayments: List<UserPayment> = emptyList()
    ) {
        val mergedUsers = (users + importedUsers)
            .filter { it.userId.isNotBlank() }
            .associateBy { it.userId }
            .values
            .sortedBy { it.userId }

        val mergedReceipts = (receipts + importedReceipts)
            .filter { it.period.isNotBlank() }
            .associateBy { it.period }
            .values
            .sortedByDescending { it.period }

        val mergedReadings = (readings + importedReadings)
            .filter { it.period.isNotBlank() && it.userId.isNotBlank() }
            .associateBy { reading ->
                reading.id.ifBlank { "${reading.period}|${reading.userId}|${reading.internalReadingDate}" }
            }
            .values
            .sortedWith(compareByDescending<MeterReading> { it.period }.thenBy { it.userId })

        val mergedServices = (serviceExpenses + importedServices)
            .filter { it.period.isNotBlank() && it.name.isNotBlank() && it.amount > 0.0 }
            .associateBy { service ->
                service.id.ifBlank { "${service.period}|${service.name}" }
            }
            .values
            .sortedWith(compareByDescending<ServiceExpense> { it.period }.thenBy { it.name })

        val mergedPayments = (payments + importedPayments)
            .filter { it.period.isNotBlank() && it.userId.isNotBlank() }
            .associateBy { it.period to it.userId }
            .values
            .sortedWith(compareByDescending<UserPayment> { it.period }.thenBy { it.userId })

        users.clear()
        receipts.clear()
        readings.clear()
        serviceExpenses.clear()
        payments.clear()

        users.addAll(mergedUsers)
        receipts.addAll(mergedReceipts)
        readings.addAll(mergedReadings)
        serviceExpenses.addAll(mergedServices)
        payments.addAll(mergedPayments)
        save()
    }

    private fun load() {
        users.clear()
        receipts.clear()
        readings.clear()
        serviceExpenses.clear()
        payments.clear()

        val roomUsers = dao.getUsers().map { it.toElectricUser() }
        val roomReceipts = dao.getReceipts().map { it.toMonthlyReceipt() }
        val roomReadings = dao.getReadings().map { it.toMeterReading() }
        val roomServices = dao.getServices().map { it.toServiceExpense() }
        val roomPayments = dao.getPayments().map { it.toUserPayment() }

        if (
            roomUsers.isEmpty() &&
            roomReceipts.isEmpty() &&
            roomReadings.isEmpty() &&
            roomServices.isEmpty() &&
            roomPayments.isEmpty()
        ) {
            loadLegacyJson()
            if (users.isNotEmpty() || receipts.isNotEmpty() || readings.isNotEmpty() || serviceExpenses.isNotEmpty()) {
                save()
            }
        } else {
            users.addAll(roomUsers)
            receipts.addAll(roomReceipts)
            readings.addAll(roomReadings)
            serviceExpenses.addAll(roomServices)
            payments.addAll(roomPayments)
        }
        migrateResidualFromLegacyReadings()
        enforceSingleResidualUser()
    }

    private fun save() {
        dao.clearUsers()
        dao.clearReceipts()
        dao.clearReadings()
        dao.clearServices()
        dao.clearPayments()
        dao.upsertUsers(users.map { it.toEntity() })
        dao.upsertReceipts(receipts.map { it.toEntity() })
        dao.upsertReadings(readings.map { it.toEntity() })
        dao.upsertServices(serviceExpenses.map { it.toEntity() })
        dao.upsertPayments(payments.map { it.toEntity() })
        onDataChanged?.invoke()
    }

    private fun loadLegacyJson() {
        parseArray(prefs.getString(KEY_USERS, "[]")).forEach { users.add(it.toElectricUser()) }
        parseArray(prefs.getString(KEY_RECEIPTS, "[]")).forEach { receipts.add(it.toMonthlyReceipt()) }
        parseArray(prefs.getString(KEY_READINGS, "[]")).forEach { readings.add(it.toMeterReading()) }
        parseArray(prefs.getString(KEY_SERVICE_EXPENSES, "[]")).forEach { serviceExpenses.add(it.toServiceExpense()) }
    }

    private fun seedInitialData() {
        users.add(
            ElectricUser(
                userId = "U01",
                name = "Edwar",
                internalMeter = "Medidor interno 1",
                isActive = true,
                periodStates = listOf(UserPeriodState("", true, false)),
                notes = "Usuario calculado por lectura interna"
            )
        )
        users.add(
            ElectricUser(
                userId = "U02",
                name = "Gerard",
                internalMeter = "Medidor interno 2",
                isActive = true,
                isResidual = true,
                periodStates = listOf(UserPeriodState("", true, true)),
                notes = "Usuario residual inicial"
            )
        )
    }

    private fun parseArray(raw: String?): List<JSONObject> {
        val array = JSONArray(raw ?: "[]")
        return List(array.length()) { index -> array.getJSONObject(index) }
    }

    private fun ElectricUser.toEntity(): UserEntity = UserEntity(
        userId = userId,
        name = name,
        internalMeter = internalMeter,
        isActive = isActive,
        isResidual = isResidual,
        periodStatesJson = JSONArray(periodStates.map { it.toJson() }).toString(),
        notes = notes
    )

    private fun MonthlyReceipt.toEntity(): ReceiptEntity = ReceiptEntity(
        period = period,
        externalReadingDate = externalReadingDate,
        supplyNumber = supplyNumber,
        externalKwh = externalKwh,
        monthlyBill = monthlyBill,
        priceKwhUpTo30 = priceKwhUpTo30,
        priceKwhOver30 = priceKwhOver30,
        fixedCharge = fixedCharge,
        maintenance = maintenance,
        publicLighting = publicLighting,
        ruralElectrification = ruralElectrification,
        notes = notes
    )

    private fun MeterReading.toEntity(): ReadingEntity = ReadingEntity(
        id = id,
        period = period,
        userId = userId,
        isResidual = isResidual,
        internalReadingDate = internalReadingDate,
        previousReading = previousReading,
        currentReading = currentReading,
        notes = notes
    )

    private fun ServiceExpense.toEntity(): ServiceEntity = ServiceEntity(
        id = id,
        period = period,
        name = name,
        amount = amount,
        isActive = isActive,
        splitCost = splitCost,
        participantCount = participantCount,
        participantUserIdsJson = JSONArray(participantUserIds).toString(),
        notes = notes
    )

    private fun UserPayment.toEntity(): PaymentEntity = PaymentEntity(
        id = id,
        period = period,
        userId = userId,
        status = status.name,
        amountPaid = amountPaid,
        paymentDate = paymentDate,
        notes = notes
    )

    private fun UserPayment.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("period", period)
        .put("userId", userId)
        .put("status", status.name)
        .put("amountPaid", amountPaid)
        .put("paymentDate", paymentDate)
        .put("notes", notes)

    private fun AppSettings.toJson(): JSONObject = JSONObject()
        .put("igvRate", igvRate)
        .put("roundUpToTenth", roundUpToTenth)
        .put("supplyAlias", supplyAlias)
        .put("accountHolder", accountHolder)
        .put("monthlyReminderEnabled", monthlyReminderEnabled)
        .put("reminderDay", reminderDay)
        .put("googleSheetId", googleSheetId)
        .put("googleSheetName", googleSheetName)
        .put("googleSheetUpdatedAt", googleSheetUpdatedAt)
        .put("updateRepositoryUrl", updateRepositoryUrl)

    private fun UserEntity.toElectricUser(): ElectricUser = ElectricUser(
        userId = userId,
        name = name,
        internalMeter = internalMeter,
        isActive = isActive,
        isResidual = isResidual,
        periodStates = parsePeriodStates(JSONArray(periodStatesJson)),
        notes = notes
    )

    private fun ReceiptEntity.toMonthlyReceipt(): MonthlyReceipt = MonthlyReceipt(
        period = period,
        externalReadingDate = externalReadingDate,
        supplyNumber = supplyNumber,
        externalKwh = externalKwh,
        monthlyBill = monthlyBill,
        priceKwhUpTo30 = priceKwhUpTo30,
        priceKwhOver30 = priceKwhOver30,
        fixedCharge = fixedCharge,
        maintenance = maintenance,
        publicLighting = publicLighting,
        ruralElectrification = ruralElectrification,
        notes = notes
    )

    private fun ReadingEntity.toMeterReading(): MeterReading = MeterReading(
        id = id,
        period = period,
        userId = userId,
        isResidual = isResidual,
        internalReadingDate = internalReadingDate,
        previousReading = previousReading,
        currentReading = currentReading,
        notes = notes
    )

    private fun ServiceEntity.toServiceExpense(): ServiceExpense = ServiceExpense(
        id = id,
        period = period,
        name = name,
        amount = amount,
        isActive = isActive,
        splitCost = splitCost,
        participantCount = participantCount.coerceAtLeast(1),
        participantUserIds = parseStringArray(JSONArray(participantUserIdsJson)),
        notes = notes
    )

    private fun PaymentEntity.toUserPayment(): UserPayment = UserPayment(
        id = id,
        period = period,
        userId = userId,
        status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.UNPAID),
        amountPaid = amountPaid.coerceAtLeast(0.0),
        paymentDate = paymentDate,
        notes = notes
    )

    private fun ElectricUser.toJson(): JSONObject = JSONObject()
        .put("userId", userId)
        .put("name", name)
        .put("internalMeter", internalMeter)
        .put("isActive", isActive)
        .put("isResidual", isResidual)
        .put("periodStates", JSONArray(periodStates.map { it.toJson() }))
        .put("notes", notes)

    private fun MonthlyReceipt.toJson(): JSONObject = JSONObject()
        .put("period", period)
        .put("externalReadingDate", externalReadingDate)
        .put("supplyNumber", supplyNumber)
        .put("externalKwh", externalKwh)
        .put("monthlyBill", monthlyBill)
        .put("priceKwhUpTo30", priceKwhUpTo30)
        .put("priceKwhOver30", priceKwhOver30)
        .put("fixedCharge", fixedCharge)
        .put("maintenance", maintenance)
        .put("publicLighting", publicLighting)
        .put("ruralElectrification", ruralElectrification)
        .put("notes", notes)

    private fun MeterReading.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("period", period)
        .put("userId", userId)
        .put("isResidual", isResidual)
        .put("internalReadingDate", internalReadingDate)
        .put("previousReading", previousReading)
        .put("currentReading", currentReading)
        .put("notes", notes)

    private fun ServiceExpense.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("period", period)
        .put("name", name)
        .put("amount", amount)
        .put("isActive", isActive)
        .put("splitCost", splitCost)
        .put("participantCount", participantCount)
        .put("participantUserIds", JSONArray(participantUserIds))
        .put("notes", notes)

    private fun JSONObject.toElectricUser(): ElectricUser = ElectricUser(
        userId = optString("userId"),
        name = optString("name"),
        internalMeter = optString("internalMeter"),
        isActive = optBoolean("isActive", true),
        isResidual = optBoolean("isResidual", optString("notes").contains("residual", ignoreCase = true)),
        periodStates = parsePeriodStates(optJSONArray("periodStates")),
        notes = optString("notes")
    )

    private fun UserPeriodState.toJson(): JSONObject = JSONObject()
        .put("period", period)
        .put("isActive", isActive)
        .put("isResidual", isResidual)

    private fun JSONObject.toUserPeriodState(): UserPeriodState = UserPeriodState(
        period = optString("period"),
        isActive = optBoolean("isActive", true),
        isResidual = optBoolean("isResidual", false)
    )

    private fun parsePeriodStates(array: JSONArray?): List<UserPeriodState> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.getJSONObject(index).toUserPeriodState() }
    }

    private fun JSONObject.toMonthlyReceipt(): MonthlyReceipt = MonthlyReceipt(
        period = optString("period"),
        externalReadingDate = optString("externalReadingDate"),
        supplyNumber = optString("supplyNumber"),
        externalKwh = optDouble("externalKwh", 0.0),
        monthlyBill = optDouble("monthlyBill", 0.0),
        priceKwhUpTo30 = optDouble("priceKwhUpTo30", 0.0),
        priceKwhOver30 = optDouble("priceKwhOver30", 0.0),
        fixedCharge = optDouble("fixedCharge", 0.0),
        maintenance = optDouble("maintenance", 0.0),
        publicLighting = optDouble("publicLighting", 0.0),
        ruralElectrification = optDouble("ruralElectrification", 0.0),
        notes = optString("notes")
    )

    private fun JSONObject.toMeterReading(): MeterReading = MeterReading(
        id = optString("id"),
        period = optString("period"),
        userId = optString("userId"),
        isResidual = optBoolean("isResidual", false),
        internalReadingDate = optString("internalReadingDate"),
        previousReading = nullableDouble("previousReading"),
        currentReading = nullableDouble("currentReading"),
        notes = optString("notes")
    )

    private fun JSONObject.toServiceExpense(): ServiceExpense = ServiceExpense(
        id = optString("id"),
        period = optString("period"),
        name = optString("name"),
        amount = optDouble("amount", 0.0),
        isActive = optBoolean("isActive", false),
        splitCost = optBoolean("splitCost", true),
        participantCount = optInt("participantCount", 1).coerceAtLeast(1),
        participantUserIds = parseStringArray(optJSONArray("participantUserIds")),
        notes = optString("notes")
    )

    private fun JSONObject.toUserPaymentOrNull(): UserPayment? {
        val period = optString("period")
        val userId = optString("userId")
        if (period.isBlank() || userId.isBlank()) return null
        val status = runCatching {
            PaymentStatus.valueOf(optString("status", PaymentStatus.UNPAID.name))
        }.getOrDefault(PaymentStatus.UNPAID)
        return UserPayment(
            id = optString("id", "$period|$userId"),
            period = period,
            userId = userId,
            status = status,
            amountPaid = optDouble("amountPaid", 0.0).coerceAtLeast(0.0),
            paymentDate = optString("paymentDate"),
            notes = optString("notes")
        )
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
    }

    private fun parseObjects(array: JSONArray?): List<JSONObject> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
    }

    private fun JSONObject.nullableDouble(key: String): Double? {
        return if (isNull(key) || !has(key)) null else optDouble(key)
    }

    private fun migrateResidualFromLegacyReadings() {
        if (users.any { it.isResidual }) return
        val residualUserId = readings.firstOrNull { it.isResidual }?.userId ?: return
        val index = users.indexOfFirst { it.userId == residualUserId }
        if (index >= 0) {
            users[index] = users[index].withStateForPeriod("", users[index].isActive, true)
        }
    }

    private fun enforceSingleResidualUser() {
        var foundResidual = false
        users.indices.forEach { index ->
            val user = users[index]
            if (user.isResidual) {
                if (foundResidual) {
                    users[index] = user.withStateForPeriod("", user.isActive, false)
                } else {
                    foundResidual = true
                }
            }
        }
    }

    private fun seedDefaultServicesIfNeeded() {
        if (prefs.getBoolean(KEY_DEFAULT_SERVICES_SEEDED, false) || serviceExpenses.isNotEmpty()) return
        prefs.edit().putBoolean(KEY_DEFAULT_SERVICES_SEEDED, true).apply()
    }

    private fun loadSettings(): AppSettings = AppSettings(
        igvRate = prefs.getFloat(KEY_IGV_RATE, 0.18f).toDouble(),
        roundUpToTenth = prefs.getBoolean(KEY_ROUND_UP_TO_TENTH, true),
        supplyAlias = prefs.getString(KEY_SUPPLY_ALIAS, "").orEmpty(),
        accountHolder = prefs.getString(KEY_ACCOUNT_HOLDER, "").orEmpty(),
        monthlyReminderEnabled = prefs.getBoolean(KEY_MONTHLY_REMINDER_ENABLED, false),
        reminderDay = prefs.getInt(KEY_REMINDER_DAY, 25).coerceIn(1, 28),
        googleSheetId = prefs.getString(KEY_GOOGLE_SHEET_ID, "").orEmpty(),
        googleSheetName = prefs.getString(KEY_GOOGLE_SHEET_NAME, "").orEmpty(),
        googleSheetUpdatedAt = prefs.getString(KEY_GOOGLE_SHEET_UPDATED_AT, "").orEmpty(),
        updateRepositoryUrl = prefs.getString(KEY_UPDATE_REPOSITORY_URL, DEFAULT_UPDATE_REPOSITORY_URL).orEmpty()
    )

    private fun currentPeriod(): String = YearMonth.now().toString()

    private companion object {
        const val KEY_USERS = "users"
        const val KEY_RECEIPTS = "receipts"
        const val KEY_READINGS = "readings"
        const val KEY_SERVICE_EXPENSES = "service_expenses"
        const val KEY_DEFAULT_SERVICES_SEEDED = "default_services_seeded"
        const val KEY_AMOLED = "amoled"
        const val KEY_IGV_RATE = "igv_rate"
        const val KEY_ROUND_UP_TO_TENTH = "round_up_to_tenth"
        const val KEY_SUPPLY_ALIAS = "supply_alias"
        const val KEY_ACCOUNT_HOLDER = "account_holder"
        const val KEY_MONTHLY_REMINDER_ENABLED = "monthly_reminder_enabled"
        const val KEY_REMINDER_DAY = "reminder_day"
        const val KEY_GOOGLE_SHEET_ID = "google_sheet_id"
        const val KEY_GOOGLE_SHEET_NAME = "google_sheet_name"
        const val KEY_GOOGLE_SHEET_UPDATED_AT = "google_sheet_updated_at"
        const val KEY_UPDATE_REPOSITORY_URL = "update_repository_url"
        const val DEFAULT_UPDATE_REPOSITORY_URL = "https://github.com/Gwr4rd/Control-electrico"
    }
}
