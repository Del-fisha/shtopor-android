package com.company.shtopor.vzvod.domain

/**
 * Счётчики проверок маршрутов на вкладке Смена (CHECKING): + добавляет, − снимает последнюю.
 */
object CheckingRouteCounters {
    fun lastCheckId(checkIds: List<String?>): String? =
        checkIds.lastOrNull { !it.isNullOrBlank() }?.trim()

    fun formatSummary(
        routeCount: Int,
        checkCount: Int,
        adminCount: Int?,
        criminalCount: Int?
    ): String =
        "Проверок $checkCount · маршрутов $routeCount · АП ${adminCount ?: 0} · УП ${criminalCount ?: 0}"
}
