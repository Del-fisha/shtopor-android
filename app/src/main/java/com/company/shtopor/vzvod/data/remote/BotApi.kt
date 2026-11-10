package com.company.shtopor.vzvod.data.remote

import com.company.shtopor.vzvod.data.remote.dto.BotAdministrativeViolationCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotAuthRequest
import com.company.shtopor.vzvod.data.remote.dto.BotAuthResponse
import com.company.shtopor.vzvod.data.remote.dto.BotCatalogOptionsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRoutesResponse
import com.company.shtopor.vzvod.data.remote.dto.BotColleaguesResponse
import com.company.shtopor.vzvod.data.remote.dto.BotCriminalViolationCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import com.company.shtopor.vzvod.data.remote.dto.BotEducationUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEventCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEventItem
import com.company.shtopor.vzvod.data.remote.dto.BotEventsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotProfilePatchRequest
import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftEndTimeRequest
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftMetricDeltaRequest
import com.company.shtopor.vzvod.data.remote.dto.BotShiftUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotShiftsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVacationsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleItem
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotVehiclesResponse
import com.company.shtopor.vzvod.data.remote.dto.BotViolationOptionsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVocationCreateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

/**
 * Android API Core: /api/mobile
 */
interface BotApi {
    @POST("/api/mobile/auth")
    suspend fun auth(@Body body: BotAuthRequest): BotAuthResponse

    @GET("/api/mobile/me/profile")
    suspend fun profile(@Header("X-Mobile-Token") token: String): BotProfileResponse

    @PUT("/api/mobile/me/profile")
    suspend fun updateProfile(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotProfilePatchRequest
    ): BotProfileResponse

    @GET("/api/mobile/me/shifts")
    suspend fun shifts(@Header("X-Mobile-Token") token: String): BotShiftsResponse

    @GET("/api/mobile/me/shifts/{id}")
    suspend fun shift(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String
    ): BotShiftItem

    @POST("/api/mobile/me/shifts")
    suspend fun createShift(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotShiftUpsertRequest
    ): BotShiftItem

    @PUT("/api/mobile/me/shifts/{id}")
    suspend fun updateShift(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftUpsertRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/end-time")
    suspend fun endShift(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftEndTimeRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/ibdr")
    suspend fun adjustIbdr(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftMetricDeltaRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/migrant")
    suspend fun adjustMigrant(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftMetricDeltaRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/migrant-and-ibdr")
    suspend fun adjustMigrantAndIbdr(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftMetricDeltaRequest
    ): BotShiftItem

    @GET("/api/mobile/me/shifts/{id}/checkable-routes")
    suspend fun checkableRoutes(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String
    ): BotCheckableRoutesResponse

    @POST("/api/mobile/me/shifts/{id}/route-checks")
    suspend fun recordRouteCheck(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotRouteCheckCreateRequest
    ): BotRouteCheckItem

    @DELETE("/api/mobile/me/route-checks/{checkId}")
    suspend fun deleteRouteCheck(
        @Header("X-Mobile-Token") token: String,
        @Path("checkId") checkId: String
    )

    @POST("/api/mobile/me/shifts/{id}/count-of-statements")
    suspend fun adjustStatements(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftMetricDeltaRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/count-of-claims")
    suspend fun adjustClaims(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotShiftMetricDeltaRequest
    ): BotShiftItem

    @GET("/api/mobile/me/violation-options")
    suspend fun violationOptions(@Header("X-Mobile-Token") token: String): BotViolationOptionsResponse

    @GET("/api/mobile/me/catalog-options")
    suspend fun catalogOptions(@Header("X-Mobile-Token") token: String): BotCatalogOptionsResponse

    @POST("/api/mobile/me/shifts/{id}/administrative-violations")
    suspend fun createAdminViolation(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotAdministrativeViolationCreateRequest
    ): BotShiftItem

    @POST("/api/mobile/me/shifts/{id}/criminal-violations")
    suspend fun createCriminalViolation(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotCriminalViolationCreateRequest
    ): BotShiftItem

    @GET("/api/mobile/me/colleagues")
    suspend fun colleagues(
        @Header("X-Mobile-Token") token: String,
        @Query("department") department: Int,
        @Query("page") page: Int = 0,
        @Query("excludeShiftId") excludeShiftId: String? = null
    ): BotColleaguesResponse

    @GET("/api/mobile/me/vacations")
    suspend fun vacations(@Header("X-Mobile-Token") token: String): BotVacationsResponse

    @POST("/api/mobile/me/vacations")
    suspend fun createVacation(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotVocationCreateRequest
    ): BotVacationsResponse

    @DELETE("/api/mobile/me/vacations/{id}")
    suspend fun deleteVacation(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String
    ): BotVacationsResponse

    @GET("/api/mobile/me/education")
    suspend fun education(@Header("X-Mobile-Token") token: String): Response<BotEducationDto>

    @PUT("/api/mobile/me/education")
    suspend fun upsertEducation(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotEducationUpsertRequest
    ): BotEducationDto

    @GET("/api/mobile/me/vehicles")
    suspend fun vehicles(@Header("X-Mobile-Token") token: String): BotVehiclesResponse

    @POST("/api/mobile/me/vehicles")
    suspend fun createVehicle(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotVehicleUpsertRequest
    ): BotVehicleItem

    @PUT("/api/mobile/me/vehicles/{id}")
    suspend fun updateVehicle(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String,
        @Body body: BotVehicleUpsertRequest
    ): BotVehicleItem

    @DELETE("/api/mobile/me/vehicles/{id}")
    suspend fun deleteVehicle(
        @Header("X-Mobile-Token") token: String,
        @Path("id") id: String
    )

    @GET("/api/mobile/me/events")
    suspend fun events(
        @Header("X-Mobile-Token") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): BotEventsResponse

    @POST("/api/mobile/me/events")
    suspend fun createEvent(
        @Header("X-Mobile-Token") token: String,
        @Body body: BotEventCreateRequest
    ): BotEventItem

    @GET("/api/mobile/me/today-dashboard")
    suspend fun todayDashboard(
        @Header("X-Mobile-Token") token: String,
        @Query("date") date: String? = null
    ): BotTodayDashboardResponse

    @GET("/api/mobile/me/day-dashboard")
    suspend fun dayDashboard(
        @Header("X-Mobile-Token") token: String,
        @Query("date") date: String
    ): BotTodayDashboardResponse
}
