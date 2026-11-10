package com.company.shtopor.vzvod.ui.vacations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotVacationsResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVocationCreateRequest
import com.company.shtopor.vzvod.data.remote.dto.BotVocationItem
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.ui.components.ConfirmDialog
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.FieldLabel
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.shift.SimpleDropdown
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun VacationsScreen(
    container: AppContainer,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    var pending by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<BotVacationsResponse?>(null) }
    var catalog by remember { mutableStateOf(AppCatalog.fallback()) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<BotVocationItem?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            when (val r = container.repository.refreshVacations()) {
                is ApiResult.Ok -> data = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val r = container.repository.catalogOptions()) {
            is ApiResult.Ok -> catalog = r.value
            is ApiResult.Err -> Unit
        }
        reload()
        container.repository.pendingOpsFlow().collectLatest { pending = it }
    }

    PageScaffold(online = online, pendingCount = pending, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("Отпуска")

            SoftPanel {
                val bal = data?.balance
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Остаток дней", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Доступно: ${bal?.available ?: "—"} · положено: ${bal?.entitled ?: "—"} · использовано: ${bal?.used ?: "—"}",
                        color = InkMuted
                    )
                }
            }

            if (!creating) {
                PrimaryActionButton("Новый отпуск", onClick = { creating = true })
            } else {
                CreateVacationForm(
                    catalog = catalog,
                    onCancel = { creating = false },
                    onSubmit = { req ->
                        scope.launch {
                            when (val r = container.repository.createVacation(req)) {
                                is ApiResult.Ok -> {
                                    data = r.value
                                    creating = false
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    }
                )
            }

            val items = data?.items.orEmpty()
            if (items.isEmpty()) {
                SoftPanel { Text("Отпусков пока нет", color = InkMuted) }
            } else {
                items.forEach { item ->
                    SoftPanel {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                item.typeLabel ?: catalog.vocationLabel(item.typeId),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${item.startDate ?: "—"} — ${item.endDate ?: "—"} · ${item.countOfDays ?: "?"} дн.",
                                color = InkMuted
                            )
                            if (item.hasDeparture == true) {
                                Text(
                                    "Выезд: ${item.cityToDrive ?: "—"} (+${item.daysAddedByDeparture ?: 0} дн.)",
                                    color = InkMuted
                                )
                            }
                            if (isFutureVacation(item)) {
                                SecondaryActionButton("Удалить", onClick = { pendingDelete = item })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "Удалить отпуск?",
            message = "${item.startDate} — ${item.endDate}",
            confirmLabel = "Подтвердить",
            onConfirm = {
                val id = item.id
                pendingDelete = null
                if (id.isNullOrBlank()) {
                    error = "Нельзя удалить: нет идентификатора"
                    return@ConfirmDialog
                }
                scope.launch {
                    when (val r = container.repository.deleteVacation(id)) {
                        is ApiResult.Ok -> data = r.value
                        is ApiResult.Err -> error = r.userMessage()
                    }
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null; reload() }, onDismiss = { error = null })
    }
}

@Composable
private fun CreateVacationForm(
    catalog: AppCatalog,
    onCancel: () -> Unit,
    onSubmit: (BotVocationCreateRequest) -> Unit
) {
    var typeId by remember(catalog) { mutableIntStateOf(catalog.vocationTypes.first().id) }
    var start by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(20).toString()) }
    var hasDeparture by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf("") }

    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Новый отпуск", style = MaterialTheme.typography.titleLarge)
            FieldLabel("Тип")
            SimpleDropdown(
                options = catalog.vocationTypes.map { it.label },
                selected = catalog.vocationLabel(typeId)
            ) { label ->
                typeId = catalog.vocationTypes.first { it.label == label }.id
            }
            FieldLabel("Начало")
            OutlinedTextField(
                value = start,
                onValueChange = { start = it },
                placeholder = { Text("ГГГГ-ММ-ДД") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FieldLabel("Окончание")
            OutlinedTextField(
                value = end,
                onValueChange = { end = it },
                placeholder = { Text("ГГГГ-ММ-ДД") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDeparture, onCheckedChange = { hasDeparture = it })
                Text("Выезд за пределы региона")
            }
            if (hasDeparture) {
                FieldLabel("Город выезда")
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    placeholder = { Text("Город") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            PrimaryActionButton(
                "Сохранить",
                enabled = start.isNotBlank() && end.isNotBlank() && (!hasDeparture || city.isNotBlank()),
                onClick = {
                    onSubmit(
                        BotVocationCreateRequest(
                            typeId = typeId,
                            startDate = start.trim(),
                            endDate = end.trim(),
                            hasDeparture = hasDeparture,
                            cityToDrive = city.takeIf { hasDeparture && it.isNotBlank() }
                        )
                    )
                }
            )
            SecondaryActionButton("Отмена", onClick = onCancel)
        }
    }
}

private fun isFutureVacation(item: BotVocationItem): Boolean {
    val start = item.startDate ?: return false
    return runCatching {
        LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE).isAfter(LocalDate.now())
    }.getOrDefault(false)
}
