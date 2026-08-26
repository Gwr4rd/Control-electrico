export const ALL_USERS = "__all_users__";
export const APP_NAME = "Control Electrico";
export const CREATOR_WEBSITE = "https://github.com/Gwr4rd/Control-electrico";

export function stateForPeriod(user, period) {
  const states = (user.periodStates || [])
    .filter((state) => state.period && state.period <= period)
    .sort((a, b) => a.period.localeCompare(b.period));
  return states.at(-1) || {
    period,
    isActive: user.isActive !== false,
    isResidual: Boolean(user.isResidual)
  };
}

export function isActiveInPeriod(user, period) {
  return stateForPeriod(user, period).isActive;
}

export function isResidualInPeriod(user, period) {
  return stateForPeriod(user, period).isResidual;
}

export function serviceShareForUser(service, userId) {
  const participants = (service.participantUserIds || []).filter(Boolean);
  if (participants.length && !participants.includes(userId)) return 0;
  if (!service.splitCost) return Number(service.amount) || 0;
  const divisor = participants.length || Math.max(Number(service.participantCount) || 1, 1);
  return (Number(service.amount) || 0) / divisor;
}

function ceilTenth(value) {
  return Math.ceil((value + Number.EPSILON) * 10) / 10;
}

function roundPayment(value, settings) {
  return settings.roundUpToTenth !== false
    ? ceilTenth(value)
    : Math.round((value + Number.EPSILON) * 100) / 100;
}

export function calculatePeriod(period, data) {
  const receipt = data.receipts.find((item) => item.period === period);
  if (!receipt) return emptySummary(period, "Falta registrar el recibo del periodo");

  const activeUsers = data.users.filter((user) => isActiveInPeriod(user, period));
  if (!activeUsers.length) return emptySummary(period, "Falta registrar usuarios activos");

  const participants = activeUsers.length;
  const readingMap = new Map(
    data.readings.filter((reading) => reading.period === period).map((reading) => [reading.userId, reading])
  );
  const residualCount = activeUsers.filter((user) => isResidualInPeriod(user, period)).length;
  const usesBlockTariff =
    Number(receipt.priceKwhUpTo30) > 0 &&
    Number(receipt.priceKwhOver30) > 0 &&
    Number(receipt.priceKwhUpTo30) !== Number(receipt.priceKwhOver30);
  const threshold = usesBlockTariff ? 30 / participants : 0;
  const fixedPerUser =
    ((Number(receipt.fixedCharge) || 0) +
      (Number(receipt.maintenance) || 0) +
      (Number(receipt.publicLighting) || 0)) /
    participants;
  const ruralPerUser = (Number(receipt.ruralElectrification) || 0) / participants;
  const activeServices = data.services.filter(
    (service) => service.period === period && service.isActive !== false && Number(service.amount) > 0
  );

  const normalUsers = activeUsers.filter((user) => !isResidualInPeriod(user, period));
  const normalConsumption = new Map(
    normalUsers.map((user) => {
      const reading = readingMap.get(user.userId);
      const previous = Number(reading?.previousReading);
      const current = Number(reading?.currentReading);
      const value =
        Number.isFinite(previous) && Number.isFinite(current) ? Math.max(current - previous, 0) : 0;
      return [user.userId, value];
    })
  );
  const normalTotal = [...normalConsumption.values()].reduce((sum, value) => sum + value, 0);
  const igvRate = Number(data.settings.igvRate) || 0.18;

  let results = activeUsers.map((user) => {
    const residual = isResidualInPeriod(user, period);
    const consumption = residual
      ? Math.max((Number(receipt.externalKwh) || 0) - normalTotal, 0)
      : normalConsumption.get(user.userId) || 0;
    const energyAmount = calculateEnergy(receipt, consumption, threshold, igvRate);
    const subtotal = energyAmount + fixedPerUser;
    const igv = subtotal * igvRate;
    const beforeRounding = subtotal + igv + ruralPerUser;
    const serviceShare = activeServices.reduce(
      (sum, service) => sum + serviceShareForUser(service, user.userId),
      0
    );
    const electricTotal = roundPayment(beforeRounding, data.settings);
    return {
      userId: user.userId,
      userName: user.name,
      internalMeter: user.internalMeter,
      isResidual: residual,
      consumptionKwh: consumption,
      thresholdKwh: threshold,
      energyAmount,
      fixedCharges: fixedPerUser,
      subtotal,
      igv,
      ruralElectrification: ruralPerUser,
      totalBeforeRounding: beforeRounding,
      finalTotal: residual ? 0 : electricTotal,
      serviceShare,
      finalTotalWithServices: (residual ? 0 : electricTotal) + serviceShare,
      notes: !residual && !readingMap.has(user.userId) ? "Sin lectura registrada" : ""
    };
  });

  const normalBeforeRounding = results
    .filter((result) => !result.isResidual)
    .reduce((sum, result) => sum + result.totalBeforeRounding, 0);
  results = results.map((result) => {
    if (!result.isResidual) return result;
    const residualTotal = roundPayment(
      Math.max((Number(receipt.monthlyBill) || 0) - normalBeforeRounding, 0),
      data.settings
    );
    return {
      ...result,
      finalTotal: residualTotal,
      finalTotalWithServices: residualTotal + result.serviceShare
    };
  });

  const totalAssigned = results.reduce((sum, result) => sum + result.finalTotal, 0);
  const servicesTotal = activeServices.reduce((sum, service) => sum + Number(service.amount), 0);
  return {
    period,
    participants,
    thresholdKwhPerUser: threshold,
    fixedChargesPerUser: fixedPerUser,
    ruralElectrificationPerUser: ruralPerUser,
    serviceExpensesTotal: servicesTotal,
    totalAssigned,
    totalAssignedWithServices: totalAssigned + servicesTotal,
    receiptDifference: (Number(receipt.monthlyBill) || 0) - totalAssigned,
    residualStatus:
      residualCount === 0 ? "Sin residual" : residualCount === 1 ? "1 usuario residual" : "Error: más de un residual",
    results: results.sort((a, b) => a.userId.localeCompare(b.userId))
  };
}

