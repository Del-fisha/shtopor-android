package com.company.shtopor.vzvod.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarDayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val department: Int
)

object CalendarMonthLogic {
    fun department(date: LocalDate): Int = OperationalDay.departmentFromDate(date)

    fun cells(month: YearMonth): List<CalendarDayCell> {
        val first = month.atDay(1)
        val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val last = month.atEndOfMonth()
        val end = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val result = ArrayList<CalendarDayCell>()
        var d = start
        while (!d.isAfter(end)) {
            result.add(
                CalendarDayCell(
                    date = d,
                    inMonth = d.year == month.year && d.month == month.month,
                    department = department(d)
                )
            )
            d = d.plusDays(1)
        }
        return result
    }

    fun canShowShiftSummary(date: LocalDate, today: LocalDate): Boolean =
        !date.isAfter(today)

    fun eventDatesOn(isoDates: List<String?>): Set<LocalDate> =
        isoDates.mapNotNull { raw ->
            raw?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }.toSet()
}
