package com.company.shtopor.vzvod.domain

enum class EventDepartmentFilter {
    ALL,
    DEP_1,
    DEP_2
}

object EventListFilter {
    fun <T> filter(
        events: List<T>,
        filter: EventDepartmentFilter,
        shiftOfDepartment: (T) -> Int?
    ): List<T> = when (filter) {
        EventDepartmentFilter.ALL -> events
        EventDepartmentFilter.DEP_1 -> events.filter { shiftOfDepartment(it) == 1 }
        EventDepartmentFilter.DEP_2 -> events.filter { shiftOfDepartment(it) == 2 }
    }

    fun shiftCaption(shiftOfDepartment: Int?): String {
        val n = shiftOfDepartment?.toString() ?: "—"
        return "смена $n отделения"
    }
}
