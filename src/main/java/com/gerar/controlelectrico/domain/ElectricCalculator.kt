package com.gerar.controlelectrico.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ElectricCalculator {
    private const val DEFAULT_IGV_RATE = 0.18

    fun calculatePeriod(
        period: String,
        users: List<ElectricUser>,
        receipt: MonthlyReceipt?,
        readings: List<MeterReading>,
        services: List<ServiceExpense> = emptyList(),
        settings: AppSettings = AppSettings()
    ): PeriodSummary {
        if (receipt == null || period.isBlank()) {
            return emptySummary(period, "Falta registrar el recibo del periodo")
        }

        val activeUsers = users.filter { it.isActiveInPeriod(period) }
        val participants = activeUsers.size
        if (participants == 0) {
            return emptySummary(period, "Falta registrar usuarios activos")
        }

        val periodReadings = readings.filter { it.period == period }
        val readingByUserId = periodReadings.associateBy { it.userId }
        val residualCount = activeUsers.count { it.isResidualInPeriod(period) }
        val residualStatus = when (residualCount) {
            0 -> "OK: sin residual"
            1 -> "OK: 1 residual"
            else -> "Error: mas de un residual"
        }

        val usesBlockTariff = receipt.priceKwhUpTo30 > 0.0 &&
            receipt.priceKwhOver30 > 0.0 &&
            receipt.priceKwhUpTo30 != receipt.priceKwhOver30
        val threshold = if (usesBlockTariff) safeDivide(30.0, participants.toDouble()) else 0.0
        val fixedPerUser = safeDivide(
            receipt.fixedCharge + receipt.maintenance + receipt.publicLighting,
            participants.toDouble()
        )
        val ruralPerUser = safeDivide(receipt.ruralElectrification, participants.toDouble())
        val activeServices = services.filter { it.period == period && it.isActive && it.amount > 0.0 }
        val serviceExpensesTotal = activeServices.sumOf { it.amount }
        val serviceSharePerParticipant = activeServices.sumOf { service ->
            genericServiceShare(service)
        }

        val normalUsers = activeUsers.filterNot { it.isResidualInPeriod(period) }
        val normalConsumptionByUserId = normalUsers.associate { user ->
            val reading = readingByUserId[user.userId]
            user.userId to if (reading == null) 0.0 else normalConsumption(reading)
        }
        val normalConsumptionTotal = normalConsumptionByUserId.values.sum()

        val intermediate = activeUsers.map { user ->
            val reading = readingByUserId[user.userId]
            val isUserResidual = user.isResidualInPeriod(period)
            val consumption = if (isUserResidual) {
                max(receipt.externalKwh - normalConsumptionTotal, 0.0)
            } else {
                normalConsumptionByUserId[user.userId] ?: 0.0
            }
            val missingReadingNote = if (!isUserResidual && reading == null) {
                "Sin lectura registrada en el periodo"
            } else {
                ""
            }

            val energyAmount = calculateEnergyAmount(
                receipt = receipt,
                consumption = consumption,
                threshold = threshold,
                settings = settings
            )
            val subtotal = energyAmount + fixedPerUser
            val igv = subtotal * settings.igvRate
            val totalBeforeRounding = subtotal + igv + ruralPerUser
            val serviceShare = activeServices.sumOf { service ->
                serviceShareForUser(service, user.userId)
            }
            val finalElectricTotal = roundPayment(totalBeforeRounding, settings)

            PaymentResult(
                userId = user.userId,
                userName = user.name,
                internalMeter = user.internalMeter,
                isResidual = isUserResidual,
                consumptionKwh = consumption,
                thresholdKwh = threshold,
                energyAmount = energyAmount,
                fixedCharges = fixedPerUser,
                subtotal = subtotal,
                igv = igv,
                ruralElectrification = ruralPerUser,
                totalBeforeRounding = totalBeforeRounding,
                finalTotal = if (isUserResidual) 0.0 else finalElectricTotal,
                serviceShare = serviceShare,
                finalTotalWithServices = ceilToTenth(
                    (if (isUserResidual) 0.0 else finalElectricTotal) + serviceShare
                ),
                notes = listOf(reading?.notes.orEmpty(), missingReadingNote)
                    .filter { it.isNotBlank() }
                    .joinToString(" - ")
            )
        }

        val normalTotalBeforeRounding = intermediate
            .filterNot { it.isResidual }
            .sumOf { it.totalBeforeRounding }

        val results = intermediate.map { result ->
            if (!result.isResidual) {
                result
            } else {
                val residualTotal = roundPayment(max(receipt.monthlyBill - normalTotalBeforeRounding, 0.0), settings)
                result.copy(
                    finalTotal = residualTotal,
                    finalTotalWithServices = ceilToTenth(residualTotal + result.serviceShare),
                    notes = result.notes.ifBlank { "Pago residual = recibo - suma de otros usuarios" }
                )
            }
        }

        val totalAssigned = results.sumOf { it.finalTotal }
        return PeriodSummary(
            period = period,
            participants = participants,
            thresholdKwhPerUser = threshold,
            fixedChargesPerUser = fixedPerUser,
            ruralElectrificationPerUser = ruralPerUser,
            serviceExpensesTotal = serviceExpensesTotal,
            serviceSharePerParticipant = serviceSharePerParticipant,
            totalAssigned = totalAssigned,
            totalAssignedWithServices = totalAssigned + serviceExpensesTotal,
            receiptDifference = receipt.monthlyBill - totalAssigned,
            residualStatus = residualStatus,
            results = results.sortedBy { it.userId }
        )
    }

    private fun calculateEnergyAmount(
        receipt: MonthlyReceipt,
        consumption: Double,
        threshold: Double,
        settings: AppSettings
    ): Double {
        if (receipt.priceKwhUpTo30 <= 0.0 || receipt.priceKwhOver30 <= 0.0) {
            return consumption * estimatedUnitPrice(receipt, settings)
        }
        val firstBlock = min(consumption, threshold) * receipt.priceKwhUpTo30
        val secondBlock = max(consumption - threshold, 0.0) * receipt.priceKwhOver30
        return firstBlock + secondBlock
    }

    private fun estimatedUnitPrice(receipt: MonthlyReceipt, settings: AppSettings): Double {
        val divisor = (1.0 + settings.igvRate).takeIf { it > 0.0 } ?: 1.0
        val fixedTotal = receipt.fixedCharge + receipt.maintenance + receipt.publicLighting
        val estimatedEnergyBase = ((receipt.monthlyBill - receipt.ruralElectrification).coerceAtLeast(0.0) / divisor) - fixedTotal
        return safeDivide(estimatedEnergyBase.coerceAtLeast(0.0), receipt.externalKwh.coerceAtLeast(0.0))
    }

    private fun normalConsumption(reading: MeterReading): Double {
        val previous = reading.previousReading ?: return 0.0
        val current = reading.currentReading ?: return 0.0
        return max(current - previous, 0.0)
    }

    private fun safeDivide(value: Double, divisor: Double): Double {
        return if (divisor == 0.0) 0.0 else value / divisor
    }

    private fun genericServiceShare(service: ServiceExpense): Double {
        return if (service.splitCost) {
            safeDivide(service.amount, service.participantCount.coerceAtLeast(1).toDouble())
        } else {
            service.amount
        }
    }

    private fun serviceShareForUser(service: ServiceExpense, userId: String): Double {
        val participantIds = service.participantUserIds.filter { it.isNotBlank() }
        if (participantIds.isEmpty()) return genericServiceShare(service)
        if (userId !in participantIds) return 0.0
        return if (service.splitCost) {
            safeDivide(service.amount, participantIds.size.coerceAtLeast(1).toDouble())
        } else {
            service.amount
        }
    }

    private fun ceilToTenth(value: Double): Double {
        return ceil(value * 10.0) / 10.0
    }

    private fun roundPayment(value: Double, settings: AppSettings): Double {
        return if (settings.roundUpToTenth) ceilToTenth(value) else kotlin.math.round(value * 100.0) / 100.0
    }

    private fun emptySummary(period: String, status: String): PeriodSummary {
        return PeriodSummary(
            period = period,
            participants = 0,
            thresholdKwhPerUser = 0.0,
            fixedChargesPerUser = 0.0,
            ruralElectrificationPerUser = 0.0,
            serviceExpensesTotal = 0.0,
            serviceSharePerParticipant = 0.0,
            totalAssigned = 0.0,
            totalAssignedWithServices = 0.0,
            receiptDifference = 0.0,
            residualStatus = status,
            results = emptyList()
        )
    }
}
