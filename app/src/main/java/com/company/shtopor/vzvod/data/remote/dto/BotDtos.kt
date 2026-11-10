package com.company.shtopor.vzvod.data.remote.dto

data class BotAuthRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null
)

data class BotAuthResponse(
    val userId: String,
    val displayName: String?,
    val username: String?,
    val role: String?,
    val fullAccess: Boolean? = false,
    val token: String
)

data class BotProfileResponse(
    val userId: String?,
    val displayName: String?,
    val rank: String?,
    val post: String?,
    val department: String?,
    val breastplate: String?,
    val medicalExamination: Boolean?,
    val mobilePhoneMasked: String?,
    val registration: BotAddressResponse?,
    val habitation: BotAddressResponse?,
    val idCardIssued: String?,
    val idCardUntil: String?,
    val nearestMetro: Int? = null,
    val nearestMetroLabel: String? = null
)

data class BotAddressResponse(
    val index: String? = null,
    val city: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val body: String? = null,
    val flat: String? = null,
    val typeOfHousing: String? = null,
    val statusOfHousing: String? = null,
    val summary: String? = null
)

data class BotAddressPatch(
    val index: String? = null,
    val city: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val body: String? = null,
    val flat: String? = null,
    val typeOfHousing: String? = null,
    val statusOfHousing: String? = null
)

data class BotProfilePatchRequest(
    val breastplate: String? = null,
    val medicalExamination: Boolean? = null,
    val registration: BotAddressPatch? = null,
    val habitation: BotAddressPatch? = null,
    val nearestMetro: Int? = null
)

data class BotShiftsResponse(
    val items: List<BotShiftItem>? = emptyList()
)

data class BotShiftItem(
    val id: String,
    val date: String?,
    val route: String?,
    val shiftType: String?,
    val typeOfShiftId: String?,
    val departmentToday: String?,
    val startTime: String?,
    val endTime: String?,
    val countOfStatements: Int? = 0,
    val countOfClaims: Int? = 0,
    val ibdr: Int? = 0,
    val migrant: Int? = 0,
    val administrativeViolationsCount: Int? = 0,
    val criminalViolationsCount: Int? = 0,
    val partnerServiceInfoId: String? = null,
    val otherParticipantServiceInfoIds: List<String>? = emptyList()
) {
    val isOpen: Boolean get() = com.company.shtopor.vzvod.domain.ActiveShiftPolicy.isOpen(endTime)
}

data class BotShiftUpsertRequest(
    val date: String? = null,
    val routeId: String? = null,
    val typeOfShiftId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val partnerServiceInfoId: String? = null,
    val partnerServiceInfoIds: List<String>? = null,
    val countOfStatements: Int? = null,
    val countOfClaims: Int? = null,
    val ibdr: Int? = null,
    val migrant: Int? = null
)

data class BotShiftEndTimeRequest(val endTime: String)

data class BotShiftMetricDeltaRequest(val delta: Int)

data class BotCheckableRoutesResponse(
    val routes: List<BotCheckableRouteItem>? = emptyList()
)

data class BotCheckableRouteItem(
    val routeId: String,
    val routeLabel: String? = null,
    val checks: List<BotRouteCheckItem>? = emptyList()
)

data class BotRouteCheckItem(
    val id: String? = null,
    val checkedAt: String? = null,
    val checkerLabel: String? = null,
    val display: String? = null
)

data class BotRouteCheckCreateRequest(
    val routeId: String
)

data class BotColleaguesResponse(
    val items: List<BotColleagueItem>? = emptyList(),
    val page: Int? = 0,
    val hasMore: Boolean? = false
)

data class BotColleagueItem(
    val serviceInfoId: String,
    /** Краткая подпись с сервера: «Фамилия И. О.» */
    val label: String? = null,
    /** @deprecated старое имя поля; сервер шлёт label */
    val displayName: String? = null,
    val department: Int? = null,
    val rank: String? = null
) {
    val shortName: String
        get() = label?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: "Сотрудник"
}

data class BotVacationsResponse(
    val balance: BotVacationBalance? = null,
    val items: List<BotVocationItem>? = emptyList()
)

data class BotVacationBalance(
    val entitled: Int? = 0,
    val available: Int? = 0,
    val used: Int? = 0
)

