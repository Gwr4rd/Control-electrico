package com.gerar.controlelectrico.domain

import java.text.Normalizer
import java.util.Locale

data class ReceiptPdfData(
    val period: String? = null,
    val externalReadingDate: String? = null,
    val supplyNumber: String? = null,
    val externalKwh: Double? = null,
    val monthlyBill: Double? = null,
    val priceKwhUpTo30: Double? = null,
    val priceKwhOver30: Double? = null,
    val priceKwhRateCount: Int = 0,
    val fixedCharge: Double? = null,
    val maintenance: Double? = null,
    val maintenanceOnly: Double? = null,
    val replacementOnly: Double? = null,
    val publicLighting: Double? = null,
    val ruralElectrification: Double? = null
)

object ReceiptPdfParser {
    fun parse(rawText: String): ReceiptPdfData {
        val text = rawText.normalizeOcr()
        val rates = Regex("""\b0[.,]\d{4}\b""")
            .findAll(text)
            .mapNotNull { it.value.toDoubleValueOrNull() }
            .toList()

        return ReceiptPdfData(
            period = parsePeriod(text),
            externalReadingDate = parseFirstReceiptDate(text),
            supplyNumber = parseSupplyNumber(text),
            externalKwh = parseExternalKwh(text),
            monthlyBill = parseAmountFromLine(text, """TOTAL\s+DEL\s+MES"""),
            priceKwhUpTo30 = rates.firstOrNull(),
            priceKwhOver30 = rates.getOrNull(1) ?: rates.firstOrNull(),
            priceKwhRateCount = rates.size.coerceAtMost(2),
            fixedCharge = parseAmountFromLine(text, """CARGO\s+FIJO"""),
            maintenance = parseAmountFromLine(text, """MANT\.?\s+Y\s+REPOSICI[O0]N\s+DE\s+CONEXI[O0]N""")
                ?: separateMaintenanceTotal(text),
            maintenanceOnly = parseAmountFromLine(text, """\bMANT(?:\.|ENIMIENTO)?\b(?!.*REPOSICI[O0]N)"""),
            replacementOnly = parseAmountFromLine(text, """REPOSICI[O0]N(?:\s+DE\s+CONEXI[O0]N)?"""),
            publicLighting = parseAmountFromLine(text, """ALUMBRADO\s+PUBLICO"""),
            ruralElectrification = parseAmountFromLine(text, """ELECTRIFICACI[O0]N\s+RURAL.*28749""")
                ?: parseAmountFromLine(text, """APORTE\s+LEY\s+N\s*28749""")
                ?: parseAmountFromLine(text, """LEY\s+N\s*28749""")
        )
    }

    private fun separateMaintenanceTotal(text: String): Double? {
        val maintenanceOnly = parseAmountFromLine(text, """\bMANT(?:\.|ENIMIENTO)?\b(?!.*REPOSICI[O0]N)""")
        val replacementOnly = parseAmountFromLine(text, """REPOSICI[O0]N(?:\s+DE\s+CONEXI[O0]N)?""")
        val total = listOfNotNull(maintenanceOnly, replacementOnly).sum()
        return total.takeIf { it > 0.0 }
    }

    private fun parsePeriod(text: String): String? {
        val match = Regex("""MES\s+FACTURADO\s+([A-Z]+)\s+(\d{4})""").find(text) ?: return null
        val month = monthNumber(match.groupValues[1]) ?: return null
        return "${match.groupValues[2]}-${month.toString().padStart(2, '0')}"
    }

    private fun parseFirstReceiptDate(text: String): String? {
        val date = Regex("""\b(\d{1,2})\s*/\s*([A-Z]+)\s*/\s*(\d{2,4})\b""").find(text) ?: return null
        val day = date.groupValues[1].toIntOrNull() ?: return null
        val month = monthNumber(date.groupValues[2]) ?: return null
        val rawYear = date.groupValues[3].toIntOrNull() ?: return null
        val year = if (rawYear < 100) 2000 + rawYear else rawYear
        return "%04d-%02d-%02d".format(Locale.US, year, month, day)
    }

    private fun parseSupplyNumber(text: String): String? {
        return Regex("""SUMINISTRO\s+([0-9]{5,})""")
            .find(text)
            ?.groupValues
            ?.get(1)
    }

    private fun parseExternalKwh(text: String): Double? {
        val formulaMatch = Regex("""=\s*([0-9]+(?:[.,][0-9]+)?)\s*[Xx]\s*1(?:[.,]0+)?\s*=""")
            .find(text)
        if (formulaMatch != null) return formulaMatch.groupValues[1].toDoubleValueOrNull()

        return Regex("""([0-9]+(?:[.,][0-9]+)?)\s*[Xx]\s*1(?:[.,]0+)?""")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.toDoubleValueOrNull()
    }

    private fun parseAmountFromLine(text: String, labelRegex: String): Double? {
        val label = Regex(labelRegex)
        val amount = Regex("""\b[0-9]+[.,][0-9]{1,4}\b""")
        val lines = text.lines()
        val index = lines.indexOfFirst { label.containsMatchIn(it) }
        if (index < 0) return null

        val line = lines[index]
        return amount.findAll(line)
            .mapNotNull { it.value.toDoubleValueOrNull() }
            .lastOrNull()
    }

    private fun monthNumber(raw: String): Int? {
        return when (raw.take(3).uppercase(Locale.US)) {
            "ENE" -> 1
            "FEB" -> 2
            "MAR" -> 3
            "ABR" -> 4
            "MAY" -> 5
            "JUN" -> 6
            "JUL" -> 7
            "AGO" -> 8
            "SEP" -> 9
            "OCT" -> 10
            "NOV" -> 11
            "DIC" -> 12
            else -> null
        }
    }

    private fun String.normalizeOcr(): String {
        val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
        return withoutAccents
            .uppercase(Locale.US)
            .replace("°", "")
            .replace("º", "")
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\s*\n\s*"""), "\n")
    }

    private fun String.toDoubleValueOrNull(): Double? {
        return replace(",", ".").toDoubleOrNull()
    }
}
