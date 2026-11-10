package com.company.shtopor.vzvod.domain

/**
 * Подпись кнопки маршрута в шторке CHECKING: «МП 28 | 18:12 | 18:14».
 */
object CheckingRouteButtonLabel {
    fun format(routeLabel: String, checkedAtValues: List<String?>): String {
        val name = routeLabel.trim().ifBlank { "маршрут" }
        val times = checkedAtValues.mapNotNull { toHm(it) }
        if (times.isEmpty()) return name
        return times.joinToString(separator = " | ", prefix = "$name | ")
    }

    fun toHm(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val match = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?").find(trimmed) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return "%02d:%02d".format(h, m)
    }
}
