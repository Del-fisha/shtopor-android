package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotColleagueItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ColleaguePickOrderTest {

    @Test
    fun postThenLastName() {
        val policeman = colleague("2", "Андреев А. А.", "D")
        val commander = colleague("1", "Яковлев Я. Я.", "C")
        val intern = colleague("3", "Борисов Б. Б.", "E")

        val sorted = listOf(policeman, intern, commander).sortedForShiftPartnerPick()

        assertEquals(listOf(commander, policeman, intern), sorted)
    }

    private fun colleague(id: String, name: String, post: String) = BotColleagueItem(
        serviceInfoId = id,
        label = name,
        post = post
    )
}
