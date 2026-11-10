package com.company.shtopor.vzvod.domain

/**
 * Тип смены «Проверки маршрутов» — счётчики ИБДР/мигрант не используются.
 */
object CheckingShiftPolicy {
    const val TYPE_ID = "CHECKING"

    fun isChecking(typeOfShiftId: String?): Boolean =
        TYPE_ID.equals(typeOfShiftId?.trim(), ignoreCase = false)

    fun partnersRequired(typeOfShiftId: String?): Boolean = !isChecking(typeOfShiftId)

    fun showRouteCheckCounters(typeOfShiftId: String?): Boolean = isChecking(typeOfShiftId)

    fun showMetricCounters(typeOfShiftId: String?): Boolean = !isChecking(typeOfShiftId)
}
