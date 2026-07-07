package com.gerar.controlelectrico.domain

data class ElectricUser(
    val userId: String,
    val name: String,
    val internalMeter: String,
    val isActive: Boolean,
    val isResidual: Boolean = false,
    val periodStates: List<UserPeriodState> = emptyList(),
    val notes: String = ""
)

data class UserPeriodState(
    val period: String,
    val isActive: Boolean,
    val isResidual: Boolean
)

data class MonthlyReceipt(
    val period: String,
    val externalReadingDate: String,
    val supplyNumber: String,
    val externalKwh: Double,
    val monthlyBill: Double,
    val priceKwhUpTo30: Double,
    val priceKwhOver30: Double,
    val fixedCharge: Double,
    val maintenance: Double,
    val publicLighting: Double,
    val ruralElectrification: Double,
    val notes: String = ""
)

data class MeterReading(
    val id: String,
    val period: String,
    val userId: String,
    val isResidual: Boolean,
    val internalReadingDate: String,
    val previousReading: Double?,
    val currentReading: Double?,
    val notes: String = ""
)

data class ServiceExpense(
    val id: String,
    val period: String,
    val name: String,
    val amount: Double,
    val isActive: Boolean,
    val splitCost: Boolean,
    val participantCount: Int,
    val participantUserIds: List<String> = emptyList(),
    val notes: String = ""
)

enum class PaymentStatus {
    PAID,
    PARTIAL,
    UNPAID
}

data class UserPayment(
    val id: String,
    val period: String,
    val userId: String,
    val status: PaymentStatus,
    val amountPaid: Double,
    val paymentDate: String,
    val notes: String = ""
)

data class DebtItem(
    val period: String,
    val originalAmount: Double,
    val remainingAmount: Double
)

data class PaymentBalance(
    val period: String,
    val userId: String,
    val currentPeriodAmount: Double,
    val previousBalance: Double,
    val previousDebtItems: List<DebtItem>,
    val totalDue: Double,
    val amountPaid: Double,
    val remainingBalance: Double,
    val outstandingDebtItems: List<DebtItem>,
    val status: PaymentStatus?
)

data class AppSettings(
    val igvRate: Double = 0.18,
    val roundUpToTenth: Boolean = true,
    val supplyAlias: String = "",
    val accountHolder: String = "",
    val monthlyReminderEnabled: Boolean = false,
    val reminderDay: Int = 25
)

data class PaymentResult(
    val userId: String,
    val userName: String,
    val internalMeter: String,
    val isResidual: Boolean,
    val consumptionKwh: Double,
    val thresholdKwh: Double,
    val energyAmount: Double,
    val fixedCharges: Double,
    val subtotal: Double,
    val igv: Double,
    val ruralElectrification: Double,
    val totalBeforeRounding: Double,
    val finalTotal: Double,
    val serviceShare: Double = 0.0,
    val finalTotalWithServices: Double = finalTotal + serviceShare,
    val notes: String
)

data class PeriodSummary(
    val period: String,
    val participants: Int,
    val thresholdKwhPerUser: Double,
    val fixedChargesPerUser: Double,
    val ruralElectrificationPerUser: Double,
    val serviceExpensesTotal: Double,
    val serviceSharePerParticipant: Double,
    val totalAssigned: Double,
    val totalAssignedWithServices: Double,
    val receiptDifference: Double,
    val residualStatus: String,
    val results: List<PaymentResult>
)

fun ElectricUser.stateForPeriod(period: String): UserPeriodState {
    val matchingState = periodStates
        .filter { it.period.isNotBlank() && it.period <= period }
        .maxByOrNull { it.period }

    return matchingState ?: UserPeriodState(
        period = period,
        isActive = isActive,
        isResidual = isResidual
    )
}

fun ElectricUser.isActiveInPeriod(period: String): Boolean {
    return stateForPeriod(period).isActive
}

fun ElectricUser.isResidualInPeriod(period: String): Boolean {
    return stateForPeriod(period).isResidual
}

fun ElectricUser.withStateForPeriod(
    period: String,
    active: Boolean,
    residual: Boolean
): ElectricUser {
    if (period.isBlank()) {
        return copy(isActive = active, isResidual = residual)
    }

    val states = periodStates
        .filterNot { it.period == period }
        .plus(UserPeriodState(period = period, isActive = active, isResidual = residual))
        .sortedBy { it.period }

    return copy(
        isActive = active,
        isResidual = residual,
        periodStates = states
    )
}
