package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotCatalogOptionsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotEnumOption
import com.company.shtopor.vzvod.data.remote.dto.BotShiftRouteOption
import com.company.shtopor.vzvod.data.remote.dto.BotStringEnumOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCatalogTest {

    @Test
    fun fromDto_usesServerVocationTypesIncludingNewOnes() {
        val catalog = AppCatalog.fromDto(
            BotCatalogOptionsResponse(
                vocationTypes = listOf(
                    BotEnumOption(10, "Основной"),
                    BotEnumOption(20, "Дополнительный"),
                    BotEnumOption(30, "Часть основного"),
                    BotEnumOption(40, "Учебный отпуск"),
                    BotEnumOption(50, "Цпп")
                ),
                shiftRoutes = emptyList(),
                shiftTypes = emptyList()
            )
        )

        assertEquals(5, catalog.vocationTypes.size)
        assertTrue(catalog.vocationTypes.any { it.id == 40 && it.label == "Учебный отпуск" })
        assertTrue(catalog.vocationTypes.any { it.id == 50 && it.label == "Цпп" })
        assertEquals("Учебный отпуск", catalog.vocationLabel(40))
    }

    @Test
    fun fromDto_emptySectionsFallBackToLocalDefaults() {
        val catalog = AppCatalog.fromDto(
            BotCatalogOptionsResponse(
                vocationTypes = emptyList(),
                shiftRoutes = emptyList(),
                shiftTypes = emptyList()
            )
        )
        val fallback = AppCatalog.fallback()

        assertEquals(fallback.vocationTypes, catalog.vocationTypes)
        assertEquals(fallback.shiftRoutes, catalog.shiftRoutes)
        assertEquals(fallback.shiftTypes, catalog.shiftTypes)
    }

    @Test
    fun fromDto_usesServerRoutesAndDefaultTypes() {
        val catalog = AppCatalog.fromDto(
            BotCatalogOptionsResponse(
                vocationTypes = emptyList(),
                shiftRoutes = listOf(
                    BotShiftRouteOption("МП 28", "МП 28", "VZVOD_ROUTE"),
                    BotShiftRouteOption("Новый маршрут", "Новый маршрут", "CHECKING")
                ),
                shiftTypes = listOf(
                    BotStringEnumOption("CHECKING", "Проверки маршрутов"),
                    BotStringEnumOption("VZVOD_ROUTE", "Маршрут взвода")
                )
            )
        )

        assertEquals(2, catalog.shiftRoutes.size)
        assertEquals("CHECKING", catalog.defaultType("Новый маршрут"))
        assertEquals("Проверки маршрутов", catalog.typeLabel("CHECKING"))
    }
}
