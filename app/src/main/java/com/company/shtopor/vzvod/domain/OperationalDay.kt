package com.company.shtopor.vzvod.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Операционные сутки Core: перелом в 04:00 Europe/Moscow.
 */
object OperationalDay {
    private val zone = ZoneId.of("Europe/Moscow")
    private val cutover = LocalTime.of(4, 0)
    /** Как DepartmentConverter в Core: опорная дата 2026-02-22. */
    private val depEpoch = LocalDate.of(2026, 2, 22)

    fun today(): LocalDate {
        val now = java.time.ZonedDateTime.now(zone)
        return if (now.toLocalTime().isBefore(cutover)) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    /** Отделение «сегодня» для даты операционных суток (1 или 2). */
    fun departmentFromDate(date: LocalDate): Int {
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(depEpoch, date)
        val normalized = ((daysBetween % 4) + 4) % 4
        return when (normalized.toInt()) {
            0, 1 -> 1
            else -> 2
        }
    }

    fun departmentFromDate(isoDate: String): Int =
        runCatching { departmentFromDate(LocalDate.parse(isoDate)) }.getOrDefault(1)
}
