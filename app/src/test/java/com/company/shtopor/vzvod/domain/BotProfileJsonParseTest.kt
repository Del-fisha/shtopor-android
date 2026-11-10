package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BotProfileJsonParseTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun parsesNearestMetroAndFlat() {
        val json = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "displayName": "Тест",
              "nearestMetro": 114,
              "nearestMetroLabel": "Балтийская",
              "habitation": {
                "city": "Санкт-Петербург",
                "street": "Невский",
                "houseNumber": "1",
                "flat": "12"
              },
              "idCardIssued": "2020-01-15",
              "idCardUntil": "2030-01-15"
            }
        """.trimIndent()

        val parsed = moshi.adapter(BotProfileResponse::class.java).fromJson(json)
        assertNotNull(parsed)
        assertEquals(114, parsed!!.nearestMetro)
        assertEquals("Балтийская", parsed.nearestMetroLabel)
        assertEquals("12", parsed.habitation?.flat)
        assertEquals(
            "15.января.2020 — 15.января.2030",
            ProfileDisplayFormat.idCardRange(parsed.idCardIssued, parsed.idCardUntil)
        )
    }
}
