package com.gerar.controlelectrico.domain

import kotlin.math.max
import kotlin.math.min

object PaymentLedger {
    fun calculate(
        summaries: Map<String, PeriodSummary>,
        payments: List<UserPayment>
    ): Map<Pair<String, String>, PaymentBalance> {
        val paymentByPeriodAndUser = payments.associateBy { it.period to it.userId }
        val carriedByUser = mutableMapOf<String, List<DebtItem>>()
        val balances = mutableMapOf<Pair<String, String>, PaymentBalance>()

        summaries.keys.sorted().forEach { period ->
            summaries.getValue(period).results.forEach { result ->
                val key = period to result.userId
                val currentAmount = result.finalTotal + result.serviceShare
                val previousDebtItems = carriedByUser[result.userId].orEmpty()
                    .map { it.copy() }
                val previousBalance = previousDebtItems.sumOf { it.remainingAmount }
                val debtBeforePayment = buildList {
                    addAll(previousDebtItems)
                    if (currentAmount > 0.0) {
                        add(
                            DebtItem(
                                period = period,
                                originalAmount = currentAmount,
                                remainingAmount = currentAmount
                            )
                        )
                    }
                }
                val totalDue = debtBeforePayment.sumOf { it.remainingAmount }
                val payment = paymentByPeriodAndUser[key]
                val amountPaid = when (payment?.status) {
                    PaymentStatus.PAID -> totalDue
                    PaymentStatus.PARTIAL -> payment.amountPaid.coerceIn(0.0, totalDue)
                    PaymentStatus.UNPAID, null -> 0.0
                }
                var amountToApply = amountPaid
                val outstandingDebtItems = debtBeforePayment.mapNotNull { item ->
                    val applied = min(item.remainingAmount, amountToApply)
                    amountToApply -= applied
                    val remaining = max(item.remainingAmount - applied, 0.0)
                    item.copy(remainingAmount = remaining).takeIf { remaining > 0.005 }
                }
                val remainingBalance = outstandingDebtItems.sumOf { it.remainingAmount }

                balances[key] = PaymentBalance(
                    period = period,
                    userId = result.userId,
                    currentPeriodAmount = currentAmount,
                    previousBalance = previousBalance,
                    previousDebtItems = previousDebtItems,
                    totalDue = totalDue,
                    amountPaid = amountPaid,
                    remainingBalance = remainingBalance,
                    outstandingDebtItems = outstandingDebtItems,
                    status = payment?.status
                )
                carriedByUser[result.userId] = outstandingDebtItems
            }
        }
        return balances
    }
}
