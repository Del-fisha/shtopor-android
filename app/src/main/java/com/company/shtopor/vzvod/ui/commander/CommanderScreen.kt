package com.company.shtopor.vzvod.ui.commander

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotColleagueItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.dto.BotVacationsResponse
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.OperationalDay
import com.company.shtopor.vzvod.domain.ShiftMetricsLogic
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.shift.SimpleDropdown
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CommanderScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAssignShift: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var colleagues by remember { mutableStateOf<List<BotColleagueItem>>(emptyList()) }
    var department by remember { mutableIntStateOf(1) }
    var shifts by remember { mutableStateOf<List<BotShiftItem>>(emptyList()) }
    var vacations by remember { mutableStateOf<BotVacationsResponse?>(null) }
    var fromDate by remember { mutableStateOf(OperationalDay.today().minusDays(7).toString()) }
    var toDate by remember { mutableStateOf(OperationalDay.today().toString()) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            when (val r = container.repository.colleagues(department)) {
                is ApiResult.Ok -> colleagues = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
            when (val r = container.repository.refreshShifts()) {
                is ApiResult.Ok -> shifts = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
            when (val r = container.repository.refreshVacations()) {
                is ApiResult.Ok -> vacations = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
        }
    }

    LaunchedEffect(department) { load() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Командир", "Состав, смены, сутки и отпуска")
        SecondaryActionButton("Назад", onClick = onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("Состав", "Сутки", "Смены", "Отпуска").forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }

        when (tab) {
            0 -> {
                SoftPanel {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Отделение", style = MaterialTheme.typography.titleMedium)
                        SimpleDropdown(listOf("1", "2"), department.toString()) { department = it.toInt() }
                        SecondaryActionButton("Назначить смену (создание)", onClick = onAssignShift)
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colleagues, key = { it.serviceInfoId }) { c ->
                        SoftPanel {
                            Column {
                                Text(c.shortName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "отд. ${c.department ?: department}${c.rank?.let { " · $it" } ?: ""}",
                                    color = InkMuted
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                val today = OperationalDay.today().toString()
                val dayShifts = shifts.filter { it.date == today }
                val migrant = dayShifts.sumOf { it.migrant ?: 0 }
                val ibdr = dayShifts.sumOf { it.ibdr ?: 0 }
                val ap = dayShifts.sumOf { it.administrativeViolationsCount ?: 0 }
                val up = dayShifts.sumOf { it.criminalViolationsCount ?: 0 }
                val st = dayShifts.sumOf { it.countOfStatements ?: 0 }
                val claims = dayShifts.sumOf { it.countOfClaims ?: 0 }
                SoftPanel {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Отчёт за операционные сутки", style = MaterialTheme.typography.titleLarge)
                        Text(today, color = InkMuted)
                        Text("Смен в выборке (ваши): ${dayShifts.size}")
                        Text(ShiftMetricsLogic.formatSummary(ibdr, migrant, st, claims, ap, up))
                        Text(
                            "Полный сводный отчёт по всему взводу — через Core UI / raport-service. Здесь — ваши смены за сутки (расширяемый модуль).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted
                        )
                    }
                }
            }
            2 -> {
                SoftPanel {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Период", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = fromDate, onValueChange = { fromDate = it }, label = { Text("С") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = toDate, onValueChange = { toDate = it }, label = { Text("По") }, modifier = Modifier.fillMaxWidth())
                    }
                }
                val from = runCatching { LocalDate.parse(fromDate) }.getOrNull()
                val to = runCatching { LocalDate.parse(toDate) }.getOrNull()
                val filtered = shifts.filter {
                    val d = runCatching { LocalDate.parse(it.date) }.getOrNull() ?: return@filter false
                    from != null && to != null && !d.isBefore(from) && !d.isAfter(to)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { s ->
                        SoftPanel {
                            Column {
                                Text("${s.date} · ${s.route} · ${s.startTime ?: ""}–${s.endTime ?: "…"}")
                                Text(
                                    ShiftMetricsLogic.formatSummary(
                                        s.ibdr,
                                        s.migrant,
                                        s.countOfStatements,
                                        s.countOfClaims,
                                        s.administrativeViolationsCount,
                                        s.criminalViolationsCount
                                    ),
                                    color = InkMuted
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoftPanel {
                        val b = vacations?.balance
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Баланс отпусков", style = MaterialTheme.typography.titleLarge)
                            Text("Положено: ${b?.entitled ?: 0} · Доступно: ${b?.available ?: 0} · Использовано: ${b?.used ?: 0}")
                            Text(
                                "Сегодня и периоды — по вашим отпускам. Просмотр всего взвода расширяется при появлении commander API.",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    vacations?.items.orEmpty().forEach { v ->
                        SoftPanel {
                            Column {
                                Text("${v.typeLabel ?: "Отпуск"} · ${v.countOfDays ?: "?"} дн.")
                                Text("${v.startDate} — ${v.endDate}", color = InkMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null; load() }, onDismiss = { error = null })
    }
}
