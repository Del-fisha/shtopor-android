package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotTodayDashboardJsonParseTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun parsesRoutesEmployeesAndArticleCounts() {
        val json = """
            {
              "operationalDate": "2026-08-09",
              "departmentNumber": 1,
              "routes": [
                {
                  "routeLabel": "МП 28",
                  "employees": ["Иванов И.", "Петров П."],
                  "ibdr": 5,
                  "migrant": 2,
                  "statements": 1,
                  "claims": 0,
                  "administrative": 5,
                  "criminal": 1,
                  "administrativeArticles": [
                    {"label": "20.20", "count": 3},
                    {"label": "18.8", "count": 2}
                  ],
                  "criminalTypes": [
                    {"label": "Федеральный розыск", "count": 1}
                  ]
                }
              ],
              "totals": {
                "ibdr": 5,
                "migrant": 2,
                "statements": 1,
                "claims": 0,
                "administrative": 5,
                "criminal": 1,
                "administrativeArticles": [
                  {"label": "20.20", "count": 3},
                  {"label": "18.8", "count": 2}
                ],
                "criminalTypes": [
                  {"label": "Федеральный розыск", "count": 1}
                ]
              }
            }
        """.trimIndent()

        val parsed = moshi.adapter(BotTodayDashboardResponse::class.java).fromJson(json)
        assertNotNull(parsed)
        assertEquals("2026-08-09", parsed!!.operationalDate)
        assertEquals(1, parsed.departmentNumber)
        val route = parsed.routes!!.first()
        assertEquals(listOf("Иванов И.", "Петров П."), route.employees)
        val lines = ArticleCountFormatter.fromCounts(
            route.administrativeArticles!!.map { LabeledCount(it.label!!, it.count!!) }
        )
        assertEquals(listOf("20.20 x 3", "18.8 x 2"), lines)
        assertTrue(parsed.totals!!.ibdr == 5)
    }
}
