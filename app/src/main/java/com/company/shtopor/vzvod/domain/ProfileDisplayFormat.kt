package com.company.shtopor.vzvod.domain

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ProfileDisplayFormat {
    private val DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun idCardDate(raw: String?): String = shortDate(raw)

    fun shortDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return runCatching {
            LocalDate.parse(raw.trim().take(10)).format(DMY)
        }.getOrDefault(raw.trim())
    }

    fun idCardRange(issued: String?, until: String?): String =
        "${idCardDate(issued)} — ${idCardDate(until)}"

    fun birthLine(raw: String?, today: LocalDate = LocalDate.now()): String {
        val date = shortDate(raw)
        val years = ageYears(raw, today) ?: return date
        return "$date (${years} ${yearWord(years)})"
    }

    fun ageYears(raw: String?, today: LocalDate = LocalDate.now()): Int? {
        val date = parseDate(raw) ?: return null
        if (date.isAfter(today)) return 0
        return Period.between(date, today).years
    }

    /**
     * Календарные дни отпуска, обе границы включительно — так же считает core
     * ({@code ChronoUnit.DAYS.between + 1}). Если сервер уже прислал {@code countOfDays}, берём его.
     */
    fun inclusiveVacationDays(countOfDays: Int?, start: String?, end: String?): Int? {
        if (countOfDays != null) return countOfDays
        val from = parseDate(start) ?: return null
        val to = parseDate(end) ?: return null
        val days = ChronoUnit.DAYS.between(from, to) + 1
        return if (days < 0) 0 else days.toInt()
    }

    fun phone(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.contains('*')) return null
        val digits = raw.filter { it.isDigit() }
        if (digits.length == 11 && (digits[0] == '7' || digits[0] == '8')) {
            val n = digits.substring(1)
            return "+7 (${n.substring(0, 3)}) ${n.substring(3, 6)}-${n.substring(6, 8)}-${n.substring(8)}"
        }
        return raw.trim()
    }

    fun addressLine(city: String?, street: String?, body: String?, house: String?, flat: String?): String {
        val parts = listOfNotNull(
            city?.trim()?.takeIf { it.isNotEmpty() },
            street?.trim()?.takeIf { it.isNotEmpty() },
            body?.trim()?.takeIf { it.isNotEmpty() }?.let { "к. $it" },
            house?.trim()?.takeIf { it.isNotEmpty() }?.let { "д. $it" },
            flat?.trim()?.takeIf { it.isNotEmpty() }?.let { "кв. $it" }
        )
        return parts.joinToString(", ").ifBlank { "—" }
    }

    fun departmentNumbered(department: String?, number: Int?): String {
        val n = number?.toString()
            ?: department?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1) }
        return if (n.isNullOrBlank()) department?.trim().orEmpty().ifBlank { "—" } else "Отделение №$n"
    }

    fun departmentPlain(number: Int?): String =
        if (number == null) "Отделение —" else "Отделение $number"

    fun educationSpan(started: String?, until: String?): String? {
        val hasStart = !started.isNullOrBlank()
        val hasEnd = !until.isNullOrBlank()
        if (!hasStart && !hasEnd) return null
        val from = if (hasStart) shortDate(started) else "—"
        val to = if (hasEnd) shortDate(until) else "по н.в."
        return "$from - $to"
    }

    /** «Иванов И. И.» → «Иванов И.О.» */
    fun compactFio(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return raw.trim()
        val initials = parts.drop(1).mapNotNull { token ->
            token.trim('.').firstOrNull()?.toString()
        }
        if (initials.isEmpty()) return parts.first()
        return parts.first() + " " + initials.joinToString(".") + "."
    }

    fun metroLabel(
        nearestMetroLabel: String?,
        nearestMetroId: Int?,
        catalogLabel: String?
    ): String {
        nearestMetroLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        if (nearestMetroId != null) {
            catalogLabel?.trim()?.takeIf { it.isNotEmpty() && it != "—" }?.let { return it }
        }
        return "—"
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()
    }

    private fun yearWord(value: Int): String = countWord(value, "год", "года", "лет")

    private fun countWord(value: Int, one: String, few: String, many: String): String {
        val abs = kotlin.math.abs(value) % 100
        val last = abs % 10
        if (abs in 11..14) return many
        return when (last) {
            1 -> one
            in 2..4 -> few
            else -> many
        }
    }
}
