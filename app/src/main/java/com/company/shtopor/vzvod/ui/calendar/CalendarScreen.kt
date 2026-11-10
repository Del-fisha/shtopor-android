package com.company.shtopor.vzvod.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotEventItem
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.CalendarMonthLogic
import com.company.shtopor.vzvod.domain.CalendarMonthSwipe
import com.company.shtopor.vzvod.domain.OperationalDay
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.events.EventsScreen
import com.company.shtopor.vzvod.ui.events.formatEventDate
import com.company.shtopor.vzvod.ui.home.TodayDashboardPanel
import com.company.shtopor.vzvod.ui.theme.Ink
import com.company.shtopor.vzvod.ui.theme.InkMuted
import com.company.shtopor.vzvod.ui.theme.MistDeep
import com.company.shtopor.vzvod.ui.theme.Steel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CalendarInnerTab { SHIFTS, EVENTS }

private val Dep1Bg = Color(0xFFD6E2EA)
private val Dep2Bg = Color(0xFFCBD9D6)
private val EventDot = Color(0xFFC62828)
private val Weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun CalendarScreen(
    container: AppContainer,
    online: Boolean,
    modifier: Modifier = Modifier,
    onOpenCreateEvent: (() -> Unit)? = null
) {
    var pending by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf(CalendarInnerTab.SHIFTS) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.repository.pendingOpsFlow().collectLatest { pending = it }
    }

    PageScaffold(online = online, pendingCount = pending, modifier = modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("Календарь", "СМЕНЫ И МЕРОПРИЯТИЯ")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tab == CalendarInnerTab.SHIFTS,
                    onClick = { tab = CalendarInnerTab.SHIFTS },
                    label = { Text("Смены") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = tab == CalendarInnerTab.EVENTS,
                    onClick = { tab = CalendarInnerTab.EVENTS },
                    label = { Text("Мероприятия") },
                    modifier = Modifier.weight(1f)
                )
            }

            when (tab) {
                CalendarInnerTab.SHIFTS -> MonthShiftsCalendar(
                    container = container,
                    onError = { error = it }
                )
                CalendarInnerTab.EVENTS -> EventsScreen(
                    container = container,
                    online = online,
                    onOpenCreateEvent = onOpenCreateEvent,
                    embedInParent = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    error?.let {
        ErrorRetryDialog(
            it,
            onRetry = { error = null },
            onDismiss = { error = null }
        )
    }
}

@Composable
private fun MonthShiftsCalendar(
    container: AppContainer,
    onError: (String) -> Unit
) {
    val today = remember { OperationalDay.today() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var slideDir by remember { mutableStateOf(1) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var monthEvents by remember { mutableStateOf<List<BotEventItem>>(emptyList()) }
    var dayDashboard by remember { mutableStateOf<BotTodayDashboardResponse?>(null) }
    var dayLoading by remember { mutableStateOf(false) }
    var dayError by remember { mutableStateOf<String?>(null) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val monthTitle = remember(month) {
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
    }

    fun loadMonthEvents(ym: YearMonth) {
        scope.launch {
            val from = ym.atDay(1).toString()
            val to = ym.atEndOfMonth().toString()
            when (val r = container.repository.eventsInRange(from, to)) {
                is ApiResult.Ok -> monthEvents = r.value
                is ApiResult.Err -> {
                    monthEvents = emptyList()
                    if (!r.offline) onError(r.userMessage())
                }
            }
        }
    }

    fun shiftMonth(delta: Int) {
        if (delta == 0) return
        slideDir = delta
        month = month.plusMonths(delta.toLong())
    }

    LaunchedEffect(month) {
        selected = null
        dayDashboard = null
        dayError = null
        dayLoading = false
        loadMonthEvents(month)
    }

    LaunchedEffect(selected) {
        val day = selected
        dayDashboard = null
        dayError = null
        if (day != null && CalendarMonthLogic.canShowShiftSummary(day, today)) {
            dayLoading = true
            when (val r = container.repository.dayDashboard(day.toString())) {
                is ApiResult.Ok -> {
                    dayDashboard = r.value
                    dayError = null
                }
                is ApiResult.Err -> {
                    dayError = r.userMessage()
                    if (!r.offline) onError(r.userMessage())
                }
            }
            dayLoading = false
        } else {
            dayLoading = false
        }
    }

    val eventDays = remember(monthEvents) {
        CalendarMonthLogic.eventDatesOn(monthEvents.map { it.date })
    }
    val cells = remember(month) { CalendarMonthLogic.cells(month) }
    val selectedEvents = remember(selected, monthEvents) {
        val day = selected?.toString() ?: return@remember emptyList()
        monthEvents.filter { it.date == day }
    }
    val detailOpen = selected != null

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        shiftMonth(CalendarMonthSwipe.monthDeltaForDrag(dragAccum))
                        dragAccum = 0f
                    },
                    onHorizontalDrag = { _, amount ->
                        dragAccum += amount
                    }
                )
            }
    ) {
        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Weekdays.forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !detailOpen)
        ) {
            val rows = (cells.size / 7).coerceAtLeast(1)
            val cellH = maxHeight / rows
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (slideDir >= 0) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "monthSlide"
            ) { animatedMonth ->
                val animatedCells = CalendarMonthLogic.cells(animatedMonth)
                Column(Modifier.fillMaxSize()) {
                    animatedCells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellH),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            week.forEach { cell ->
                                val bg = when {
                                    !cell.inMonth -> MistDeep.copy(alpha = 0.35f)
                                    cell.department == 1 -> Dep1Bg
                                    else -> Dep2Bg
                                }
                                val isSelected = selected == cell.date
                                val isToday = cell.date == today
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .then(
                                            if (isToday || isSelected) {
                                                Modifier.border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Steel else InkMuted,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                            } else Modifier
                                        )
                                        .clickable {
                                            selected = if (selected == cell.date) null else cell.date
                                        }
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = cell.date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (cell.inMonth) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (cell.inMonth) Ink else InkMuted,
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )
                                    Text(
                                        text = "${cell.department}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = InkMuted,
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                    if (eventDays.contains(cell.date)) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(1.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(EventDot)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (detailOpen) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                selectedEvents.forEach { e ->
                    SoftPanel {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Мероприятие", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(formatEventDate(e.date), color = InkMuted)
                            Text(e.name ?: "—", style = MaterialTheme.typography.titleMedium)
                            Text(e.place?.takeIf { it.isNotBlank() } ?: "—", color = InkMuted)
                            if (!e.time.isNullOrBlank()) {
                                Text(e.time, color = InkMuted)
                            }
                        }
                    }
                }
                if (CalendarMonthLogic.canShowShiftSummary(selected!!, today)) {
                    when {
                        dayLoading -> SoftPanel {
                            Text("Загрузка итогов смены…", color = InkMuted)
                        }
                        dayDashboard != null -> TodayDashboardPanel(
                            dashboard = dayDashboard!!,
                            title = "Итоги смены"
                        )
                        else -> SoftPanel {
                            Text(
                                dayError ?: "Нет данных по сменам за этот день",
                                color = InkMuted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

