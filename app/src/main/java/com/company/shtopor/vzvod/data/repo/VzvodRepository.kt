package com.company.shtopor.vzvod.data.repo

import com.company.shtopor.vzvod.data.local.AppDatabase
import com.company.shtopor.vzvod.data.local.CachedEventEntity
import com.company.shtopor.vzvod.data.local.CachedProfileEntity
import com.company.shtopor.vzvod.data.local.CachedShiftEntity
import com.company.shtopor.vzvod.data.local.CachedVacationsEntity
import com.company.shtopor.vzvod.data.local.LocalMetricsEntity
import com.company.shtopor.vzvod.data.local.OutboxOpEntity
import com.company.shtopor.vzvod.data.local.OutboxOpType
import com.company.shtopor.vzvod.data.local.OutboxStatus
import com.company.shtopor.vzvod.data.remote.ApiClientFactory
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.NetworkMonitor
import com.company.shtopor.vzvod.data.remote.dto.BotAdministrativeViolationCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotAuthRequest
import com.company.shtopor.vzvod.data.remote.dto.BotCatalogOptionsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotColleagueItem
import com.company.shtopor.vzvod.data.remote.dto.BotCriminalViolationCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import com.company.shtopor.vzvod.data.remote.dto.BotEducationUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEventCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotEventItem
import com.company.shtopor.vzvod.data.remote.dto.BotProfilePatchRequest
import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRouteItem
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRoutesResponse
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftEndTimeRequest
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftMetricDeltaRequest
import com.company.shtopor.vzvod.data.remote.dto.BotShiftUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVacationsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleItem
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotViolationOptionsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVocationCreateRequest
import com.company.shtopor.vzvod.data.remote.safeApiCall
import com.company.shtopor.vzvod.data.secure.SecureSessionStore
import com.company.shtopor.vzvod.domain.EducationResponseMapper
import com.company.shtopor.vzvod.domain.ActiveShiftPolicy
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.domain.CheckingShiftPolicy
import com.company.shtopor.vzvod.domain.AppRole
import com.company.shtopor.vzvod.domain.LocalMetricsSyncPolicy
import com.company.shtopor.vzvod.domain.LocalShiftMetrics
import com.company.shtopor.vzvod.domain.OutboxMetricSyncPlanner
import com.company.shtopor.vzvod.domain.PendingOutboxOp
import com.company.shtopor.vzvod.domain.RoleResolver
import com.company.shtopor.vzvod.domain.ShiftMetricsLogic
import com.company.shtopor.vzvod.notify.ShiftForegroundService
import com.company.shtopor.vzvod.notify.ShiftNotificationManager
import com.company.shtopor.vzvod.sync.SyncScheduler
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

