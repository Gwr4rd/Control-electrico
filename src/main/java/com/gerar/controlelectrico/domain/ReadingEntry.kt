package com.gerar.controlelectrico.domain

import java.time.LocalDate
import java.time.YearMonth

object ReadingEntry {
    fun previous(readings: List<MeterReading>, user: String, date: String, editingId: String): Double? {
        val cutoff = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return readings.asSequence()
            .filter { it.id != editingId && it.userId == user && !it.isResidual && it.currentReading?.isFinite() == true }
            .mapNotNull { reading ->
                val day = runCatching { LocalDate.parse(reading.internalReadingDate) }.getOrNull()
                day?.takeIf { it < cutoff }?.let { it to reading.currentReading }
            }
            .maxByOrNull { it.first }?.second
    }

    fun warnings(period: String, date: String, previous: Double?, current: Double?): List<String> = buildList {
        if (runCatching { YearMonth.parse(period) }.isFailure) add("Escribe el periodo como AAAA-MM.")
        if (runCatching { LocalDate.parse(date) }.isFailure) add("Selecciona una fecha válida.")
        if (previous == null || !previous.isFinite() || previous < 0) add("Ingresa una lectura anterior válida.")
        if (current == null || !current.isFinite() || current < 0) add("Ingresa una lectura actual válida.")
        if (previous != null && current != null && current < previous) add("La lectura actual no puede ser menor que la anterior.")
    }
}
