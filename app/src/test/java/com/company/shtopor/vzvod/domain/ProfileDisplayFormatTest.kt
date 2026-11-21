package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProfileDisplayFormatTest {

    @Test
    fun idCardDate_formatsIsoToDdMmYyyy() {
        assertEquals("15.03.2024", ProfileDisplayFormat.idCardDate("2024-03-15"))
        assertEquals("09.08.2026", ProfileDisplayFormat.idCardDate("2026-08-09T00:00:00"))
    }

    @Test
    fun idCardDate_blankStaysDash() {
        assertEquals("—", ProfileDisplayFormat.idCardDate(null))
        assertEquals("—", ProfileDisplayFormat.idCardDate("  "))
    }

    @Test
    fun idCardRange_joinsWithDash() {
        assertEquals(
            "01.01.2020 — 31.12.2030",
            ProfileDisplayFormat.idCardRange("2020-01-01", "2030-12-31")
        )
    }

    @Test
    fun metroLabel_prefersServerLabelThenCatalog() {
        assertEquals(
            "Балтийская",
            ProfileDisplayFormat.metroLabel(
                nearestMetroLabel = "Балтийская",
                nearestMetroId = 114,
                catalogLabel = "Балтийская (каталог)"
            )
        )
        assertEquals(
            "Балтийская (каталог)",
            ProfileDisplayFormat.metroLabel(
                nearestMetroLabel = null,
                nearestMetroId = 114,
                catalogLabel = "Балтийская (каталог)"
            )
        )
        assertEquals(
            "—",
            ProfileDisplayFormat.metroLabel(
                nearestMetroLabel = null,
                nearestMetroId = null,
                catalogLabel = "—"
            )
        )
    }

    @Test
    fun birthLine_includesFullYears() {
        assertEquals(
            "15.03.1990 (36 лет)",
            ProfileDisplayFormat.birthLine("1990-03-15", LocalDate.of(2026, 11, 20))
        )
    }

    @Test
    fun inclusiveVacationDays_prefersCoreAndIncludesBothDates() {
        assertEquals(5, ProfileDisplayFormat.inclusiveVacationDays(5, "2026-01-01", "2026-01-10"))
        assertEquals(1, ProfileDisplayFormat.inclusiveVacationDays(null, "2026-05-01", "2026-05-01"))
        assertEquals(10, ProfileDisplayFormat.inclusiveVacationDays(null, "2026-01-01", "2026-01-10"))
    }

    @Test
    fun phoneAndAddress_useCardLayout() {
        assertEquals("+7 (999) 123-45-67", ProfileDisplayFormat.phone("+79991234567"))
        assertEquals(
            "Санкт-Петербург, Невский, к. 2, д. 10, кв. 4",
            ProfileDisplayFormat.addressLine("Санкт-Петербург", "Невский", "2", "10", "4")
        )
    }

    @Test
    fun educationSpan_openEndedIsPresent() {
        assertEquals("01.09.2020 - по н.в.", ProfileDisplayFormat.educationSpan("2020-09-01", null))
        assertEquals("Иванов И.О.", ProfileDisplayFormat.compactFio("Иванов И. И."))
    }
}
