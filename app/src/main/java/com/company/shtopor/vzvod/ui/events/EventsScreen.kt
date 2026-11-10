package com.company.shtopor.vzvod.ui.events

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotEventItem
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.EventDepartmentFilter
import com.company.shtopor.vzvod.domain.EventListFilter
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(
    container: AppContainer,
    online: Boolean,
    modifier: Modifier = Modifier,
    onOpenCreateEvent: (() -> Unit)? = null,
    embedInParent: Boolean = false
) {
    var pending by remember { mutableStateOf(0) }
    var events by remember { mutableStateOf<List<BotEventItem>>(emptyList()) }
    var filter by remember { mutableStateOf(EventDepartmentFilter.ALL) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val canCreate = container.repository.role().isCommander && onOpenCreateEvent != null

    fun reload() {
        scope.launch {
            when (val r = container.repository.refreshEvents()) {
                is ApiResult.Ok -> events = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
        if (!embedInParent) {
            container.repository.pendingOpsFlow().collectLatest { pending = it }
        }
    }

    val filtered = remember(events, filter) {
        EventListFilter.filter(events, filter) { it.shiftOfDepartment }
    }

    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (embedInParent) Modifier.fillMaxSize() else Modifier.fillMaxSize())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!embedInParent) {
                SectionTitle("Мероприятия", "РАБОЧИЕ СМЕНЫ")
            }

            if (canCreate) {
                SecondaryActionButton("Добавить усиление", onClick = { onOpenCreateEvent?.invoke() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == EventDepartmentFilter.ALL,
                    onClick = { filter = EventDepartmentFilter.ALL },
                    label = { Text("Все") }
                )
                FilterChip(
                    selected = filter == EventDepartmentFilter.DEP_1,
                    onClick = { filter = EventDepartmentFilter.DEP_1 },
                    label = { Text("1 отделение") }
                )
                FilterChip(
                    selected = filter == EventDepartmentFilter.DEP_2,
                    onClick = { filter = EventDepartmentFilter.DEP_2 },
                    label = { Text("2 отделение") }
                )
            }

            if (filtered.isEmpty()) {
                SoftPanel {
                    Text("Нет предстоящих мероприятий", color = InkMuted)
                }
            } else {
                filtered.forEach { e ->
                    SoftPanel {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(e.name ?: "Без названия", style = MaterialTheme.typography.titleMedium)
                            Text(formatEventDate(e.date), color = InkMuted)
                            Text(
                                text = e.place?.takeIf { it.isNotBlank() } ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                            if (!e.time.isNullOrBlank()) {
                                Text(e.time, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                            }
                            if (filter == EventDepartmentFilter.ALL) {
                                Text(
                                    text = EventListFilter.shiftCaption(e.shiftOfDepartment),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (embedInParent) {
        Box(modifier = modifier) { body() }
    } else {
        PageScaffold(online = online, pendingCount = pending, modifier = modifier) {
            body()
        }
    }

    error?.let {
        ErrorRetryDialog(
            it,
            onRetry = {
                error = null
                reload()
            },
            onDismiss = { error = null }
        )
    }
}

internal fun formatEventDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return runCatching {
        LocalDate.parse(raw.take(10)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault(raw)
}
