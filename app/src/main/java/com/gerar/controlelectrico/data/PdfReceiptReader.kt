package com.gerar.controlelectrico.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.gerar.controlelectrico.domain.ReceiptPdfData
import com.gerar.controlelectrico.domain.ReceiptPdfParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object PdfReceiptReader {
    fun readReceipt(
        context: Context,
        uri: Uri,
        onSuccess: (ReceiptPdfData) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val bitmap = renderFirstPage(context, uri)
            val image = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { recognized ->
                    val parsed = ReceiptPdfParser.parse(buildSearchableText(recognized))
                    val anchored = extractConceptAmounts(recognized)
                    onSuccess(
                        parsed.copy(
                            externalReadingDate = anchored.externalReadingDate ?: parsed.externalReadingDate,
                            monthlyBill = anchored.monthlyBill ?: parsed.monthlyBill,
                            fixedCharge = anchored.fixedCharge ?: parsed.fixedCharge,
                            maintenance = anchored.maintenance ?: parsed.maintenance,
                            maintenanceOnly = anchored.maintenanceOnly ?: parsed.maintenanceOnly,
                            replacementOnly = anchored.replacementOnly ?: parsed.replacementOnly,
                            publicLighting = anchored.publicLighting ?: parsed.publicLighting,
                            ruralElectrification = anchored.ruralElectrification ?: parsed.ruralElectrification
                        )
                    )
                }
                .addOnFailureListener { error ->
                    onError(error.message ?: "No se pudo leer el texto del PDF")
                }
        } catch (error: Exception) {
            onError(error.message ?: "No se pudo abrir el PDF")
        }
    }

    private fun renderFirstPage(context: Context, uri: Uri): Bitmap {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("No se pudo abrir el archivo seleccionado")

        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) error("El PDF no tiene paginas")
                renderer.openPage(0).use { page ->
                    val scale = 3
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    Canvas(bitmap).drawColor(Color.WHITE)
                    val matrix = Matrix().apply { postScale(scale.toFloat(), scale.toFloat()) }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    private fun buildSearchableText(recognized: Text): String {
        val lines = recognized.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                PositionedLine(text = line.text, box = box)
            }
            .sortedWith(compareBy<PositionedLine> { it.centerY }.thenBy { it.box.left })

        if (lines.isEmpty()) return recognized.text

        val rows = mutableListOf<MutableList<PositionedLine>>()
        lines.forEach { line ->
            val row = rows.lastOrNull()
            val threshold = max(18, line.box.height() / 2)
            if (row != null && abs(row.averageCenterY() - line.centerY) <= threshold) {
                row.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        val rowText = rows.joinToString("\n") { row ->
            row.sortedBy { it.box.left }
                .joinToString(" ") { it.text }
        }
        return rowText + "\n" + recognized.text
    }

    private fun extractConceptAmounts(recognized: Text): ReceiptPdfData {
        val lines = recognized.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                PositionedLine(
                    text = line.text,
                    box = box,
                    elements = line.elements.mapNotNull { element ->
                        val elementBox = element.boundingBox ?: return@mapNotNull null
                        PositionedElement(element.text, elementBox)
                    }
                )
        }

        return ReceiptPdfData(
            externalReadingDate = findDateBelowLastReadingHeader(lines),
            monthlyBill = findAmountToRight(lines, Regex("""\bTOTAL\s+DEL\s+MES\b""")),
            fixedCharge = findAmountToRight(lines, Regex("""\bCARGO\s+FIJO\b""")),
            maintenance = findAmountToRight(
                lines,
                Regex("""\bMANT\.?\s+Y\s+REPOSICION\s+DE\s+CONEXION\b""")
            ) ?: separateMaintenanceTotal(lines),
            maintenanceOnly = findAmountToRight(
                lines,
                Regex("""\bMANT(?:\.|ENIMIENTO)?\b(?!.*REPOSICION)""")
            ),
            replacementOnly = findAmountToRight(
                lines,
                Regex("""\bREPOSICION(?:\s+DE\s+CONEXION)?\b""")
            ),
            publicLighting = findAmountToRight(lines, Regex("""\bALUMBRADO\s+PUBLICO\b""")),
            ruralElectrification = findAmountToRight(
                lines,
                Regex("""\bELECTRIFICACION\s+RURAL\b.*\b28749\b""")
            ) ?: findAmountToRight(lines, Regex("""\bAPORTE\s+LEY\s+N\s*28749\b"""))
                ?: findAmountToRight(lines, Regex("""\bLEY\s+N\s*28749\b"""))
                ?: findAmountToRight(lines, Regex("""\bELECTRIFICACION\s+RURAL\b"""))
        )
    }

    private fun separateMaintenanceTotal(lines: List<PositionedLine>): Double? {
        val maintenanceOnly = findAmountToRight(lines, Regex("""\bMANT(?:\.|ENIMIENTO)?\b(?!.*REPOSICION)"""))
        val replacementOnly = findAmountToRight(lines, Regex("""\bREPOSICION(?:\s+DE\s+CONEXION)?\b"""))
        val total = listOfNotNull(maintenanceOnly, replacementOnly).sum()
        return total.takeIf { it > 0.0 }
    }

    private fun findDateBelowLastReadingHeader(lines: List<PositionedLine>): String? {
        val headerBox = findLastReadingHeaderBox(lines) ?: return null
        val headerCenterX = headerBox.left + headerBox.width() / 2
        val yLimit = headerBox.bottom + max(260, headerBox.height() * 8)
        val xTolerance = max(140, headerBox.width() * 2)

        return lines
            .flatMap { it.elements }
            .mapNotNull { element ->
                val rawDate = receiptDateRegex.find(element.text)?.value ?: return@mapNotNull null
                DateCandidate(rawDate, element.box)
            }
            .filter { it.box.top >= headerBox.bottom - 6 }
            .filter { it.centerY <= yLimit }
            .filter { abs(it.centerX - headerCenterX) <= xTolerance }
            .minWithOrNull(
                compareBy<DateCandidate> { it.box.top }
                    .thenBy { abs(it.centerX - headerCenterX) }
            )
            ?.text
            ?.toIsoReceiptDate()
    }

    private fun findLastReadingHeaderBox(lines: List<PositionedLine>): Rect? {
        lines
            .filter { it.normalizedText.contains("ULTIMA") && it.normalizedText.contains("LECTURA") }
            .minByOrNull { it.box.left }
            ?.let { return it.box }

        val elements = lines.flatMap { it.elements }
        return elements
            .filter { it.normalizedText.contains("ULTIMA") }
            .sortedBy { it.box.left }
            .firstNotNullOfOrNull { ultima ->
                elements
                    .filter { it.normalizedText.contains("LECTURA") }
                    .filter { abs(it.centerX - ultima.centerX) <= max(80, ultima.box.width() * 3) }
                    .filter { it.centerY >= ultima.centerY }
                    .filter { it.centerY - ultima.centerY <= max(120, ultima.box.height() * 5) }
                    .minByOrNull { it.centerY }
                    ?.let { lectura -> ultima.box.unionWith(lectura.box) }
            }
    }

    private fun findAmountToRight(lines: List<PositionedLine>, label: Regex): Double? {
        val labelLine = lines
            .filter { label.containsMatchIn(it.normalizedText) }
            .minByOrNull { it.box.left }
            ?: return null

        amountRegex.findAll(labelLine.text)
            .mapNotNull { it.value.toAmountOrNull() }
            .lastOrNull()
            ?.let { return it }

        val yTolerance = max(16, labelLine.box.height())
        val minimumLeft = labelLine.box.left + (labelLine.box.width() * 0.55).toInt()
        return lines
            .flatMap { it.elements }
            .filter { amountRegex.matches(it.text.trim()) }
            .filter { abs(it.centerY - labelLine.centerY) <= yTolerance }
            .filter { it.box.left > minimumLeft }
            .minByOrNull { it.box.left }
            ?.text
            ?.toAmountOrNull()
    }

    private data class PositionedLine(
        val text: String,
        val box: Rect,
        val elements: List<PositionedElement> = emptyList()
    ) {
        val centerY: Int = box.top + box.height() / 2
        val normalizedText: String = text.normalizeForSearch()
    }

    private data class PositionedElement(
        val text: String,
        val box: Rect
    ) {
        val centerY: Int = box.top + box.height() / 2
        val centerX: Int = box.left + box.width() / 2
        val normalizedText: String = text.normalizeForSearch()
    }

    private data class DateCandidate(
        val text: String,
        val box: Rect
    ) {
        val centerY: Int = box.top + box.height() / 2
        val centerX: Int = box.left + box.width() / 2
    }

    private fun List<PositionedLine>.averageCenterY(): Int {
        return sumOf { it.centerY } / size
    }

    private val amountRegex = Regex("""[0-9]+[.,][0-9]{1,4}""")
    private val receiptDateRegex = Regex("""\b\d{1,2}\s*/\s*[A-Za-z]{3}\s*/\s*\d{2,4}\b""")

    private fun String.toAmountOrNull(): Double? {
        return trim().replace(",", ".").toDoubleOrNull()
    }

    private fun String.normalizeForSearch(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .uppercase(Locale.US)
            .replace("°", "")
            .replace("º", "")
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
    }

    private fun String.toIsoReceiptDate(): String? {
        val match = Regex("""(\d{1,2})\s*/\s*([A-Za-z]{3})\s*/\s*(\d{2,4})""")
            .find(this)
            ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = when (match.groupValues[2].take(3).uppercase(Locale.US)) {
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
            else -> return null
        }
        val rawYear = match.groupValues[3].toIntOrNull() ?: return null
        val year = if (rawYear < 100) 2000 + rawYear else rawYear
        return "%04d-%02d-%02d".format(Locale.US, year, month, day)
    }

    private fun Rect.unionWith(other: Rect): Rect {
        return Rect(
            minOf(left, other.left),
            minOf(top, other.top),
            maxOf(right, other.right),
            maxOf(bottom, other.bottom)
        )
    }
}
