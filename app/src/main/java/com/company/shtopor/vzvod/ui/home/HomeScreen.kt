package com.company.shtopor.vzvod.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.company.shtopor.vzvod.data.local.PetrovichBaselineStore
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRouteItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.CheckingRouteButtonLabel
import com.company.shtopor.vzvod.domain.CheckingRouteCounters
import com.company.shtopor.vzvod.domain.CheckingShiftPolicy
import com.company.shtopor.vzvod.domain.PetrovichMetrics
import com.company.shtopor.vzvod.domain.PetrovichPlayback
import com.company.shtopor.vzvod.domain.PetrovichStageLogic
import com.company.shtopor.vzvod.domain.ShiftMetricsLogic
import com.company.shtopor.vzvod.ui.components.CompactMetricRow
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val SHIFTS_PANEL_ALPHA = 0.88f
/** Прозрачность фона 40% → alpha = 0.4 */
private const val PETROVICH_ALPHA = 0.4f

@Composable
fun ShiftsScreen(
    container: AppContainer,
    online: Boolean,
    modifier: Modifier = Modifier,
    onOpenCreateShift: () -> Unit,
    onOpenEndShift: (BotShiftItem) -> Unit,
    onOpenAdmin: (String) -> Unit,
    onOpenCriminal: (String) -> Unit
) {
    var pending by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf<BotShiftItem?>(null) }
    var dashboard by remember { mutableStateOf<BotTodayDashboardResponse?>(null) }
    var dashboardError by remember { mutableStateOf<String?>(null) }
    var checkableRoutes by remember { mutableStateOf<List<BotCheckableRouteItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var petrovich by remember { mutableStateOf<PetrovichPlayback>(PetrovichPlayback.Loop(1)) }
    var petrovichReady by remember { mutableStateOf(false) }
    var petrovichShiftId by remember { mutableStateOf<String?>(null) }
    val baselineStore = remember { PetrovichBaselineStore(container.appContext) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            when (val r = container.repository.refreshShifts()) {
                is ApiResult.Ok -> {
                    open = r.value.firstOrNull { it.isOpen }
                        ?: container.repository.getCachedOpenShift()
                }
                is ApiResult.Err -> {
                    if (open == null) error = r.userMessage()
                    open = container.repository.getCachedOpenShift()
                }
            }
            when (val d = container.repository.todayDashboard()) {
                is ApiResult.Ok -> {
                    dashboard = d.value
                    dashboardError = null
                }
                is ApiResult.Err -> {
                    dashboard = null
                    if (online) dashboardError = d.userMessage()
                }
            }
            val checkingShift = open
            if (checkingShift != null && CheckingShiftPolicy.showRouteCheckCounters(checkingShift.typeOfShiftId)) {
                when (val r = container.repository.loadCheckableRoutes(checkingShift.id)) {
                    is ApiResult.Ok -> checkableRoutes = r.value.routes.orEmpty()
                    is ApiResult.Err -> checkableRoutes =
                        container.repository.cachedCheckableRoutes(checkingShift.id)
                }
            } else {
                checkableRoutes = emptyList()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        container.repository.pendingOpsFlow().collectLatest { pending = it }
    }

    LaunchedEffect(online) {
        if (online) {
            delay(700)
            reload()
        }
    }

    val current = open
    LaunchedEffect(current?.id, current?.typeOfShiftId, online) {
        if (current == null || !CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId)) {
            return@LaunchedEffect
        }
        while (true) {
            when (val r = container.repository.loadCheckableRoutes(current.id)) {
                is ApiResult.Ok -> checkableRoutes = r.value.routes.orEmpty()
                is ApiResult.Err -> checkableRoutes =
                    container.repository.cachedCheckableRoutes(current.id)
            }
            delay(20_000)
        }
    }
    fun metricsOf(shift: BotShiftItem) = PetrovichMetrics.of(
        ibdr = shift.ibdr,
        admin = shift.administrativeViolationsCount,
        criminal = shift.criminalViolationsCount
    )

    fun evaluateCurrent(): Pair<Int, PetrovichMetrics?> {
        val shift = current ?: return 1 to null
        val stored = baselineStore.load(shift.id)
        val evaluation = PetrovichStageLogic.evaluate(metricsOf(shift), stored)
        when {
            evaluation.stage2Baseline == null -> baselineStore.clear(shift.id)
            stored == null || stored != evaluation.stage2Baseline ->
                baselineStore.save(shift.id, evaluation.stage2Baseline)
        }
        return evaluation.stage to evaluation.stage2Baseline
    }

    LaunchedEffect(
        current?.id,
        current?.ibdr,
        current?.administrativeViolationsCount,
        current?.criminalViolationsCount
    ) {
        if (current == null) {
            petrovichShiftId = null
            petrovich = PetrovichPlayback.Loop(1)
            petrovichReady = false
            return@LaunchedEffect
        }
        val shiftChanged = petrovichShiftId != current.id
        if (shiftChanged) {
            petrovichShiftId = current.id
            petrovichReady = false
        }
        val (target, _) = evaluateCurrent()
        if (!petrovichReady || shiftChanged) {
            // Возврат в приложение / новая смена: сразу нужный loop без *_change.
            petrovich = PetrovichStageLogic.initialPlayback(target)
            petrovichReady = true
        } else {
            petrovich = PetrovichStageLogic.resolve(petrovich, target)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PetrovichGifBackground(
            playback = petrovich,
            onLoopCycleEnded = {
                petrovich = PetrovichStageLogic.onLoopCycleEnded(petrovich)
            },
            onChangeEnded = {
                val afterChange = PetrovichStageLogic.onChangeEnded(petrovich)
                val (target, _) = evaluateCurrent()
                petrovich = PetrovichStageLogic.resolve(afterChange, target)
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(PETROVICH_ALPHA)
        )

        PageScaffold(
            online = online,
            pendingCount = pending,
            clearBackground = true,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionTitle(
                    title = "Смены",
                    subtitle = if (online) null else "Офлайн: изменения сохраняются на устройстве"
                )

                if (current == null) {
                    SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Нет открытой смены", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Создайте смену — после старта появятся компактные счётчики.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                            PrimaryActionButton("Новая смена", onClick = onOpenCreateShift)
                        }
                    }
                } else {
                    SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Текущая смена", style = MaterialTheme.typography.titleLarge)
                            Text(
                                listOfNotNull(
                                    current.date,
                                    current.route,
                                    current.shiftType?.takeIf { it.isNotBlank() }
                                        ?: current.typeOfShiftId,
                                    current.startTime?.let { "с $it" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                if (CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId)) {
                                    CheckingRouteCounters.formatSummary(
                                        routeCount = checkableRoutes.size,
                                        checkCount = checkableRoutes.sumOf { it.checks.orEmpty().size },
                                        adminCount = current.administrativeViolationsCount,
                                        criminalCount = current.criminalViolationsCount
                                    )
                                } else {
                                    ShiftMetricsLogic.formatSummary(
                                        current.ibdr,
                                        current.migrant,
                                        current.countOfStatements,
                                        current.countOfClaims,
                                        current.administrativeViolationsCount,
                                        current.criminalViolationsCount
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }

                    if (CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId)) {
                        if (checkableRoutes.isEmpty()) {
                            SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA) {
                                Text(
                                    "Маршрутов сегодня нет",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted
                                )
                            }
                        } else {
                            checkableRoutes.forEach { route ->
                                CompactMetricRow(
                                    label = CheckingRouteButtonLabel.format(
                                        route.routeLabel?.takeIf { it.isNotBlank() } ?: route.routeId,
                                        route.checks.orEmpty().map { it.checkedAt }
                                    ),
                                    value = route.checks.orEmpty().size,
                                    surfaceAlpha = SHIFTS_PANEL_ALPHA,
                                    onInc = {
                                        scope.launch {
                                            when (val r = container.repository.recordRouteCheck(
                                                current.id,
                                                route.routeId
                                            )) {
                                                is ApiResult.Ok -> checkableRoutes =
                                                    container.repository.cachedCheckableRoutes(current.id)
                                                is ApiResult.Err -> error = r.userMessage()
                                            }
                                        }
                                    },
                                    onDec = {
                                        val checkId = CheckingRouteCounters.lastCheckId(
                                            route.checks.orEmpty().map { it.id }
                                        )
                                        if (checkId != null) {
                                            scope.launch {
                                                when (val r = container.repository.deleteRouteCheck(
                                                    current.id,
                                                    checkId
                                                )) {
                                                    is ApiResult.Ok -> checkableRoutes =
                                                        container.repository.cachedCheckableRoutes(current.id)
                                                    is ApiResult.Err -> error = r.userMessage()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (CheckingShiftPolicy.showMetricCounters(current.typeOfShiftId)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactMetricRow(
                            label = "ИБДР",
                            value = current.ibdr ?: 0,
                            surfaceAlpha = SHIFTS_PANEL_ALPHA,
                            modifier = Modifier.weight(1f),
                            onInc = {
                                scope.launch {
                                    when (val r = container.repository.adjustIbdr(current.id, 1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            },
                            onDec = {
                                scope.launch {
                                    when (val r = container.repository.adjustIbdr(current.id, -1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            }
                        )
                        CompactMetricRow(
                            label = "Мигрант",
                            value = current.migrant ?: 0,
                            surfaceAlpha = SHIFTS_PANEL_ALPHA,
                            modifier = Modifier.weight(1f),
                            onInc = {
                                scope.launch {
                                    when (val r = container.repository.adjustMigrant(current.id, 1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            },
                            onDec = {
                                scope.launch {
                                    when (val r = container.repository.adjustMigrant(current.id, -1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            }
                        )
                    }

                    CompactMetricRow(
                        label = "Мигрант + ИБДР",
                        value = minOf(current.ibdr ?: 0, current.migrant ?: 0),
                        surfaceAlpha = SHIFTS_PANEL_ALPHA,
                        onInc = {
                            scope.launch {
                                when (val r = container.repository.adjustMigrantAndIbdr(current.id, 1)) {
                                    is ApiResult.Ok -> open = r.value
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        },
                        onDec = {
                            scope.launch {
                                when (val r = container.repository.adjustMigrantAndIbdr(current.id, -1)) {
                                    is ApiResult.Ok -> open = r.value
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactMetricRow(
                            label = "Заявки",
                            value = current.countOfStatements ?: 0,
                            surfaceAlpha = SHIFTS_PANEL_ALPHA,
                            modifier = Modifier.weight(1f),
                            onInc = {
                                scope.launch {
                                    when (val r = container.repository.adjustStatements(current.id, 1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            },
                            onDec = {
                                scope.launch {
                                    when (val r = container.repository.adjustStatements(current.id, -1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            }
                        )
                        CompactMetricRow(
                            label = "Жалобы на наряд",
                            value = current.countOfClaims ?: 0,
                            surfaceAlpha = SHIFTS_PANEL_ALPHA,
                            modifier = Modifier.weight(1f),
                            onInc = {
                                scope.launch {
                                    when (val r = container.repository.adjustClaims(current.id, 1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            },
                            onDec = {
                                scope.launch {
                                    when (val r = container.repository.adjustClaims(current.id, -1)) {
                                        is ApiResult.Ok -> open = r.value
                                        is ApiResult.Err -> error = r.userMessage()
                                    }
                                }
                            }
                        )
                    }
                    }

                    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryActionButton(
                            "Админка ${current.administrativeViolationsCount ?: 0}",
                            onClick = { onOpenAdmin(current.id) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            "Уголовка ${current.criminalViolationsCount ?: 0}",
                            onClick = { onOpenCriminal(current.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PrimaryActionButton("Завершить смену", onClick = { onOpenEndShift(current) })
                }

                when {
                    dashboard != null -> TodayDashboardPanel(dashboard!!)
                    !dashboardError.isNullOrBlank() -> SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Сводка за сегодня", style = MaterialTheme.typography.titleMedium)
                            Text(dashboardError.orEmpty(), color = InkMuted)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    error?.let {
        ErrorRetryDialog(
            message = it,
            onRetry = { error = null; reload() },
            onDismiss = { error = null }
        )
    }
}

/** @deprecated use ShiftsScreen */
@Composable
fun HomeScreen(
    container: AppContainer,
    online: Boolean,
    onOpenCreateShift: () -> Unit,
    onOpenEndShift: (BotShiftItem) -> Unit,
    onOpenAdmin: (String) -> Unit,
    onOpenCriminal: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenCommander: () -> Unit
) {
    ShiftsScreen(
        container = container,
        online = online,
        onOpenCreateShift = onOpenCreateShift,
        onOpenEndShift = onOpenEndShift,
        onOpenAdmin = onOpenAdmin,
        onOpenCriminal = onOpenCriminal
    )
}
