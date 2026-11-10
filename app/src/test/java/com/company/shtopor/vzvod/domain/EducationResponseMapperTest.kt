package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class EducationResponseMapperTest {

    @Test
    fun maps204ToNull() {
        val response = Response.success<BotEducationDto>(204, null)
        assertNull(EducationResponseMapper.from(response))
    }

    @Test
    fun maps200Body() {
        val dto = BotEducationDto(id = "1", typeId = "A")
        val response = Response.success(dto)
        assertEquals(dto, EducationResponseMapper.from(response))
    }

    @Test
    fun mapsEmpty200ToNull() {
        val response = Response.success<BotEducationDto>(null)
        assertNull(EducationResponseMapper.from(response))
    }
}