data class BotVocationItem(
    val id: String? = null,
    val startDate: String?,
    val endDate: String?,
    val countOfDays: Int? = null,
    val typeId: Int? = null,
    val typeLabel: String? = null,
    val hasDeparture: Boolean? = null,
    val cityToDrive: String? = null,
    val daysAddedByDeparture: Int? = null
)

data class BotVocationCreateRequest(
    val typeId: Int,
    val startDate: String,
    val endDate: String,
    val hasDeparture: Boolean? = false,
    val cityToDrive: String? = null,
    val daysAddedByDeparture: Int? = 0
)

data class BotEducationDto(
    val id: String? = null,
    val started: String? = null,
    val until: String? = null,
    val typeId: String? = null,
    val typeLabel: String? = null,
    val statusId: String? = null,
    val statusLabel: String? = null,
    val nameOfInstitution: String? = null
)

data class BotEducationUpsertRequest(
    val started: String? = null,
    val until: String? = null,
    val typeId: String? = null,
    val nameOfInstitution: String? = null
)

data class BotVehiclesResponse(val items: List<BotVehicleItem>? = emptyList())

data class BotVehicleItem(
    val id: String,
    val stateNumber: String?,
    val brand: String? = null,
    val model: String? = null,
    val registrationCertificate: String? = null,
    val insurance: String? = null
)

data class BotVehicleUpsertRequest(
    val stateNumber: String,
    val brand: String? = null,
    val model: String? = null,
    val registrationCertificate: String? = null,
    val insurance: String? = null
)

data class BotEventsResponse(
    val items: List<BotEventItem>? = emptyList()
)

data class BotEventItem(
    val id: String? = null,
    val date: String?,
    val time: String? = null,
    val name: String?,
    val place: String? = null,
    val eventType: String? = null,
    val shiftOfDepartment: Int? = null
)

data class BotEventCreateRequest(
    val name: String,
    val date: String,
    val time: String? = null,
    val place: String? = null,
    val description: String? = null,
    val eventType: String? = null
)

data class BotTodayDashboardResponse(
    val operationalDate: String? = null,
    val departmentNumber: Int? = null,
    val routes: List<BotTodayRouteRow>? = emptyList(),
    val totals: BotTodayTotals? = null
)

data class BotTodayRouteRow(
    val routeLabel: String? = null,
    val employees: List<String>? = emptyList(),
    val ibdr: Int? = 0,
    val migrant: Int? = 0,
    val statements: Int? = 0,
    val claims: Int? = 0,
    val administrative: Int? = 0,
    val criminal: Int? = 0,
    val administrativeArticles: List<BotLabeledCount>? = emptyList(),
    val criminalTypes: List<BotLabeledCount>? = emptyList()
)

data class BotTodayTotals(
    val ibdr: Int? = 0,
    val migrant: Int? = 0,
    val statements: Int? = 0,
    val claims: Int? = 0,
    val administrative: Int? = 0,
    val criminal: Int? = 0,
    val administrativeArticles: List<BotLabeledCount>? = emptyList(),
    val criminalTypes: List<BotLabeledCount>? = emptyList()
)

data class BotLabeledCount(
    val label: String? = null,
    val count: Int? = 0
)

data class BotViolationOptionsResponse(
    val impacts: List<BotEnumOption>? = emptyList(),
    val administrativeArticles: List<BotEnumOption>? = emptyList(),
    val criminalTypes: List<BotEnumOption>? = emptyList()
)

data class BotCatalogOptionsResponse(
    val vocationTypes: List<BotEnumOption>? = emptyList(),
    val shiftRoutes: List<BotShiftRouteOption>? = emptyList(),
    val shiftTypes: List<BotStringEnumOption>? = emptyList(),
    val metroStations: List<BotEnumOption>? = emptyList()
)

data class BotShiftRouteOption(
    val id: String?,
    val label: String?,
    val defaultTypeOfShiftId: String? = null
)

data class BotStringEnumOption(
    val id: String?,
    val label: String?
)

data class BotEnumOption(
    val id: Int,
    val label: String?
)

data class BotAdministrativeViolationCreateRequest(
    val impactId: Int,
    val articleId: Int
)

data class BotCriminalViolationCreateRequest(
    val impactId: Int,
    val typeId: Int
)