class VzvodRepository(
    private val apiFactory: ApiClientFactory,
    private val session: SecureSessionStore,
    private val db: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    private val appContext: android.content.Context
) {
    private val moshi: Moshi get() = apiFactory.moshi()
    private val shiftAdapter by lazy { moshi.adapter(BotShiftItem::class.java) }
    private val profileAdapter by lazy { moshi.adapter(BotProfileResponse::class.java) }
    private val vacationsAdapter by lazy { moshi.adapter(BotVacationsResponse::class.java) }
    private val eventAdapter by lazy { moshi.adapter(BotEventItem::class.java) }
    private val violationOptionsAdapter by lazy { moshi.adapter(BotViolationOptionsResponse::class.java) }
    private val catalogOptionsAdapter by lazy { moshi.adapter(BotCatalogOptionsResponse::class.java) }
    private val cachePrefs by lazy {
        appContext.getSharedPreferences("vzvod_offline_cache", android.content.Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_VIOLATION_OPTIONS = "violation_options_json"
        private const val KEY_CATALOG_OPTIONS = "catalog_options_json"
    }

    private val syncMutex = Mutex()
    private val checkableRoutesCache = java.util.concurrent.ConcurrentHashMap<String, List<BotCheckableRouteItem>>()

    fun pendingOpsFlow(): Flow<Int> = db.outboxDao().pendingCountFlow()

    fun isOnline(): Boolean = networkMonitor.isOnlineNow()

    fun token(): String = session.mobileToken
        ?: throw IllegalStateException("Не авторизован")

    fun role(): AppRole = RoleResolver.fromServer(session.role, session.fullAccess)

    suspend fun login(username: String, password: String): ApiResult<BotProfileResponse> {
        val auth = safeApiCall {
            apiFactory.api().auth(
                BotAuthRequest(
                    username = username.trim(),
                    password = password,
                    deviceId = session.deviceId()
                )
            )
        }
        when (auth) {
            is ApiResult.Err -> return auth
            is ApiResult.Ok -> {
                session.saveAuth(
                    userId = auth.value.userId,
                    displayName = auth.value.displayName,
                    username = auth.value.username,
                    role = auth.value.role,
                    fullAccess = auth.value.fullAccess == true,
                    token = auth.value.token
                )
            }
        }
        val profile = refreshProfile()
        if (profile is ApiResult.Ok) {
            // Сразу подтянуть активную смену с Core (или отсутствие смены).
            refreshShifts()
            syncActiveShiftNotification()
        }
        return profile
    }

    fun logout() {
        session.clearSession()
        ShiftForegroundService.stop(appContext)
    }

    /**
     * Только кэш — без refreshShifts. Иначе reconcile→notify→openShift→refresh→reconcile = ANR.
     */
    suspend fun syncActiveShiftNotification() {
        if (!ActiveShiftPolicy.notificationUsesCacheOnly()) return
        refreshNotificationFromCache()
    }

    private suspend fun refreshNotificationFromCache() {
        if (!session.isLoggedIn) {
            ShiftForegroundService.stop(appContext)
            return
        }
        val open = cachedOpenShift()
        if (open != null) {
            val routes = if (CheckingShiftPolicy.isChecking(open.typeOfShiftId)) {
                ensureCheckableRoutes(open.id)
            } else {
                emptyList()
            }
            ShiftNotificationManager.showOrUpdate(appContext, open, routes)
        } else {
            ShiftForegroundService.stop(appContext)
        }
    }

    suspend fun loadCheckableRoutes(shiftId: String): ApiResult<BotCheckableRoutesResponse> {
        val result = safeApiCall { apiFactory.api().checkableRoutes(token(), shiftId) }
        if (result is ApiResult.Ok) {
            checkableRoutesCache[shiftId] = result.value.routes.orEmpty()
        }
        return result
    }

    fun cachedCheckableRoutes(shiftId: String): List<BotCheckableRouteItem> =
        checkableRoutesCache[shiftId].orEmpty()

    private suspend fun ensureCheckableRoutes(shiftId: String): List<BotCheckableRouteItem> {
        loadCheckableRoutes(shiftId)
        return checkableRoutesCache[shiftId].orEmpty()
    }

    suspend fun recordRouteCheck(shiftId: String, routeId: String): ApiResult<BotRouteCheckItem> {
        val result = safeApiCall {
            apiFactory.api().recordRouteCheck(token(), shiftId, BotRouteCheckCreateRequest(routeId))
        }
        if (result is ApiResult.Ok) {
            loadCheckableRoutes(shiftId)
            refreshNotificationFromCache()
        }
        return result
    }

    suspend fun deleteRouteCheck(shiftId: String, checkId: String): ApiResult<Unit> {
        val result = safeApiCall { apiFactory.api().deleteRouteCheck(token(), checkId) }
        if (result is ApiResult.Ok) {
            loadCheckableRoutes(shiftId)
            refreshNotificationFromCache()
        }
        return result
    }

    /** Открытая смена только из Room (без сети) — для UI после create и офлайна. */
    suspend fun getCachedOpenShift(): BotShiftItem? {
        val cached = db.cacheDao().openShift() ?: return null
        val shift = runCatching { shiftAdapter.fromJson(cached.json) }.getOrNull() ?: return null
        if (!ActiveShiftPolicy.isOpen(shift.endTime)) return null
        return applyLocalOverlay(shift)
    }

    private suspend fun cachedOpenShift(): BotShiftItem? = getCachedOpenShift()

    suspend fun refreshProfile(): ApiResult<BotProfileResponse> {
        if (!session.isLoggedIn) return ApiResult.Err(401, "Не авторизован")
        val result = safeApiCall { apiFactory.api().profile(token()) }
        if (result is ApiResult.Ok) {
            session.post = result.value.post
            session.displayName = result.value.displayName
            db.cacheDao().upsertProfile(
                CachedProfileEntity(json = profileAdapter.toJson(result.value), updatedAt = System.currentTimeMillis())
            )
        }
        return result
    }

    suspend fun cachedProfile(): BotProfileResponse? {
        val raw = db.cacheDao().profile()?.json ?: return null
        return runCatching { profileAdapter.fromJson(raw) }.getOrNull()
    }

    suspend fun updateProfile(patch: BotProfilePatchRequest): ApiResult<BotProfileResponse> {
        if (!networkMonitor.isOnlineNow()) {
            enqueue(OutboxOpType.UPDATE_PROFILE, moshi.adapter(BotProfilePatchRequest::class.java).toJson(patch))
            val local = cachedProfile()
            return if (local != null) ApiResult.Ok(local) else ApiResult.Err(0, "Сохранено в очередь", offline = true)
        }
        val result = safeApiCall { apiFactory.api().updateProfile(token(), patch) }
        if (result is ApiResult.Ok) {
            db.cacheDao().upsertProfile(
                CachedProfileEntity(json = profileAdapter.toJson(result.value), updatedAt = System.currentTimeMillis())
            )
        }
        return result
    }

    suspend fun refreshShifts(): ApiResult<List<BotShiftItem>> {
        val result = safeApiCall { apiFactory.api().shifts(token()) }
        if (result is ApiResult.Ok) {
            return ApiResult.Ok(reconcileShiftsFromServer(result.value.items.orEmpty()))
        }
        return when (result) {
            is ApiResult.Err -> {
                val cached = db.cacheDao().allShifts().mapNotNull {
                    runCatching { shiftAdapter.fromJson(it.json) }.getOrNull()
                        ?.let { shift -> if (shift.isOpen) applyLocalOverlay(shift) else shift }
                }
                if (cached.isNotEmpty()) ApiResult.Ok(cached) else result
            }
            else -> result as ApiResult<List<BotShiftItem>>
        }
    }

    /**
     * Core — источник истины по открытой смене (endTime пустой).
     * Не делаем clear всех смен: из‑за этого только что созданная смена пропадала из APK.
     */
    private suspend fun reconcileShiftsFromServer(items: List<BotShiftItem>): List<BotShiftItem> {
        val serverOpen = items.filter { it.isOpen }
        val serverOpenIds = serverOpen.map { it.id }.toSet()
        val serverIds = items.map { it.id }.toSet()
        val now = System.currentTimeMillis()

        for (cached in db.cacheDao().allShifts().filter { it.isOpen }) {
            if (cached.id in serverOpenIds) continue
            db.outboxDao().discardForShift(cached.id)
            db.cacheDao().deleteLocalMetrics(cached.id)
            when {
                cached.id.startsWith("local-") -> db.cacheDao().deleteShift(cached.id)
                cached.id in serverIds -> {
                    val closed = items.first { it.id == cached.id }
                    db.cacheDao().upsertShift(
                        CachedShiftEntity(
                            id = closed.id,
                            json = shiftAdapter.toJson(closed),
                            updatedAt = now,
                            isOpen = false
                        )
                    )
                }
                else -> {
                    // Была открыта локально, на сервере в выборке нет — считаем закрытой.
                    val old = runCatching { shiftAdapter.fromJson(cached.json) }.getOrNull()
                    if (old != null) {
                        val closed = old.copy(endTime = old.endTime?.takeIf { it.isNotBlank() } ?: "00:00")
                        db.cacheDao().upsertShift(
                            CachedShiftEntity(
                                id = closed.id,
                                json = shiftAdapter.toJson(closed),
                                updatedAt = now,
                                isOpen = false
                            )
                        )
                    } else {
                        db.cacheDao().deleteShift(cached.id)
                    }
                }
            }
        }

        if (items.isNotEmpty()) {
            db.cacheDao().upsertShifts(items.map {
                CachedShiftEntity(
                    id = it.id,
                    json = shiftAdapter.toJson(it),
                    updatedAt = now,
                    isOpen = it.isOpen
                )
            })
        }

        if (serverOpen.isEmpty()) {
            ShiftForegroundService.stop(appContext)
        } else {
            for (item in serverOpen) {
                val pending = db.outboxDao().pendingCountForShift(item.id)
                if (LocalMetricsSyncPolicy.shouldOverwriteLocalFromServer(pending)) {
                    persistLocalMetrics(item)
                }
            }
            // Только кэш — НЕ openShift()/refreshShifts (рекурсия → ANR).
            refreshNotificationFromCache()
        }

        return items.map { if (it.isOpen) applyLocalOverlay(it) else it }
    }

    /**
     * Немедленный слив outbox (без ожидания WorkManager).
     * Метрики (±ИБДР/мигрант/заявки) уходят абсолютными значениями из local_metrics —
     * чтобы таймаут после успеха на Core не давал дублей (5+5 → 13).
     */
    suspend fun syncOutboxNow(): Boolean = syncMutex.withLock {
        if (!session.isLoggedIn) return true
        if (!networkMonitor.isOnlineNow()) return false

        val dao = db.outboxDao()
        // Только зависшие после краша; не трогаем свежие IN_FLIGHT другой корутины.
        dao.recoverStaleInFlight(System.currentTimeMillis() - 90_000)
        val pendingEntities = dao.pending()
        if (pendingEntities.isEmpty()) {
            return true
        }

        val pendingOps = pendingEntities.map {
            PendingOutboxOp(id = it.id, type = it.type, shiftId = it.clientShiftId)
        }
        val localByShift = pendingOps
            .mapNotNull { it.shiftId }
            .distinct()
            .mapNotNull { sid ->
                val m = db.cacheDao().localMetrics(sid) ?: return@mapNotNull null
                sid to LocalShiftMetrics(m.ibdr, m.migrant, m.statements, m.claims)
            }
            .toMap()
        val plan = OutboxMetricSyncPlanner.plan(pendingOps, localByShift)
        val byId = pendingEntities.associateBy { it.id }

        val api = apiFactory.api()
        val token = token()
        var hadFailure = false
        var stoppedOffline = false

        for (abs in plan.absoluteMetricSyncs) {
            val cached = db.cacheDao().shiftById(abs.shiftId)
                ?.let { runCatching { shiftAdapter.fromJson(it.json) }.getOrNull() }
            if (cached == null ||
                cached.date.isNullOrBlank() ||
                cached.startTime.isNullOrBlank() ||
                cached.route.isNullOrBlank() ||
                cached.typeOfShiftId.isNullOrBlank()
            ) {
                hadFailure = true
                continue
            }
            // Помечаем consumed как IN_FLIGHT, чтобы параллельный sync не реплеил ±1.
            for (opId in abs.consumedOpIds) {
                val op = byId[opId] ?: continue
                dao.update(op.copy(status = OutboxStatus.IN_FLIGHT.name, attempts = op.attempts + 1))
            }
            val body = BotShiftUpsertRequest(
                date = cached.date,
                routeId = cached.route,
                typeOfShiftId = cached.typeOfShiftId,
                startTime = cached.startTime,
                ibdr = abs.metrics.ibdr,
                migrant = abs.metrics.migrant,
                countOfStatements = abs.metrics.statements,
                countOfClaims = abs.metrics.claims
            )
            val outcome = safeApiCall { api.updateShift(token, abs.shiftId, body) }
            when (outcome) {
                is ApiResult.Ok -> {
                    if (abs.consumedOpIds.isNotEmpty()) {
                        dao.markDone(abs.consumedOpIds)
                    }
                    persistShiftCache(outcome.value)
                }
                is ApiResult.Err -> {
                    hadFailure = true
                    for (opId in abs.consumedOpIds) {
                        val op = byId[opId] ?: continue
                        val retryable = outcome.offline || outcome.code == 0 || outcome.code in 500..599
                        dao.update(
                            op.copy(
                                status = if (retryable) OutboxStatus.PENDING.name else OutboxStatus.FAILED.name,
                                lastError = outcome.message
                            )
                        )
                    }
                    if (outcome.offline) {
                        stoppedOffline = true
                        break
                    }
                }
            }
        }

        if (!stoppedOffline) {
            for (pendingOp in plan.remainingOps) {
                val op = byId[pendingOp.id] ?: continue
                dao.update(op.copy(status = OutboxStatus.IN_FLIGHT.name, attempts = op.attempts + 1))
                val outcome = runCatching { executeOutboxOp(api, token, op) }
                    .getOrElse { ApiResult.Err(0, it.message ?: "sync error") }

                when (outcome) {
                    is ApiResult.Ok<*> -> {
                        dao.update(op.copy(status = OutboxStatus.DONE.name, lastError = null))
                        if (outcome.value is BotShiftItem) {
                            persistShiftCache(outcome.value)
                        }
                    }
                    is ApiResult.Err -> {
                        hadFailure = true
                        val retryable = outcome.offline || outcome.code == 0 || outcome.code in 500..599
                        dao.update(
                            op.copy(
                                status = if (retryable) OutboxStatus.PENDING.name else OutboxStatus.FAILED.name,
                                lastError = outcome.message
                            )
                        )
                        if (outcome.offline) {
                            stoppedOffline = true
                            break
                        }
                    }
                }
            }
        }

        dao.clearDone()
        if (!stoppedOffline) {
            refreshShifts()
        } else {
            refreshNotificationFromCache()
        }
        return !hadFailure && !stoppedOffline
    }

    suspend fun openShift(): BotShiftItem? {
        if (networkMonitor.isOnlineNow()) {
            when (val refreshed = refreshShifts()) {
                is ApiResult.Ok -> {
                    return refreshed.value.firstOrNull { it.isOpen }
                        ?: cachedOpenShift() // на случай лага списка после create
                }
                is ApiResult.Err -> Unit
            }
        }
        return cachedOpenShift()
    }

    suspend fun createShift(request: BotShiftUpsertRequest): ApiResult<BotShiftItem> {
        if (!networkMonitor.isOnlineNow()) {
            val localId = "local-" + UUID.randomUUID()
            val provisional = BotShiftItem(
                id = localId,
                date = request.date,
                route = request.routeId,
                shiftType = request.typeOfShiftId,
                typeOfShiftId = request.typeOfShiftId,
                departmentToday = null,
                startTime = request.startTime,
                endTime = null,
                countOfStatements = 0,
                countOfClaims = 0,
                ibdr = 0,
                migrant = 0,
                administrativeViolationsCount = 0,
                criminalViolationsCount = 0,
                partnerServiceInfoId = request.partnerServiceInfoId
            )
            cacheShift(provisional)
            enqueue(
                OutboxOpType.CREATE_SHIFT,
                moshi.adapter(BotShiftUpsertRequest::class.java).toJson(request),
                clientShiftId = localId
            )
            return ApiResult.Ok(provisional)
        }
        val result = safeApiCall { apiFactory.api().createShift(token(), request) }
        if (result is ApiResult.Ok) {
            // Один раз в кэш + уведомление из кэша. Без повторных refresh/reconcile (ANR).
            cacheShift(result.value)
        }
        return result
    }

    suspend fun endShift(shiftId: String, endTime: String): ApiResult<BotShiftItem> {
        val body = BotShiftEndTimeRequest(endTime)
        if (!networkMonitor.isOnlineNow() || shiftId.startsWith("local-")) {
            enqueue(
                OutboxOpType.END_SHIFT,
                """{"shiftId":"$shiftId","endTime":"$endTime"}""",
                clientShiftId = shiftId
            )
            val existing = db.cacheDao().shiftById(shiftId)?.let { shiftAdapter.fromJson(it.json) }
            if (existing != null) {
                val closed = existing.copy(endTime = endTime)
                cacheShift(closed)
                return ApiResult.Ok(closed)
            }
            return ApiResult.Err(0, "Смена сохранена для отправки", offline = true)
        }
        // Сначала сливаем офлайн-дельты/нарушения, иначе закрытие смены «убивает» очередь (409).
        syncOutboxNow()
        val result = safeApiCall { apiFactory.api().endShift(token(), shiftId, body) }
        if (result is ApiResult.Ok) cacheShift(result.value)
        return result
    }

    /** ИБДР ± → только ibdr, endpoint /ibdr. */
    suspend fun adjustIbdr(shiftId: String, delta: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val next = ShiftMetricsLogic.applyIbdrDelta(
            ibdr = current.ibdr,
            migrant = current.migrant,
            delta = delta
        )
        if (next.ibdr == (current.ibdr ?: 0) && next.migrant == (current.migrant ?: 0)) {
            return ApiResult.Ok(current)
        }
        val optimistic = current.copy(ibdr = next.ibdr, migrant = next.migrant)
        return applyMetricDelta(
            shiftId = shiftId,
            optimistic = optimistic,
            opType = OutboxOpType.ADJUST_IBDR,
            delta = delta
        ) { token, id, body -> apiFactory.api().adjustIbdr(token, id, body) }
    }

    /** Мигрант ± → только migrant, endpoint /migrant. */
    suspend fun adjustMigrant(shiftId: String, delta: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val next = ShiftMetricsLogic.applyMigrantDelta(
            ibdr = current.ibdr,
            migrant = current.migrant,
            delta = delta
        )
        if (next.ibdr == (current.ibdr ?: 0) && next.migrant == (current.migrant ?: 0)) {
            return ApiResult.Ok(current)
        }
        val optimistic = current.copy(ibdr = next.ibdr, migrant = next.migrant)
        return applyMetricDelta(
            shiftId = shiftId,
            optimistic = optimistic,
            opType = OutboxOpType.ADJUST_MIGRANT,
            delta = delta
        ) { token, id, body -> apiFactory.api().adjustMigrant(token, id, body) }
    }

    /** Мигрант + ИБДР ± → оба поля, endpoint /migrant-and-ibdr. */
    suspend fun adjustMigrantAndIbdr(shiftId: String, delta: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val next = ShiftMetricsLogic.applyMigrantAndIbdrDelta(
            ibdr = current.ibdr,
            migrant = current.migrant,
            delta = delta
        )
        if (next.ibdr == (current.ibdr ?: 0) && next.migrant == (current.migrant ?: 0)) {
            return ApiResult.Ok(current)
        }
        val optimistic = current.copy(ibdr = next.ibdr, migrant = next.migrant)
        return applyMetricDelta(
            shiftId = shiftId,
            optimistic = optimistic,
            opType = OutboxOpType.ADJUST_MIGRANT_AND_IBDR,
            delta = delta
        ) { token, id, body -> apiFactory.api().adjustMigrantAndIbdr(token, id, body) }
    }

    suspend fun adjustStatements(shiftId: String, delta: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val next = ShiftMetricsLogic.applyDelta(current.countOfStatements, delta)
        if (next == (current.countOfStatements ?: 0)) return ApiResult.Ok(current)
        val optimistic = current.copy(countOfStatements = next)
        return applyMetricDelta(
            shiftId = shiftId,
            optimistic = optimistic,
            opType = OutboxOpType.ADJUST_STATEMENTS,
            delta = delta
        ) { token, id, body -> apiFactory.api().adjustStatements(token, id, body) }
    }

    suspend fun adjustClaims(shiftId: String, delta: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val next = ShiftMetricsLogic.applyDelta(current.countOfClaims, delta)
        if (next == (current.countOfClaims ?: 0)) return ApiResult.Ok(current)
        val optimistic = current.copy(countOfClaims = next)
        return applyMetricDelta(
            shiftId = shiftId,
            optimistic = optimistic,
            opType = OutboxOpType.ADJUST_CLAIMS,
            delta = delta
        ) { token, id, body -> apiFactory.api().adjustClaims(token, id, body) }
    }

    suspend fun createAdminViolation(shiftId: String, impactId: Int, articleId: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val optimistic = current.copy(
            administrativeViolationsCount = (current.administrativeViolationsCount ?: 0) + 1
        )
        cacheShift(optimistic)
        persistLocalMetrics(optimistic)
        val body = BotAdministrativeViolationCreateRequest(impactId, articleId)
        if (!networkMonitor.isOnlineNow() || shiftId.startsWith("local-")) {
            enqueue(
                OutboxOpType.CREATE_ADMIN_VIOLATION,
                moshi.adapter(AdminPayload::class.java).toJson(AdminPayload(shiftId, impactId, articleId)),
                shiftId
            )
            return ApiResult.Ok(optimistic)
        }
        val result = safeApiCall { apiFactory.api().createAdminViolation(token(), shiftId, body) }
        if (result is ApiResult.Ok) {
            cacheShift(result.value)
            persistLocalMetrics(result.value)
        } else if (result is ApiResult.Err && result.offline) {
            enqueue(
                OutboxOpType.CREATE_ADMIN_VIOLATION,
                moshi.adapter(AdminPayload::class.java).toJson(AdminPayload(shiftId, impactId, articleId)),
                shiftId
            )
            return ApiResult.Ok(optimistic)
        }
        return result
    }

    suspend fun createCriminalViolation(shiftId: String, impactId: Int, typeId: Int): ApiResult<BotShiftItem> {
        val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
        val optimistic = current.copy(
            criminalViolationsCount = (current.criminalViolationsCount ?: 0) + 1
        )
        cacheShift(optimistic)
        persistLocalMetrics(optimistic)
        val body = BotCriminalViolationCreateRequest(impactId, typeId)
        if (!networkMonitor.isOnlineNow() || shiftId.startsWith("local-")) {
            enqueue(
                OutboxOpType.CREATE_CRIMINAL_VIOLATION,
                moshi.adapter(CriminalPayload::class.java).toJson(CriminalPayload(shiftId, impactId, typeId)),
                shiftId
            )
            return ApiResult.Ok(optimistic)
        }
        val result = safeApiCall { apiFactory.api().createCriminalViolation(token(), shiftId, body) }
        if (result is ApiResult.Ok) {
            cacheShift(result.value)
            persistLocalMetrics(result.value)
        } else if (result is ApiResult.Err && result.offline) {
            enqueue(
                OutboxOpType.CREATE_CRIMINAL_VIOLATION,
                moshi.adapter(CriminalPayload::class.java).toJson(CriminalPayload(shiftId, impactId, typeId)),
                shiftId
            )
            return ApiResult.Ok(optimistic)
        }
        return result
    }

    suspend fun violationOptions(): ApiResult<BotViolationOptionsResponse> {
        val result = safeApiCall { apiFactory.api().violationOptions(token()) }
        if (result is ApiResult.Ok) {
            cachePrefs.edit()
                .putString(KEY_VIOLATION_OPTIONS, violationOptionsAdapter.toJson(result.value))
                .apply()
            return result
        }
        val cached = cachePrefs.getString(KEY_VIOLATION_OPTIONS, null)
            ?.let { runCatching { violationOptionsAdapter.fromJson(it) }.getOrNull() }
        return if (cached != null) ApiResult.Ok(cached) else result
    }

    suspend fun catalogOptions(): ApiResult<AppCatalog> {
        val result = safeApiCall { apiFactory.api().catalogOptions(token()) }
        if (result is ApiResult.Ok) {
            cachePrefs.edit()
                .putString(KEY_CATALOG_OPTIONS, catalogOptionsAdapter.toJson(result.value))
                .apply()
            return ApiResult.Ok(AppCatalog.fromDto(result.value))
        }
        val cached = cachePrefs.getString(KEY_CATALOG_OPTIONS, null)
            ?.let { runCatching { catalogOptionsAdapter.fromJson(it) }.getOrNull() }
        return if (cached != null) {
            ApiResult.Ok(AppCatalog.fromDto(cached))
        } else {
            ApiResult.Ok(AppCatalog.fallback())
        }
    }

    suspend fun colleagues(department: Int, page: Int = 0, excludeShiftId: String? = null): ApiResult<List<BotColleagueItem>> {
        val result = safeApiCall {
            apiFactory.api().colleagues(token(), department, page, excludeShiftId)
        }
        return when (result) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.items.orEmpty())
            is ApiResult.Err -> result
        }
    }

    suspend fun refreshEvents(): ApiResult<List<BotEventItem>> {
        val result = safeApiCall { apiFactory.api().events(token()) }
        if (result is ApiResult.Ok) {
            db.cacheDao().clearEvents()
            db.cacheDao().upsertEvents(
                result.value.items.orEmpty().mapIndexed { index, e ->
                    CachedEventEntity(
                        id = e.id?.takeIf { it.isNotBlank() } ?: "${e.date}-${e.name}-$index",
                        json = eventAdapter.toJson(e)
                    )
                }
            )
            return ApiResult.Ok(result.value.items.orEmpty())
        }
        val cached = db.cacheDao().events().mapNotNull { runCatching { eventAdapter.fromJson(it.json) }.getOrNull() }
        return if (cached.isNotEmpty()) ApiResult.Ok(cached) else result as ApiResult<List<BotEventItem>>
    }

    suspend fun eventsInRange(from: String, to: String): ApiResult<List<BotEventItem>> {
        if (!networkMonitor.isOnlineNow()) {
            val cached = db.cacheDao().events().mapNotNull { runCatching { eventAdapter.fromJson(it.json) }.getOrNull() }
            val filtered = cached.filter { it.date != null && it.date!! >= from && it.date!! <= to }
            return if (filtered.isNotEmpty()) ApiResult.Ok(filtered)
            else ApiResult.Err(0, "Нет сети", offline = true)
        }
        return when (val result = safeApiCall { apiFactory.api().events(token(), from = from, to = to) }) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.items.orEmpty())
            is ApiResult.Err -> result
        }
    }

    suspend fun createEvent(request: BotEventCreateRequest): ApiResult<BotEventItem> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Создание мероприятия доступно только онлайн", offline = true)
        }
        val result = safeApiCall { apiFactory.api().createEvent(token(), request) }
        if (result is ApiResult.Ok) {
            refreshEvents()
        }
        return result
    }

    suspend fun todayDashboard(): ApiResult<BotTodayDashboardResponse> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Нет сети", offline = true)
        }
        return when (val result = safeApiCall { apiFactory.api().todayDashboard(token()) }) {
            is ApiResult.Ok -> {
                // Старый Core/прокси может отдать HTML (Vaadin) → Moshi null/пустые routes.
                val value = result.value
                if (value.routes == null && value.totals == null && value.operationalDate == null) {
                    ApiResult.Err(502, "Сводка смен недоступна на сервере (нужен Core с /today-dashboard)")
                } else {
                    result
                }
            }
            is ApiResult.Err -> result
        }
    }

    suspend fun dayDashboard(date: String): ApiResult<BotTodayDashboardResponse> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Нет сети", offline = true)
        }
        fun validate(result: ApiResult.Ok<BotTodayDashboardResponse>): ApiResult<BotTodayDashboardResponse> {
            val value = result.value
            return if (value.routes == null && value.totals == null && value.operationalDate == null) {
                ApiResult.Err(502, "Сводка за день недоступна на сервере")
            } else {
                result
            }
        }
        when (val primary = safeApiCall { apiFactory.api().dayDashboard(token(), date) }) {
            is ApiResult.Ok -> return validate(primary)
            is ApiResult.Err -> {
                if (primary.code != 404 && primary.code != 405 && primary.code != 502) {
                    return primary
                }
            }
        }
        return when (val fallback = safeApiCall { apiFactory.api().todayDashboard(token(), date) }) {
            is ApiResult.Ok -> validate(fallback)
            is ApiResult.Err -> fallback
        }
    }

    suspend fun refreshVacations(): ApiResult<BotVacationsResponse> {
        val result = safeApiCall { apiFactory.api().vacations(token()) }
        if (result is ApiResult.Ok) {
            cacheVacations(result.value)
        } else if (result is ApiResult.Err) {
            val cached = db.cacheDao().vacations()?.json?.let { vacationsAdapter.fromJson(it) }
            if (cached != null) return ApiResult.Ok(cached)
        }
        return result
    }

    suspend fun createVacation(request: BotVocationCreateRequest): ApiResult<BotVacationsResponse> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Создание отпуска доступно только онлайн", offline = true)
        }
        val result = safeApiCall { apiFactory.api().createVacation(token(), request) }
        if (result is ApiResult.Ok) cacheVacations(result.value)
        return result
    }

    suspend fun deleteVacation(id: String): ApiResult<BotVacationsResponse> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Удаление отпуска доступно только онлайн", offline = true)
        }
        val result = safeApiCall { apiFactory.api().deleteVacation(token(), id) }
        if (result is ApiResult.Ok) cacheVacations(result.value)
        return result
    }

    suspend fun loadEducation(): ApiResult<BotEducationDto?> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Нет сети", offline = true)
        }
        return safeApiCall {
            val response = apiFactory.api().education(token())
            if (!response.isSuccessful && response.code() != 204) {
                throw retrofit2.HttpException(response)
            }
            EducationResponseMapper.from(response)
        }
    }

    suspend fun upsertEducation(request: BotEducationUpsertRequest): ApiResult<BotEducationDto> {
        if (!networkMonitor.isOnlineNow()) {
            enqueue(
                OutboxOpType.UPDATE_EDUCATION,
                moshi.adapter(BotEducationUpsertRequest::class.java).toJson(request)
            )
            return ApiResult.Err(0, "Сохранено в очередь", offline = true)
        }
        val result = safeApiCall { apiFactory.api().upsertEducation(token(), request) }
        if (result is ApiResult.Err && result.offline) {
            enqueue(
                OutboxOpType.UPDATE_EDUCATION,
                moshi.adapter(BotEducationUpsertRequest::class.java).toJson(request)
            )
        }
        return result
    }

    suspend fun loadVehicles(): ApiResult<List<BotVehicleItem>> {
        if (!networkMonitor.isOnlineNow()) {
            return ApiResult.Err(0, "Нет сети", offline = true)
        }
        return when (val result = safeApiCall { apiFactory.api().vehicles(token()) }) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.items.orEmpty())
            is ApiResult.Err -> result
        }
    }

    suspend fun createVehicle(request: BotVehicleUpsertRequest): ApiResult<BotVehicleItem> {
        if (!networkMonitor.isOnlineNow()) {
            enqueue(
                OutboxOpType.CREATE_VEHICLE,
                moshi.adapter(BotVehicleUpsertRequest::class.java).toJson(request)
            )
            return ApiResult.Err(0, "Сохранено в очередь", offline = true)
        }
        val result = safeApiCall { apiFactory.api().createVehicle(token(), request) }
        if (result is ApiResult.Err && result.offline) {
            enqueue(
                OutboxOpType.CREATE_VEHICLE,
                moshi.adapter(BotVehicleUpsertRequest::class.java).toJson(request)
            )
        }
        return result
    }

    suspend fun updateVehicle(id: String, request: BotVehicleUpsertRequest): ApiResult<BotVehicleItem> {
        val payload = VehicleUpdatePayload(id, request)
        if (!networkMonitor.isOnlineNow()) {
            enqueue(
                OutboxOpType.UPDATE_VEHICLE,
                moshi.adapter(VehicleUpdatePayload::class.java).toJson(payload)
            )
            return ApiResult.Err(0, "Сохранено в очередь", offline = true)
        }
        val result = safeApiCall { apiFactory.api().updateVehicle(token(), id, request) }
        if (result is ApiResult.Err && result.offline) {
            enqueue(
                OutboxOpType.UPDATE_VEHICLE,
                moshi.adapter(VehicleUpdatePayload::class.java).toJson(payload)
            )
        }
        return result
    }

    suspend fun deleteVehicle(id: String): ApiResult<Unit> {
        if (!networkMonitor.isOnlineNow()) {
            enqueue(OutboxOpType.DELETE_VEHICLE, """{"id":"$id"}""")
            return ApiResult.Err(0, "Сохранено в очередь", offline = true)
        }
        val result = safeApiCall { apiFactory.api().deleteVehicle(token(), id) }
        if (result is ApiResult.Err && result.offline) {
            enqueue(OutboxOpType.DELETE_VEHICLE, """{"id":"$id"}""")
        }
        return result
    }

    suspend fun setAbsoluteMetrics(
        shiftId: String,
        ibd: Int?,
        statements: Int?
    ): ApiResult<BotShiftItem> {
        val body = BotShiftUpsertRequest(
            ibdr = ibd,
            countOfStatements = statements
        )
        if (!networkMonitor.isOnlineNow() || shiftId.startsWith("local-")) {
            enqueue(
                OutboxOpType.UPDATE_SHIFT,
                """{"shiftId":"$shiftId","ibdr":$ibd,"countOfStatements":$statements}""",
                shiftId
            )
            val current = requireShift(shiftId) ?: return ApiResult.Err(404, "Смена не найдена")
            val updated = current.copy(
                ibdr = ibd ?: current.ibdr,
                countOfStatements = statements ?: current.countOfStatements
            )
            cacheShift(updated)
            return ApiResult.Ok(updated)
        }
        syncOutboxNow()
        val result = safeApiCall { apiFactory.api().updateShift(token(), shiftId, body) }
        if (result is ApiResult.Ok) cacheShift(result.value)
        return result
    }

    private suspend fun applyMetricDelta(
        shiftId: String,
        optimistic: BotShiftItem,
        opType: OutboxOpType,
        delta: Int,
        apiCall: suspend (token: String, shiftId: String, body: BotShiftMetricDeltaRequest) -> BotShiftItem
    ): ApiResult<BotShiftItem> {
        cacheShift(optimistic)
        persistLocalMetrics(optimistic)
        val payload = """{"shiftId":"$shiftId","delta":$delta}"""

        if (!networkMonitor.isOnlineNow() || shiftId.startsWith("local-")) {
            enqueue(opType, payload, shiftId)
            return ApiResult.Ok(optimistic)
        }
        val result = safeApiCall { apiCall(token(), shiftId, BotShiftMetricDeltaRequest(delta)) }
        if (result is ApiResult.Ok) {
            cacheShift(result.value)
            persistLocalMetrics(result.value)
            return result
        }
        // Ошибка/таймаут: в очередь. Flush шлёт абсолют из local_metrics (не реплей ±1),
        // поэтому повтор после уже принятого на Core запроса не накручивает счётчик.
        if (result is ApiResult.Err) {
            enqueue(opType, payload, shiftId)
            return ApiResult.Ok(optimistic)
        }
        return result
    }

    private suspend fun requireShift(shiftId: String): BotShiftItem? {
        db.cacheDao().shiftById(shiftId)?.let {
            return applyLocalOverlay(shiftAdapter.fromJson(it.json)!!)
        }
        val remote = safeApiCall { apiFactory.api().shift(token(), shiftId) }
        return (remote as? ApiResult.Ok)?.value?.also { cacheShift(it) }
    }

    private suspend fun applyLocalOverlay(shift: BotShiftItem): BotShiftItem {
        val local = db.cacheDao().localMetrics(shift.id) ?: return shift
        return shift.copy(
            migrant = local.migrant,
            ibdr = local.ibdr,
            countOfStatements = local.statements,
            countOfClaims = local.claims,
            administrativeViolationsCount = local.adminCount,
            criminalViolationsCount = local.criminalCount
        )
    }

    private suspend fun persistLocalMetrics(shift: BotShiftItem) {
        db.cacheDao().upsertLocalMetrics(
            LocalMetricsEntity(
                shiftId = shift.id,
                migrant = shift.migrant ?: 0,
                ibdr = shift.ibdr ?: 0,
                statements = shift.countOfStatements ?: 0,
                claims = shift.countOfClaims ?: 0,
                adminCount = shift.administrativeViolationsCount ?: 0,
                criminalCount = shift.criminalViolationsCount ?: 0,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun cacheVacations(value: BotVacationsResponse) {
        db.cacheDao().upsertVacations(
            CachedVacationsEntity(
                json = vacationsAdapter.toJson(value),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun persistShiftCache(shift: BotShiftItem) {
        db.cacheDao().upsertShift(
            CachedShiftEntity(
                id = shift.id,
                json = shiftAdapter.toJson(shift),
                updatedAt = System.currentTimeMillis(),
                isOpen = shift.isOpen
            )
        )
        persistLocalMetrics(shift)
    }

    private suspend fun cacheShift(shift: BotShiftItem) {
        persistShiftCache(shift)
        if (session.isLoggedIn) {
            // Никогда не вызывать openShift() отсюда — там refresh и была рекурсия/ANR.
            refreshNotificationFromCache()
        }
    }

    private suspend fun enqueue(type: OutboxOpType, payloadJson: String, clientShiftId: String? = null) {
        db.outboxDao().insert(
            OutboxOpEntity(
                id = UUID.randomUUID().toString(),
                type = type.name,
                payloadJson = payloadJson,
                createdAt = System.currentTimeMillis(),
                status = OutboxStatus.PENDING.name,
                clientShiftId = clientShiftId
            )
        )
        // Всегда ставим WorkManager (на случай, если прямой sync сейчас недоступен).
        SyncScheduler.enqueueImmediate(appContext)
        if (networkMonitor.isOnlineNow()) {
            runCatching { syncOutboxNow() }
        }
    }

    private suspend fun executeOutboxOp(
        api: com.company.shtopor.vzvod.data.remote.BotApi,
        token: String,
        op: OutboxOpEntity
    ): ApiResult<*> {
        return when (OutboxOpType.valueOf(op.type)) {
            OutboxOpType.CREATE_SHIFT -> {
                val body = moshi.adapter(BotShiftUpsertRequest::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.createShift(token, body) }
            }
            OutboxOpType.UPDATE_SHIFT -> {
                val json = JSONObject(op.payloadJson)
                val shiftId = json.getString("shiftId")
                val body = BotShiftUpsertRequest(
                    ibdr = if (json.has("ibdr") && !json.isNull("ibdr")) json.getInt("ibdr") else null,
                    migrant = if (json.has("migrant") && !json.isNull("migrant")) {
                        json.getInt("migrant")
                    } else null,
                    countOfStatements = if (json.has("countOfStatements") && !json.isNull("countOfStatements")) {
                        json.getInt("countOfStatements")
                    } else null
                )
                safeApiCall { api.updateShift(token, shiftId, body) }
            }
            OutboxOpType.END_SHIFT -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.endShift(
                        token,
                        json.getString("shiftId"),
                        BotShiftEndTimeRequest(json.getString("endTime"))
                    )
                }
            }
            OutboxOpType.ADJUST_IBDR -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.adjustIbdr(
                        token,
                        json.getString("shiftId"),
                        BotShiftMetricDeltaRequest(json.getInt("delta"))
                    )
                }
            }
            OutboxOpType.ADJUST_MIGRANT -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.adjustMigrant(
                        token,
                        json.getString("shiftId"),
                        BotShiftMetricDeltaRequest(json.getInt("delta"))
                    )
                }
            }
            OutboxOpType.ADJUST_MIGRANT_AND_IBDR -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.adjustMigrantAndIbdr(
                        token,
                        json.getString("shiftId"),
                        BotShiftMetricDeltaRequest(json.getInt("delta"))
                    )
                }
            }
            OutboxOpType.ADJUST_STATEMENTS -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.adjustStatements(
                        token,
                        json.getString("shiftId"),
                        BotShiftMetricDeltaRequest(json.getInt("delta"))
                    )
                }
            }
            OutboxOpType.ADJUST_CLAIMS -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall {
                    api.adjustClaims(
                        token,
                        json.getString("shiftId"),
                        BotShiftMetricDeltaRequest(json.getInt("delta"))
                    )
                }
            }
            OutboxOpType.CREATE_ADMIN_VIOLATION -> {
                val payload = moshi.adapter(AdminPayload::class.java).fromJson(op.payloadJson)!!
                safeApiCall {
                    api.createAdminViolation(
                        token,
                        payload.shiftId,
                        BotAdministrativeViolationCreateRequest(payload.impactId, payload.articleId)
                    )
                }
            }
            OutboxOpType.CREATE_CRIMINAL_VIOLATION -> {
                val payload = moshi.adapter(CriminalPayload::class.java).fromJson(op.payloadJson)!!
                safeApiCall {
                    api.createCriminalViolation(
                        token,
                        payload.shiftId,
                        BotCriminalViolationCreateRequest(payload.impactId, payload.typeId)
                    )
                }
            }
            OutboxOpType.UPDATE_PROFILE -> {
                val body = moshi.adapter(BotProfilePatchRequest::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.updateProfile(token, body) }
            }
            OutboxOpType.CREATE_VOCATION -> {
                val body = moshi.adapter(BotVocationCreateRequest::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.createVacation(token, body) }
            }
            OutboxOpType.DELETE_VOCATION -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall { api.deleteVacation(token, json.getString("id")) }
            }
            OutboxOpType.UPDATE_EDUCATION -> {
                val body = moshi.adapter(BotEducationUpsertRequest::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.upsertEducation(token, body) }
            }
            OutboxOpType.CREATE_VEHICLE -> {
                val body = moshi.adapter(BotVehicleUpsertRequest::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.createVehicle(token, body) }
            }
            OutboxOpType.UPDATE_VEHICLE -> {
                val payload = moshi.adapter(VehicleUpdatePayload::class.java).fromJson(op.payloadJson)!!
                safeApiCall { api.updateVehicle(token, payload.id, payload.request) }
            }
            OutboxOpType.DELETE_VEHICLE -> {
                val json = JSONObject(op.payloadJson)
                safeApiCall { api.deleteVehicle(token, json.getString("id")) }
            }
        }
    }

    data class AdminPayload(val shiftId: String, val impactId: Int, val articleId: Int)
    data class CriminalPayload(val shiftId: String, val impactId: Int, val typeId: Int)
    data class VehicleUpdatePayload(val id: String, val request: BotVehicleUpsertRequest)
}
