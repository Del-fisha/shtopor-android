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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.company.shtopor.vzvod.data.local.PetrovichBaselineStore
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRouteItem
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckItem
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckerOption
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.CheckingRouteButtonLabel
import com.company.shtopor.vzvod.domain.ProfileDisplayFormat
import com.company.shtopor.vzvod.domain.RouteCheckEditLogic
import com.company.shtopor.vzvod.domain.CheckingRouteCounters
import com.company.shtopor.vzvod.domain.CheckingShiftPolicy
import com.company.shtopor.vzvod.domain.PetrovichMetrics
import com.company.shtopor.vzvod.domain.PetrovichPlayback
import com.company.shtopor.vzvod.domain.PetrovichStageLogic
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
    var routeCommanders by remember { mutableStateOf<List<BotRouteCheckerOption>>(emptyList()) }
    var editingCheck by remember { mutableStateOf<Pair<String, BotRouteCheckItem>?>(null) }
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
                    is ApiResult.Ok -> {
                        checkableRoutes = r.value.routes.orEmpty()
                        routeCommanders = r.value.commanders.orEmpty()
                    }
                    is ApiResult.Err -> {
                        checkableRoutes = container.repository.cachedCheckableRoutes(checkingShift.id)
                        routeCommanders = container.repository.cachedRouteCommanders(checkingShift.id)
                    }
                }
            } else {
                checkableRoutes = emptyList()
                routeCommanders = emptyList()
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
                    val visibleRoutes = checkableRoutes.filter { route ->
                        !container.repository.role().isCommander || CheckingRouteCounters.commanderSeesRoute(
                            route.routeLabel?.takeIf { it.isNotBlank() } ?: route.routeId
                        )
                    }
                    val checkCount = if (CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId)) {
                        visibleRoutes.sumOf { it.checks.orEmpty().size }
                    } else {
                        dashboard?.routeChecks.orEmpty()
                            .filter { it.routeLabel == current.route }
                            .sumOf { it.checks.orEmpty().size }
                    }
                    val routeType = current.shiftType?.takeIf { it.isNotBlank() } ?: current.typeOfShiftId.orEmpty()
                    val routeNumber = current.route?.takeIf { it.isNotBlank() }
                    SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA, contentPadding = 12.dp, corner = 12.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Текущая смена", style = MaterialTheme.typography.titleMedium)
                            Text(ProfileDisplayFormat.shortDate(current.date), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    routeType.isNotBlank() && routeNumber != null -> "$routeType ($routeNumber)"
                                    routeType.isNotBlank() -> routeType
                                    routeNumber != null -> routeNumber
                                    else -> "—"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Начало: ${CheckingRouteButtonLabel.toHm(current.startTime) ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Ибдр : ${current.ibdr ?: 0} | АП : ${current.administrativeViolationsCount ?: 0} | УП : ${current.criminalViolationsCount ?: 0} | Проверок : $checkCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                    }

                    if (CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId) && visibleRoutes.isEmpty()) {
                        SoftPanel(surfaceAlpha = SHIFTS_PANEL_ALPHA, contentPadding = 12.dp, corner = 12.dp) {
                            Text(
                                "Маршрутов сегодня нет",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }

                    val boards = buildStepperCells(
                        container = container,
                        current = current,
                        commander = container.repository.role().isCommander,
                        checkableRoutes = visibleRoutes,
                        onRouteInc = { routeId ->
                            scope.launch {
                                when (val r = container.repository.recordRouteCheck(current.id, routeId)) {
                                    is ApiResult.Ok -> {
                                        checkableRoutes = container.repository.cachedCheckableRoutes(current.id)
                                        routeCommanders = container.repository.cachedRouteCommanders(current.id)
                                    }
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        },
                        onRouteDec = { _, checkId ->
                            scope.launch {
                                when (val r = container.repository.deleteRouteCheck(current.id, checkId)) {
                                    is ApiResult.Ok -> {
                                        checkableRoutes = container.repository.cachedCheckableRoutes(current.id)
                                        routeCommanders = container.repository.cachedRouteCommanders(current.id)
                                    }
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        },
                        onEditCheck = { label, check -> editingCheck = label to check },
                        onMetric = { adjust ->
                            scope.launch {
                                when (val r = adjust()) {
                                    is ApiResult.Ok -> open = r.value
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        }
                    )
                    if (boards.checks.isNotEmpty()) {
                        StepperGrid(boards.checks, tailHeight = 56.dp)
                    }
                    if (boards.checks.isNotEmpty() && boards.metrics.isNotEmpty()) {
                        HorizontalDivider(color = InkMuted.copy(alpha = 0.45f))
                    }
                    if (boards.metrics.isNotEmpty()) {
                        StepperGrid(boards.metrics)
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

    val editTarget = editingCheck
    val editShiftId = open?.id
    if (editTarget != null && editShiftId != null) {
        val (routeLabel, check) = editTarget
        val checkId = check.id
        if (!checkId.isNullOrBlank()) {
            RouteCheckEditDialog(
                routeLabel = routeLabel,
                check = check,
                commanders = routeCommanders,
                onDismiss = { editingCheck = null },
                onSave = { time, checkerId ->
                    scope.launch {
                        when (val r = container.repository.updateRouteCheck(
                            editShiftId,
                            checkId,
                            time,
                            checkerId
                        )) {
                            is ApiResult.Ok -> {
                                checkableRoutes = container.repository.cachedCheckableRoutes(editShiftId)
                                routeCommanders = container.repository.cachedRouteCommanders(editShiftId)
                                editingCheck = null
                            }
                            is ApiResult.Err -> error = r.userMessage()
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        when (val r = container.repository.deleteRouteCheck(editShiftId, checkId)) {
                            is ApiResult.Ok -> {
                                checkableRoutes = container.repository.cachedCheckableRoutes(editShiftId)
                                routeCommanders = container.repository.cachedRouteCommanders(editShiftId)
                                editingCheck = null
                            }
                            is ApiResult.Err -> error = r.userMessage()
                        }
                    }
                }
            )
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

@Composable
private fun StepperGrid(cells: List<StepperCell>, tailHeight: androidx.compose.ui.unit.Dp = 0.dp) {
    cells.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pair.forEach { cell ->
                CompactMetricRow(
                    label = cell.label,
                    value = cell.value,
                    surfaceAlpha = SHIFTS_PANEL_ALPHA,
                    modifier = Modifier.weight(1f),
                    onInc = cell.onInc,
                    onDec = cell.onDec,
                    tailHeight = if (cell.below != null || tailHeight > 0.dp) tailHeight else 0.dp,
                    below = cell.below
                )
            }
            if (pair.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class StepperBoards(
    val checks: List<StepperCell>,
    val metrics: List<StepperCell>
)

private data class StepperCell(
    val label: String,
    val value: Int,
    val onInc: () -> Unit,
    val onDec: () -> Unit,
    val below: (@Composable () -> Unit)? = null
)

@Composable
private fun buildStepperCells(
    container: AppContainer,
    current: BotShiftItem,
    commander: Boolean,
    checkableRoutes: List<BotCheckableRouteItem>,
    onRouteInc: (String) -> Unit,
    onRouteDec: (String, String) -> Unit,
    onEditCheck: (String, BotRouteCheckItem) -> Unit,
    onMetric: (suspend () -> ApiResult<BotShiftItem>) -> Unit
): StepperBoards {
    val checks = mutableListOf<StepperCell>()
    val metrics = mutableListOf<StepperCell>()
    if (CheckingShiftPolicy.showRouteCheckCounters(current.typeOfShiftId)) {
        checkableRoutes.forEach { route ->
            val routeName = route.routeLabel?.takeIf { it.isNotBlank() } ?: route.routeId
            if (commander && !CheckingRouteCounters.commanderSeesRoute(routeName)) return@forEach
            val routeChecks = route.checks.orEmpty()
            checks += StepperCell(
                label = routeName,
                value = routeChecks.size,
                onInc = { onRouteInc(route.routeId) },
                onDec = {
                    val checkId = CheckingRouteCounters.lastCheckId(routeChecks.map { it.id })
                    if (checkId != null) onRouteDec(route.routeId, checkId)
                },
                below = if (routeChecks.isEmpty()) null else {
                    {
                        Column {
                            routeChecks.forEach { check ->
                                val checkId = check.id
                                if (checkId.isNullOrBlank()) return@forEach
                                TextButton(
                                    onClick = { onEditCheck(routeName, check) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(22.dp).fillMaxWidth()
                                ) {
                                    Text(
                                        text = buildString {
                                            append(RouteCheckEditLogic.displayTime(check.checkedAt))
                                            check.checkerLabel?.takeIf { it.isNotBlank() }?.let {
                                                append(" ")
                                                append(ProfileDisplayFormat.compactFio(it))
                                            }
                                        },
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
    val showPatrolMetrics = CheckingShiftPolicy.showMetricCounters(current.typeOfShiftId)
    val showCommanderMetrics = commander && CheckingShiftPolicy.isChecking(current.typeOfShiftId)
    if (showPatrolMetrics || showCommanderMetrics) {
        metrics += StepperCell(
            label = "ИБДР",
            value = current.ibdr ?: 0,
            onInc = { onMetric { container.repository.adjustIbdr(current.id, 1) } },
            onDec = { onMetric { container.repository.adjustIbdr(current.id, -1) } }
        )
        metrics += StepperCell(
            label = "Мигрант",
            value = current.migrant ?: 0,
            onInc = { onMetric { container.repository.adjustMigrant(current.id, 1) } },
            onDec = { onMetric { container.repository.adjustMigrant(current.id, -1) } }
        )
        metrics += StepperCell(
            label = "Мигрант + ИБДР",
            value = minOf(current.ibdr ?: 0, current.migrant ?: 0),
            onInc = { onMetric { container.repository.adjustMigrantAndIbdr(current.id, 1) } },
            onDec = { onMetric { container.repository.adjustMigrantAndIbdr(current.id, -1) } }
        )
    }
    if (showPatrolMetrics) {
        metrics += StepperCell(
            label = "Заявки",
            value = current.countOfStatements ?: 0,
            onInc = { onMetric { container.repository.adjustStatements(current.id, 1) } },
            onDec = { onMetric { container.repository.adjustStatements(current.id, -1) } }
        )
        metrics += StepperCell(
            label = "Жалобы на наряд",
            value = current.countOfClaims ?: 0,
            onInc = { onMetric { container.repository.adjustClaims(current.id, 1) } },
            onDec = { onMetric { container.repository.adjustClaims(current.id, -1) } }
        )
    }
    return StepperBoards(checks, metrics)
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
