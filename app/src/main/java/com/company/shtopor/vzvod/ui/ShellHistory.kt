package com.company.shtopor.vzvod.ui

/**
 * История разделов и нижних вкладок. Системная «Назад» снимает последний экран.
 * Новый раздел добавляется в [AppSections] и получает свой список вкладок.
 */
data class ShellPlace(
    val areaId: String,
    val tabId: String?
)

data class ShellHistory(
    private val stack: List<ShellPlace> = listOf(
        ShellPlace(AppSections.SERVICE, AppSections.TAB_SHIFTS)
    )
) {
    val place: ShellPlace get() = stack.last()
    val areaId: String get() = place.areaId
    val tabId: String? get() = place.tabId

    fun canGoBack(): Boolean = stack.size > 1

    fun openArea(areaId: String, defaultTabId: String?): ShellHistory {
        if (areaId == this.areaId) return this
        return copy(stack = stack + ShellPlace(areaId, defaultTabId))
    }

    fun selectTab(tabId: String): ShellHistory {
        if (tabId == this.tabId) return this
        return copy(stack = stack + place.copy(tabId = tabId))
    }

    fun back(): ShellHistory {
        if (!canGoBack()) return this
        return copy(stack = stack.dropLast(1))
    }
}

data class AppSection(
    val id: String,
    val title: String,
    val tabs: List<SectionTab>
)

data class SectionTab(
    val id: String,
    val label: String
)

object AppSections {
    const val SERVICE = "service"
    const val GAME = "game"
    const val TAB_SHIFTS = "shifts"
    const val TAB_VACATIONS = "vacations"
    const val TAB_CALENDAR = "calendar"
    const val TAB_PROFILE = "profile"

    val serviceTabs = listOf(
        SectionTab(TAB_SHIFTS, "Смены"),
        SectionTab(TAB_VACATIONS, "Отпуска"),
        SectionTab(TAB_CALENDAR, "Календарь"),
        SectionTab(TAB_PROFILE, "Профиль")
    )

    val all = listOf(
        AppSection(SERVICE, "Служебное", serviceTabs),
        AppSection(GAME, "Игра", emptyList())
    )

    fun byId(id: String): AppSection = all.first { it.id == id }
}
