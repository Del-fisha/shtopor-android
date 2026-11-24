package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotColleagueItem

/**
 * Список «добавить сотрудника» при создании смены: должность, затем фамилия.
 * Id должности идут A…E от старшей к младшей. Пустая должность — в конце.
 */
fun List<BotColleagueItem>.sortedForShiftPartnerPick(): List<BotColleagueItem> =
    sortedWith(
        compareBy<BotColleagueItem> { it.post ?: "~" }
            .thenBy { it.shortName.lowercase() }
    )
