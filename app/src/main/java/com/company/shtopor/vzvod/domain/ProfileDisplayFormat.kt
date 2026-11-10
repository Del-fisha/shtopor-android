package com.company.shtopor.vzvod.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ProfileDisplayFormat {
    /** ДД.ММММ.ГГГГ — день, полное название месяца, год. */
    private val RU = DateTimeFormatter.ofPattern("dd.MMMM.yyyy", Locale.forLanguageTag("ru"))

    fun idCardDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return runCatching {
            LocalDate.parse(raw.trim().take(10)).format(RU)
        }.getOrDefault(raw.trim())
    }

    fun idCardRange(issued: String?, until: String?): String =
        "${idCardDate(issued)} — ${idCardDate(until)}"

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
}
