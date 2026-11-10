package com.company.shtopor.vzvod.domain

enum class TodayDashboardSection {
    TOTALS,
    ROUTES
}

enum class TodayDashboardTapTarget {
    ROUTE_LABEL,
    IBDR,
    MIGRANT,
    AP,
    UP,
    STATEMENTS
}

object TodayDashboardUiPolicy {
    fun sectionOrder(): List<TodayDashboardSection> =
        listOf(TodayDashboardSection.TOTALS, TodayDashboardSection.ROUTES)

    fun totalsTitle(): String = "Общее"

    fun metricRowOrder(): List<TodayDashboardTapTarget> = listOf(
        TodayDashboardTapTarget.IBDR,
        TodayDashboardTapTarget.MIGRANT,
        TodayDashboardTapTarget.AP,
        TodayDashboardTapTarget.UP,
        TodayDashboardTapTarget.STATEMENTS
    )

    fun label(target: TodayDashboardTapTarget): String = when (target) {
        TodayDashboardTapTarget.ROUTE_LABEL -> "Маршрут"
        TodayDashboardTapTarget.IBDR -> "ИБДР"
        TodayDashboardTapTarget.MIGRANT -> "МИГРАНТ"
        TodayDashboardTapTarget.AP -> "АП"
        TodayDashboardTapTarget.UP -> "УП"
        TodayDashboardTapTarget.STATEMENTS -> "ЗАЯВКИ"
    }

    /** Маршрут: тап только у номера маршрута, АП, УП. */
    fun isExpandable(target: TodayDashboardTapTarget): Boolean = when (target) {
        TodayDashboardTapTarget.ROUTE_LABEL,
        TodayDashboardTapTarget.AP,
        TodayDashboardTapTarget.UP -> true
        else -> false
    }

    /** Блок «Общее»: тап только у АП, УП. */
    fun isTotalsExpandable(target: TodayDashboardTapTarget): Boolean = when (target) {
        TodayDashboardTapTarget.AP,
        TodayDashboardTapTarget.UP -> true
        else -> false
    }

    fun sectionCollapsedByDefault(): Boolean = true
}
