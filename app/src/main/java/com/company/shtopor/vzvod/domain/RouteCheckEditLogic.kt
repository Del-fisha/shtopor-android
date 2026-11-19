package com.company.shtopor.vzvod.domain

object RouteCheckEditLogic {
    fun normalizeTime(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        val match = Regex("""^([01]?\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$""").matchEntire(text) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return "%02d:%02d".format(hour, minute)
    }

    fun displayTime(raw: String?): String {
        val normalized = normalizeTime(raw?.take(5)) ?: raw?.trim()?.take(5)
        return normalized?.takeIf { it.isNotBlank() } ?: "—"
    }
}
