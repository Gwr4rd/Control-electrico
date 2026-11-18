package com.gerar.controlelectrico.domain

import org.junit.Assert.*
import org.junit.Test

class ReadingEntryTest {
    private fun entry(day: String, value: Double, user: String = "A") =
        MeterReading("$user-$day", day.take(7), user, false, day, null, value)

    @Test fun onlyUsesEarlierDatesForTheSameUser() {
        val rows = listOf(entry("2026-01-10", 2.0), entry("2026-02-10", 7.0),
            entry("2026-02-09", 80.0, "B"), entry("bad", 88.0))
        assertEquals(2.0, ReadingEntry.previous(rows, "A", "2026-02-10", "")!!, 0.0)
        assertNull(ReadingEntry.previous(rows, "A", "2025-12-10", ""))
        assertNull(ReadingEntry.previous(rows, "A", "2026-02-10", rows.first().id))
    }

    @Test fun requiresValidReadingsDateAndPeriodButAcceptsZeroConsumption() {
        assertTrue(ReadingEntry.warnings("2026-01", "2026-01-10", 0.0, 0.0).isEmpty())
        assertEquals(4, ReadingEntry.warnings("2026-13", "2026-02-30", null, Double.NaN).size)
        assertTrue(ReadingEntry.warnings("2026-01", "2026-01-10", 2.0, 1.0).isNotEmpty())
    }
}
