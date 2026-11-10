package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileDisplayFormatTest {

    @Test
    fun idCardDate_formatsIsoToDdMmmYyyyRussian() {
        assertEquals("15.марта.2024", ProfileDisplayFormat.idCardDate("2024-03-15"))
        assertEquals("09.августа.2026", ProfileDisplayFormat.idCardDate("2026-08-09T00:00:00"))
    }

    @Test
    fun idCardDate_blankStaysDash() {
        assertEquals("—", ProfileDisplayFormat.idCardDate(null))
        assertEquals("—", ProfileDisplayFormat.idCardDate("  "))
    }

    @Test
    fun idCardRange_joinsWithDash() {
        assertEquals(
            "01.января.2020 — 31.декабря.2030",
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
}
