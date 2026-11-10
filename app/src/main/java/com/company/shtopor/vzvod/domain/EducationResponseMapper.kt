package com.company.shtopor.vzvod.domain

import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import retrofit2.Response

object EducationResponseMapper {
    fun from(response: Response<BotEducationDto>): BotEducationDto? {
        if (!response.isSuccessful) return null
        if (response.code() == 204) return null
        return response.body()
    }
}