function calculateEnergy(receipt, consumption, threshold, igvRate) {
  const firstRate = Number(receipt.priceKwhUpTo30) || 0;
  const secondRate = Number(receipt.priceKwhOver30) || 0;
  if (firstRate > 0 && secondRate > 0) {
    return Math.min(consumption, threshold) * firstRate + Math.max(consumption - threshold, 0) * secondRate;
  }
  if (firstRate > 0) return consumption * firstRate;
  const fixed =
    (Number(receipt.fixedCharge) || 0) +
    (Number(receipt.maintenance) || 0) +
    (Number(receipt.publicLighting) || 0);
  const base =
    Math.max((Number(receipt.monthlyBill) || 0) - (Number(receipt.ruralElectrification) || 0), 0) /
      (1 + igvRate) -
    fixed;
  const rate = (Number(receipt.externalKwh) || 0) > 0 ? Math.max(base, 0) / Number(receipt.externalKwh) : 0;
  return consumption * rate;
}

function emptySummary(period, residualStatus) {
  return {
    period,
    participants: 0,
    thresholdKwhPerUser: 0,
    fixedChargesPerUser: 0,
    ruralElectrificationPerUser: 0,
    serviceExpensesTotal: 0,
    totalAssigned: 0,
    totalAssignedWithServices: 0,
    receiptDifference: 0,
    residualStatus,
    results: []
  };
}

