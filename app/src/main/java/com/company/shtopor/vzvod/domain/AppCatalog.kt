package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotCatalogOptionsResponse

data class IntCatalogOption(val id: Int, val label: String)

data class StringCatalogOption(val id: String, val label: String)

data class ShiftRouteCatalogOption(
    val id: String,
    val label: String,
    val defaultTypeOfShiftId: String?
)

/**
 * Справочники для выпадающих списков. Источник истины — сервер ([BotCatalogOptionsResponse]);
 * [fallback] — только офлайн / пустой кэш.
 */
data class AppCatalog(
    val vocationTypes: List<IntCatalogOption>,
    val shiftRoutes: List<ShiftRouteCatalogOption>,
    val shiftTypes: List<StringCatalogOption>,
    val metroStations: List<IntCatalogOption> = emptyList()
) {
    fun metroLabel(metroId: Int?): String =
        metroStations.firstOrNull { it.id == metroId }?.label ?: "—"

    fun vocationLabel(typeId: Int?): String =
        vocationTypes.firstOrNull { it.id == typeId }?.label ?: "Отпуск"

    fun typeLabel(typeId: String?): String {
        if (typeId.isNullOrBlank()) return "—"
        shiftTypes.firstOrNull { it.id == typeId }?.label?.let { return it }
        return ShiftRoutes.fallbackTypeLabel(typeId)
    }

    fun defaultType(routeId: String): String =
        shiftRoutes.firstOrNull { it.id == routeId }?.defaultTypeOfShiftId
            ?: ShiftRoutes.fallbackDefaultType(routeId)

    companion object {
        fun fallback(): AppCatalog = AppCatalog(
            vocationTypes = listOf(
                IntCatalogOption(10, "Основной"),
                IntCatalogOption(30, "Часть основного"),
                IntCatalogOption(20, "Дополнительный")
            ),
            shiftRoutes = listOf(
                "МП 28", "МП 30", "МП 31", "МП 32", "МП 5", "МП 6", "СП 3", "СП 18", "Другое"
            ).map { id ->
                ShiftRouteCatalogOption(id, id, ShiftRoutes.fallbackDefaultType(id))
            },
            shiftTypes = listOf(
                StringCatalogOption("VZVOD_ROUTE", "Маршрут взвода"),
                StringCatalogOption("BAT_POST", "Пост батальона"),
                StringCatalogOption("Strengthening", "Усиление"),
                StringCatalogOption("CHECKING", "Проверка"),
                StringCatalogOption("ON_BASES", "На базах")
            ),
            metroStations = emptyList()
        )

        fun fromDto(dto: BotCatalogOptionsResponse?): AppCatalog {
            val base = fallback()
            if (dto == null) return base

            val vocationTypes = dto.vocationTypes.orEmpty()
                .mapNotNull { opt ->
                    val label = opt.label?.trim().orEmpty()
                    if (label.isEmpty()) null else IntCatalogOption(opt.id, label)
                }
                .ifEmpty { base.vocationTypes }

            val shiftRoutes = dto.shiftRoutes.orEmpty()
                .mapNotNull { opt ->
                    val id = opt.id?.trim().orEmpty()
                    if (id.isEmpty()) return@mapNotNull null
                    val label = opt.label?.trim().orEmpty().ifEmpty { id }
                    ShiftRouteCatalogOption(id, label, opt.defaultTypeOfShiftId)
                }
                .ifEmpty { base.shiftRoutes }

            val shiftTypes = dto.shiftTypes.orEmpty()
                .mapNotNull { opt ->
                    val id = opt.id?.trim().orEmpty()
                    if (id.isEmpty()) return@mapNotNull null
                    val label = opt.label?.trim().orEmpty().ifEmpty { id }
                    StringCatalogOption(id, label)
                }
                .ifEmpty { base.shiftTypes }

            val metroStations = dto.metroStations.orEmpty()
                .mapNotNull { opt ->
                    val label = opt.label?.trim().orEmpty()
                    if (label.isEmpty()) null else IntCatalogOption(opt.id, label)
                }

            return AppCatalog(vocationTypes, shiftRoutes, shiftTypes, metroStations)
        }
    }
}
