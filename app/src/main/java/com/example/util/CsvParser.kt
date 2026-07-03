package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

object CsvParser {
    data class StudentImport(
        val level: String,
        val group: String,
        val className: String,
        val names: List<String>
    )

    private data class ClassInfo(
        val level: String,
        val group: String,
        val name: String
    )

    fun parseStudentsFromCsv(context: Context, uri: Uri): List<String> {
        return parseStudentImport(context, uri).names
    }

    fun parseStudentImport(
        context: Context,
        uri: Uri,
        fallbackClassName: String? = null,
        fallbackGroup: String? = null
    ): StudentImport {
        val displayName = getDisplayName(context, uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val rows = when {
            isXlsx(displayName, bytes) -> parseXlsxRows(bytes)
            isOldXls(displayName, bytes) -> parseOldXlsRows(bytes)
            else -> parseTextRows(bytes)
        }

        val classInfo = detectClass(displayName, rows)
            ?: detectClassFromText(listOfNotNull(fallbackClassName, fallbackGroup).joinToString(" "))
            ?: fallbackClassInfo(fallbackClassName, fallbackGroup)

        return StudentImport(
            level = classInfo.level,
            group = classInfo.group,
            className = classInfo.name,
            names = extractStudentNames(rows)
        )
    }

    private fun getDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) ?: "" else ""
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun isXlsx(displayName: String, bytes: ByteArray): Boolean {
        return displayName.endsWith(".xlsx", ignoreCase = true) ||
            displayName.endsWith(".xlsm", ignoreCase = true) ||
            (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
    }

    private fun isOldXls(displayName: String, bytes: ByteArray): Boolean {
        return displayName.endsWith(".xls", ignoreCase = true) ||
            (bytes.size > 8 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte())
    }

    private fun parseTextRows(bytes: ByteArray): List<List<String>> {
        val utf8 = bytes.toString(Charsets.UTF_8)
        val text = if (utf8.count { it == '\uFFFD' } > 3) {
            bytes.toString(Charset.forName("windows-1256"))
        } else {
            utf8
        }
        return text.lineSequence()
            .map { splitDelimitedLine(it) }
            .filter { row -> row.any { it.isNotBlank() } }
            .toList()
    }

    private fun parseOldXlsRows(bytes: ByteArray): List<List<String>> {
        val text = bytes.toString(Charset.forName("UTF-16LE")) + "\n" + bytes.toString(Charset.forName("windows-1256"))
        return Regex("[\u0600-\u06FF]+(?:\\s+[\u0600-\u06FF]+){0,5}")
            .findAll(text)
            .map { listOf(cleanName(it.value)) }
            .filter { row -> row.first().isNotBlank() }
            .toList()
    }

    private fun splitDelimitedLine(line: String): List<String> {
        val delimiter = when {
            line.contains(';') -> ';'
            line.contains('\t') -> '\t'
            else -> ','
        }
        return line.split(delimiter).map { it.trim().trim('"') }
    }

    private fun parseXlsxRows(bytes: ByteArray): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        val sheets = mutableListOf<String>()

        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val content = zip.readBytes().toString(Charsets.UTF_8)
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        sharedStrings.clear()
                        sharedStrings.addAll(parseSharedStrings(content))
                    }
                    entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") -> {
                        sheets.add(content)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return sheets.flatMap { parseWorksheetRows(it, sharedStrings) }
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val siRegex = Regex("<si[\\s\\S]*?</si>")
        val tRegex = Regex("<t[^>]*>([\\s\\S]*?)</t>")
        return siRegex.findAll(xml).map { si ->
            tRegex.findAll(si.value)
                .joinToString("") { unescapeXml(it.groupValues[1]) }
                .trim()
        }.toList()
    }

    private fun parseWorksheetRows(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("<row[\\s\\S]*?</row>")
        val cellRegex = Regex("<c([^>]*)>([\\s\\S]*?)</c>")
        val valueRegex = Regex("<v>([\\s\\S]*?)</v>")
        val textRegex = Regex("<t[^>]*>([\\s\\S]*?)</t>")

        rowRegex.findAll(xml).forEach { rowMatch ->
            val row = cellRegex.findAll(rowMatch.value).map { cell ->
                val attrs = cell.groupValues[1]
                val body = cell.groupValues[2]
                val value = valueRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
                val textValue = textRegex.findAll(body).joinToString("") { unescapeXml(it.groupValues[1]) }.trim()
                when {
                    attrs.contains("t=\"s\"") && value != null -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    textValue.isNotBlank() -> textValue
                    value != null -> unescapeXml(value)
                    else -> ""
                }.trim()
            }.toList()
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    private fun extractStudentNames(rows: List<List<String>>): List<String> {
        val names = linkedSetOf<String>()
        val headerIndex = rows.indexOfFirst { row -> row.any { isNameHeader(it) } }

        if (headerIndex >= 0) {
            val header = rows[headerIndex]
            val fullNameIndex = header.indexOfFirst { cell ->
                val h = normalizeArabic(cell)
                h.contains("الاسم واللقب") || h.contains("الاسم الكامل")
            }
            val nameIndex = header.indexOfFirst { cell ->
                val h = normalizeArabic(cell)
                h.contains("الاسم") && !h.contains("ولي") && !h.contains("كود") && !h.contains("رمز")
            }
            val lastNameIndex = header.indexOfFirst { cell -> normalizeArabic(cell).contains("اللقب") }

            rows.drop(headerIndex + 1).forEach { row ->
                val candidate = when {
                    fullNameIndex >= 0 -> row.getOrNull(fullNameIndex).orEmpty()
                    nameIndex >= 0 && lastNameIndex >= 0 && nameIndex != lastNameIndex ->
                        listOf(row.getOrNull(nameIndex).orEmpty(), row.getOrNull(lastNameIndex).orEmpty())
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    nameIndex >= 0 -> row.getOrNull(nameIndex).orEmpty()
                    else -> row.firstOrNull { isLikelyStudentName(it) }.orEmpty()
                }
                val cleaned = cleanName(candidate)
                if (isLikelyStudentName(cleaned)) names.add(cleaned)
            }
        } else {
            rows.forEach { row ->
                val candidate = row
                    .map { cleanName(it) }
                    .filter { isLikelyStudentName(it) }
                    .maxByOrNull { it.count { ch -> ch in '\u0600'..'\u06FF' } }
                if (candidate != null) names.add(candidate)
            }
        }

        return names.toList()
    }

    private fun isNameHeader(value: String): Boolean {
        val h = normalizeArabic(value)
        return h.contains("الاسم") || h.contains("اللقب") || h.contains("name")
    }

    private fun isLikelyStudentName(value: String): Boolean {
        val text = cleanName(value)
        if (text.length < 3) return false
        if (text.contains('\uFFFD')) return false
        val lower = text.lowercase()
        if (lower.contains("workbook") || lower.startsWith("pk") || lower.contains("xml")) return false
        val normalized = normalizeArabic(text)
        val blocked = listOf("الاسم", "اللقب", "الكود", "رمز", "مرتبط", "غير مرتبط", "القسم", "المستوى", "الفوج", "قائمة", "تلميذ", "تلاميذ", "الخامسة", "الرابعة", "الثالثة", "الثانية", "الاولى", "الأولى", "السادسة")
        if (blocked.any { normalized.contains(it) }) return false
        val arabicCount = text.count { it in '\u0600'..'\u06FF' }
        return arabicCount >= 2
    }

    private fun cleanName(value: String): String {
        return value.replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', ':', '-', '_', '.', '،')
    }

    private fun detectClass(displayName: String, rows: List<List<String>>): ClassInfo? {
        val cells = rows.take(15).flatten().joinToString(" ")
        return detectClassFromText(displayName) ?: detectClassFromText(cells)
    }

    private fun detectClassFromText(text: String): ClassInfo? {
        val normalized = normalizeDigits(normalizeArabic(text))
        val groupChars = "أابتثجحخدذرزسشصضطظعغفقكلمنهويABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val wordRegex = Regex("(الأولى|الاولى|الثانية|الثالثة|الرابعة|الخامسة|السادسة)\\s*[-_/\\\\]*\\s*([$groupChars])")
        wordRegex.find(normalized)?.let { match ->
            val level = standardLevel(match.groupValues[1])
            val group = standardGroup(match.groupValues[2])
            return ClassInfo(level, group, "$level $group")
        }

        val digitRegex = Regex("(?:^|[^0-9])([1-6])\\s*[-_/\\\\]*\\s*([$groupChars])")
        digitRegex.find(normalized)?.let { match ->
            val level = digitToLevel(match.groupValues[1])
            val group = standardGroup(match.groupValues[2])
            return ClassInfo(level, group, "$level $group")
        }
        return null
    }

    private fun fallbackClassInfo(fallbackClassName: String?, fallbackGroup: String?): ClassInfo {
        val group = fallbackGroup?.trim().orEmpty()
        val level = fallbackClassName
            ?.replace(group, "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "قسم مستورد"
        val name = listOf(level, group).filter { it.isNotBlank() }.joinToString(" ")
        return ClassInfo(level, group, name)
    }

    private fun standardLevel(level: String): String {
        return when (normalizeArabic(level)) {
            "الاولى" -> "الأولى"
            else -> level.trim()
        }
    }

    private fun digitToLevel(value: String): String {
        return when (value) {
            "1" -> "الأولى"
            "2" -> "الثانية"
            "3" -> "الثالثة"
            "4" -> "الرابعة"
            "5" -> "الخامسة"
            "6" -> "السادسة"
            else -> "قسم مستورد"
        }
    }

    private fun standardGroup(group: String): String {
        return when (group.trim().uppercase()) {
            "A" -> "أ"
            "B" -> "ب"
            "C" -> "ج"
            "D" -> "د"
            "E" -> "هـ"
            else -> group.trim().take(1)
        }
    }

    private fun normalizeArabic(text: String): String {
        return text.replace('إ', 'ا')
            .replace('أ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .trim()
    }

    private fun normalizeDigits(text: String): String {
        val eastern = "٠١٢٣٤٥٦٧٨٩"
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        return buildString {
            text.forEach { ch ->
                val e = eastern.indexOf(ch)
                val p = persian.indexOf(ch)
                append(
                    when {
                        e >= 0 -> ('0'.code + e).toChar()
                        p >= 0 -> ('0'.code + p).toChar()
                        else -> ch
                    }
                )
            }
        }
    }

    private fun unescapeXml(value: String): String {
        return value.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