export function buildPaymentLedger(periods, data) {
  const carried = new Map();
  const ledger = new Map();
  const paymentMap = new Map(data.payments.map((payment) => [`${payment.period}|${payment.userId}`, payment]));
  [...periods].sort().forEach((period) => {
    const summary = calculatePeriod(period, data);
    summary.results.forEach((result) => {
      const key = `${period}|${result.userId}`;
      const currentPeriodAmount = result.finalTotal + result.serviceShare;
      const previousDebtItems = (carried.get(result.userId) || []).map((item) => ({ ...item }));
      const previousBalance = previousDebtItems.reduce((sum, item) => sum + item.remainingAmount, 0);
      const debtBeforePayment = [
        ...previousDebtItems,
        ...(currentPeriodAmount > 0
          ? [{ period, originalAmount: currentPeriodAmount, remainingAmount: currentPeriodAmount }]
          : [])
      ];
      const totalDue = debtBeforePayment.reduce((sum, item) => sum + item.remainingAmount, 0);
      const payment = paymentMap.get(key);
      let amountPaid = 0;
      if (payment?.status === "PAID") {
        amountPaid = totalDue;
      } else if (payment?.status === "PARTIAL") {
        amountPaid = Math.min(Math.max(Number(payment.amountPaid) || 0, 0), totalDue);
      }
      let amountToApply = amountPaid;
      const outstandingDebtItems = debtBeforePayment
        .map((item) => {
          const applied = Math.min(item.remainingAmount, amountToApply);
          amountToApply -= applied;
          return { ...item, remainingAmount: Math.max(item.remainingAmount - applied, 0) };
        })
        .filter((item) => item.remainingAmount > 0.005);
      const remainingBalance = outstandingDebtItems.reduce(
        (sum, item) => sum + item.remainingAmount,
        0
      );
      ledger.set(key, {
        period,
        userId: result.userId,
        currentPeriodAmount,
        previousBalance,
        previousDebtItems,
        totalDue,
        amountPaid,
        remainingBalance,
        outstandingDebtItems,
        status: payment?.status || null
      });
      carried.set(result.userId, outstandingDebtItems);
    });
  });
  return ledger;
}

export function balanceAmountForResult(period, result, ledger) {
  const balance = ledger.get(`${period}|${result.userId}`);
  return balance?.remainingBalance ?? result.finalTotalWithServices ?? result.finalTotal + result.serviceShare;
}

export function totalOutstandingForPeriod(period, results, ledger) {
  return results.reduce(
    (sum, result) => sum + balanceAmountForResult(period, result, ledger),
    0
  );
}

export function availablePeriods(data) {
  const readingPeriods = new Set(data.readings.map((reading) => reading.period));
  return data.receipts
    .map((receipt) => receipt.period)
    .filter((period) => readingPeriods.has(period))
    .filter((period, index, array) => array.indexOf(period) === index)
    .sort()
    .reverse();
}

