package com.company.shtopor.vzvod.domain

/**
 * Счётчики проверок маршрутов на вкладке Смена (CHECKING): + добавляет, − снимает последнюю.
 */
object CheckingRouteCounters {
    /** У командира в проверках только маршруты СП и МП. «Другое» и прочие номера скрыты. */
    fun commanderSeesRoute(routeLabel: String?): Boolean {
        val label = routeLabel?.trim().orEmpty()
        if (label.isEmpty()) return false
        return label.startsWith("МП") || label.startsWith("СП")
            || label.startsWith("мп") || label.startsWith("сп")
    }

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
