package com.gerar.controlelectrico.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentLedgerTest {
    @Test
    fun unpaidAndPartialBalancesCarryToFollowingPeriods() {
        val summaries = mapOf(
            "2026-01" to summary("2026-01", 100.0),
            "2026-02" to summary("2026-02", 80.0),
            "2026-03" to summary("2026-03", 70.0)
        )
        val payments = listOf(
            payment("2026-01", PaymentStatus.UNPAID, 0.0),
            payment("2026-02", PaymentStatus.PARTIAL, 50.0),
            payment("2026-03", PaymentStatus.PAID, 200.0)
        )

        val balances = PaymentLedger.calculate(summaries, payments)

        assertEquals(100.0, balances.getValue("2026-01" to "U01").remainingBalance, 0.001)
        assertEquals(180.0, balances.getValue("2026-02" to "U01").totalDue, 0.001)
        assertEquals(130.0, balances.getValue("2026-02" to "U01").remainingBalance, 0.001)
        assertEquals(200.0, balances.getValue("2026-03" to "U01").totalDue, 0.001)
        assertEquals(0.0, balances.getValue("2026-03" to "U01").remainingBalance, 0.001)
    }

    @Test
    fun unregisteredHistoricalPeriodCreatesIdentifiedDebt() {
        val summaries = mapOf(
            "2026-01" to summary("2026-01", 100.0),
            "2026-02" to summary("2026-02", 80.0)
        )

        val balances = PaymentLedger.calculate(summaries, emptyList())

        assertEquals(100.0, balances.getValue("2026-01" to "U01").remainingBalance, 0.001)
        assertEquals(100.0, balances.getValue("2026-02" to "U01").previousBalance, 0.001)
        assertEquals(180.0, balances.getValue("2026-02" to "U01").totalDue, 0.001)
        assertEquals(
            listOf("2026-01", "2026-02"),
            balances.getValue("2026-02" to "U01").outstandingDebtItems.map { it.period }
        )
    }

    @Test
    fun partialPaymentIsAppliedToOldestDebtFirst() {
        val summaries = mapOf(
            "2026-01" to summary("2026-01", 100.0),
            "2026-02" to summary("2026-02", 80.0)
        )
        val payments = listOf(
            payment("2026-02", PaymentStatus.PARTIAL, 120.0)
        )

        val balance = PaymentLedger.calculate(summaries, payments)
            .getValue("2026-02" to "U01")

        assertEquals(60.0, balance.remainingBalance, 0.001)
        assertEquals(1, balance.outstandingDebtItems.size)
        assertEquals("2026-02", balance.outstandingDebtItems.single().period)
        assertEquals(60.0, balance.outstandingDebtItems.single().remainingAmount, 0.001)
    }

    private fun summary(period: String, total: Double): PeriodSummary {
        return PeriodSummary(
            period = period,
            participants = 1,
            thresholdKwhPerUser = 0.0,
            fixedChargesPerUser = 0.0,
            ruralElectrificationPerUser = 0.0,
            serviceExpensesTotal = 0.0,
            serviceSharePerParticipant = 0.0,
            totalAssigned = total,
            totalAssignedWithServices = total,
            receiptDifference = 0.0,
            residualStatus = "OK",
            results = listOf(
                PaymentResult(
                    userId = "U01",
                    userName = "Usuario",
                    internalMeter = "1",
                    isResidual = false,
                    consumptionKwh = 0.0,
                    thresholdKwh = 0.0,
                    energyAmount = 0.0,
                    fixedCharges = 0.0,
                    subtotal = 0.0,
                    igv = 0.0,
                    ruralElectrification = 0.0,
                    totalBeforeRounding = total,
                    finalTotal = total,
                    notes = ""
                )
            )
        )
    }

    private fun payment(period: String, status: PaymentStatus, amount: Double): UserPayment {
        return UserPayment(
            id = "$period|U01",
            period = period,
            userId = "U01",
            status = status,
            amountPaid = amount,
            paymentDate = "2026-01-01"
        )
    }
}