export function buildSmartAlerts(data, period, ledger) {
  if (!period) return [];
  const alerts = [];
  const receipt = data.receipts.find((item) => item.period === period);
  const activeUsers = data.users.filter((user) => isActiveInPeriod(user, period));
  const activeInternalUsers = activeUsers.filter((user) => !isResidualInPeriod(user, period));
  const readingUserIds = new Set(data.readings.filter((reading) => reading.period === period).map((reading) => reading.userId));
  const missingReadings = activeInternalUsers.filter((user) => !readingUserIds.has(user.userId));
  const balances = [...ledger.values()].filter((balance) => balance.period === period);
  const debtors = balances.filter((balance) => balance.remainingBalance > 0.005);
  const withPreviousDebt = balances.filter((balance) => balance.previousBalance > 0.005);
  const lastBackupAt = data.settings?.lastBackupAt ? new Date(data.settings.lastBackupAt) : null;
  const daysSinceBackup = lastBackupAt && !Number.isNaN(lastBackupAt.getTime())
    ? Math.floor((Date.now() - lastBackupAt.getTime()) / 86400000)
    : null;

  if (!receipt) {
    alerts.push({
      tone: "danger",
      title: "Falta el recibo del periodo",
      text: "Carga el recibo PDF o registra los importes manualmente para calcular el resumen."
    });
  }
  if (receipt && missingReadings.length) {
    alerts.push({
      tone: "amber",
      title: `${missingReadings.length} lectura pendiente`,
      text: missingReadings.map((user) => user.name || user.userId).join(", ")
    });
  }
  if (withPreviousDebt.length) {
    alerts.push({
      tone: "danger",
      title: "Hay deudas de pagos anteriores",
      text: withPreviousDebt
        .map((balance) => {
          const user = data.users.find((item) => item.userId === balance.userId);
          return `${user?.name || balance.userId}: ${money(balance.previousBalance)}`;
        })
        .join(" · ")
    });
  } else if (debtors.length) {
    alerts.push({
      tone: "amber",
      title: "Pagos del periodo sin cerrar",
      text: `${debtors.length} usuario(s) mantienen saldo por pagar.`
    });
  }
  if (daysSinceBackup == null || daysSinceBackup >= (Number(data.settings?.backupReminderDays) || 30)) {
    alerts.push({
      tone: "blue",
      title: "Conviene guardar un respaldo",
      text: daysSinceBackup == null
        ? "Aún no se registra una copia JSON o CSV reciente."
        : `Han pasado ${daysSinceBackup} días desde el último respaldo.`
    });
  }
  const previousPeriod = availablePeriods(data).filter((item) => item < period)[0];
  if (previousPeriod) {
    const currentKwh = calculatePeriod(period, data).results.reduce((sum, item) => sum + item.consumptionKwh, 0);
    const previousKwh = calculatePeriod(previousPeriod, data).results.reduce((sum, item) => sum + item.consumptionKwh, 0);
    if (previousKwh > 0 && currentKwh > previousKwh * 1.35) {
      alerts.push({
        tone: "amber",
        title: "Consumo inusualmente alto",
        text: `El consumo subió de ${kwh(previousKwh)} a ${kwh(currentKwh)}.`
      });
    }
  }
  return alerts;
}

export function validateReceipt(receipt) {
  const warnings = [];
  const required = [
    ["period", "Periodo"],
    ["externalReadingDate", "Fecha de lectura exterior"],
    ["externalKwh", "Consumo exterior"],
    ["monthlyBill", "Total del mes"],
    ["fixedCharge", "Cargo fijo"],
    ["maintenance", "Mantenimiento y reposición"],
    ["publicLighting", "Alumbrado público"]
  ];
  required.forEach(([key, label]) => {
    const value = receipt?.[key];
    if (value === "" || value == null || (typeof value === "number" && value <= 0)) {
      warnings.push(`${label} pendiente o en cero`);
    }
  });
  if ((Number(receipt?.priceKwhUpTo30) || 0) <= 0 && (Number(receipt?.priceKwhOver30) || 0) <= 0) {
    warnings.push("Precio kWh pendiente");
  }
  if ((Number(receipt?.monthlyBill) || 0) > 0 && (Number(receipt?.externalKwh) || 0) <= 0) {
    warnings.push("Hay total del mes, pero falta el consumo exterior en kWh");
  }
  return warnings;
}

export function money(value) {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN",
    minimumFractionDigits: 2
  }).format(Number(value) || 0);
}

export function kwh(value) {
  return `${(Number(value) || 0).toFixed(2)} kWh`;
}

export function paymentStatusLabel(status) {
  if (status === "PAID") return "Pagado total";
  if (status === "PARTIAL") return "Pago parcial";
  if (status === "UNPAID") return "No pagado";
  return "Sin registrar";
}

export function createInitialData() {
  return {
    users: [],
    receipts: [],
    readings: [],
    services: [],
    payments: [],
    auditEvents: [],
    settings: {
      igvRate: 0.18,
      roundUpToTenth: true,
      supplyAlias: "",
      accountHolder: "",
      monthlyReminderEnabled: false,
      reminderDay: 25,
      lastBackupAt: "",
      backupReminderDays: 30,
      appLockPinHash: "",
      appLockUpdatedAt: "",
      onboardingComplete: false,
      googleSheetId: "",
      googleSheetName: "",
      googleSheetUpdatedAt: "",
      creatorWebsite: CREATOR_WEBSITE
    }
  };
}
