package com.company.shtopor.vzvod.domain

/**
 * Счётчики смены:
 * - ИБДР ± → только ibdr
 * - Мигрант ± → только migrant
 * - Мигрант + ИБДР ± → оба поля
 * - Заявки ± → countOfStatements
 * - Претензии ± → countOfClaims
 * - АП / УП → число сущностей (+1 при создании)
 */
data class IbdCounters(
    val ibdr: Int,
    val migrant: Int
)

object ShiftMetricsLogic {
    fun applyDelta(current: Int?, delta: Int): Int {
        require(delta == 1 || delta == -1) { "delta must be ±1" }
        return maxOf(0, (current ?: 0) + delta)
    }

    fun applyIbdrDelta(ibdr: Int?, migrant: Int?, delta: Int): IbdCounters =
        IbdCounters(ibdr = applyDelta(ibdr, delta), migrant = migrant ?: 0)

    fun applyMigrantDelta(ibdr: Int?, migrant: Int?, delta: Int): IbdCounters =
        IbdCounters(ibdr = ibdr ?: 0, migrant = applyDelta(migrant, delta))

    fun applyMigrantAndIbdrDelta(ibdr: Int?, migrant: Int?, delta: Int): IbdCounters =
        IbdCounters(
            ibdr = applyDelta(ibdr, delta),
            migrant = applyDelta(migrant, delta)
        )

    fun formatSummary(
        ibdr: Int?,
        migrant: Int?,
        statements: Int?,
        claims: Int?,
        adminCount: Int?,
        criminalCount: Int?
    ): String = buildString {
        append("ИБДР ").append(ibdr ?: 0)
        append(" · мигрант ").append(migrant ?: 0)
        append(" · заявки ").append(statements ?: 0)
        append(" · претензии ").append(claims ?: 0)
        append(" · АП ").append(adminCount ?: 0)
        append(" · УП ").append(criminalCount ?: 0)
    }
}

/**
 * Активная смена и правила, защищающие от ANR:
 * уведомление/reconcile читают только кэш, не вызывают refreshShifts повторно.
 */
object ActiveShiftPolicy {
    fun isOpen(endTime: String?): Boolean = endTime.isNullOrBlank()

    fun openFromServer(items: List<Pair<String, String?>>): String? =
        items.firstOrNull { isOpen(it.second) }?.first

    fun mergeCreatedIfMissing(serverItems: List<String>, createdId: String?): Boolean =
        createdId != null && createdId !in serverItems

    fun notificationUsesCacheOnly(): Boolean = true

    fun notificationMayCallRefreshShifts(): Boolean = false

    fun resolveOpenShiftId(serverOpenIds: List<String>, cachedOpenId: String?): String? =
        serverOpenIds.firstOrNull() ?: null
}

object ShiftRoutes {
    fun fallbackDefaultType(routeId: String): String = when (routeId) {
        "МП 28", "МП 30", "МП 31", "МП 32" -> "VZVOD_ROUTE"
        "МП 5", "МП 6", "СП 3", "СП 18" -> "BAT_POST"
        else -> "CHECKING"
    }

    fun fallbackTypeLabel(typeId: String?): String = when (typeId) {
        "VZVOD_ROUTE" -> "Маршрут взвода"
        "BAT_POST" -> "Пост батальона"
        "Strengthening", "STRENGTHENING" -> "Усиление"
        "CHECKING" -> "Проверка"
        "ON_BASES" -> "На базах"
        else -> typeId ?: "—"
    }
}
