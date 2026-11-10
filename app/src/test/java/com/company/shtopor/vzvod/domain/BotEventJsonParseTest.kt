package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotEventItem
import com.company.shtopor.vzvod.data.remote.dto.BotEventsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BotEventJsonParseTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun parsesPlaceAndShiftOfDepartmentFromCoreJson() {
        val json = """
            {
              "items": [
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "date": "2026-08-10",
                  "time": "12:30:00",
                  "name": "Тест",
                  "place": "Дворцовая площадь",
                  "eventType": "OTHER",
                  "shiftOfDepartment": 1
                }
              ]
            }
        """.trimIndent()

        val parsed = moshi.adapter(BotEventsResponse::class.java).fromJson(json)
        assertNotNull(parsed)
        val item = parsed!!.items!!.first()
        assertEquals("Дворцовая площадь", item.place)
        assertEquals(1, item.shiftOfDepartment)
        assertEquals("Тест", item.name)
    }

    @Test
    fun filterUsesParsedShiftOfDepartment() {
        val events = listOf(
            BotEventItem(date = "2026-08-10", name = "A", place = "P1", shiftOfDepartment = 1),
            BotEventItem(date = "2026-08-11", name = "B", place = "P2", shiftOfDepartment = 2)
        )
        val dep1 = EventListFilter.filter(events, EventDepartmentFilter.DEP_1) { it.shiftOfDepartment }
        assertEquals(listOf("A"), dep1.map { it.name })
        assertEquals("смена 2 отделения", EventListFilter.shiftCaption(events[1].shiftOfDepartment))
    }
}
