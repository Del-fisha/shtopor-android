package com.company.shtopor.vzvod.ui.shift

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotColleagueItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftUpsertRequest
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.domain.OperationalDay
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.FieldHint
import com.company.shtopor.vzvod.ui.components.FieldLabel
import com.company.shtopor.vzvod.ui.components.KeyValueLine
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted
import com.company.shtopor.vzvod.ui.theme.MistDeep
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShiftScreen(
    container: AppContainer,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var catalog by remember { mutableStateOf(AppCatalog.fallback()) }
    var date by remember { mutableStateOf(OperationalDay.today().toString()) }
    var route by remember { mutableStateOf(catalog.shiftRoutes.first().id) }
    var typeId by remember { mutableStateOf(catalog.defaultType(route)) }
    var colleagues by remember { mutableStateOf<List<BotColleagueItem>>(emptyList()) }
    var selectedPartnerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPartnerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var startTime by remember {
        mutableStateOf(
            LocalTime.now().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val preferredDept = remember(date) { OperationalDay.departmentFromDate(date) }

    fun loadColleagues() {
        scope.launch {
            when (val r = container.repository.colleagues(preferredDept)) {
                is ApiResult.Ok -> colleagues = r.value
                is ApiResult.Err -> error = r.userMessage()
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val r = container.repository.catalogOptions()) {
            is ApiResult.Ok -> {
                catalog = r.value
                if (catalog.shiftRoutes.none { it.id == route }) {
                    route = catalog.shiftRoutes.first().id
                    typeId = catalog.defaultType(route)
                }
                if (catalog.shiftTypes.none { it.id == typeId }) {
                    typeId = catalog.defaultType(route)
                }
            }
            is ApiResult.Err -> Unit
        }
    }

    LaunchedEffect(preferredDept, step) {
        if (step == 4) loadColleagues()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Новая смена", "Шаг $step из 6")

        SoftPanel {
            when (step) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FieldLabel("Дата операционных суток")
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            placeholder = { Text("ГГГГ-ММ-ДД") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FieldHint("Перелом суток — 04:00 МСК. Отделение сегодня: $preferredDept")
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FieldLabel("Номер маршрута")
                        ChoiceDropdown(
                            options = catalog.shiftRoutes.map { it.label },
                            selected = catalog.shiftRoutes.firstOrNull { it.id == route }?.label
                                ?: route,
                            onSelect = { label ->
                                val selected = catalog.shiftRoutes.first { it.label == label }
                                route = selected.id
                                typeId = catalog.defaultType(selected.id)
                            }
                        )
                    }
                }
                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FieldLabel("Тип смены")
                        ChoiceDropdown(
                            options = catalog.shiftTypes.map { it.label },
                            selected = catalog.typeLabel(typeId),
                            onSelect = { label ->
                                typeId = catalog.shiftTypes.first { it.label == label }.id
                            }
                        )
                    }
                }
                4 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FieldLabel("Напарники")
                        FieldHint(
                            "Напарник необязателен — можно одному или выбрать коллег. Сначала ваше отделение, затем другое, внутри — по фамилии. Занятые в смене скрыты."
                        )
                        if (colleagues.isEmpty()) {
                            FieldHint("Список пуст или недоступен офлайн. Проверьте сеть.")
                        } else {
                            ColleagueMultiPicker(
                                colleagues = colleagues,
                                preferredDepartment = preferredDept,
                                selectedIds = selectedPartnerIds,
                                onToggle = { item ->
                                    val id = item.serviceInfoId
                                    if (selectedPartnerIds.contains(id)) {
                                        selectedPartnerIds = selectedPartnerIds - id
                                        selectedPartnerNames = selectedPartnerNames - id
                                    } else {
                                        selectedPartnerIds = selectedPartnerIds + id
                                        selectedPartnerNames = selectedPartnerNames + (id to item.shortName)
                                    }
                                }
                            )
                        }
                        if (selectedPartnerIds.isNotEmpty()) {
                            FieldHint("Выбрано: ${selectedPartnerIds.size}")
                        }
                    }
                }
                5 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FieldLabel("Время начала")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                placeholder = { Text("ЧЧ:ММ") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    startTime = LocalTime.now().withSecond(0).withNano(0)
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Сейчас")
                            }
                        }
                        FieldHint("Формат 24 часа, например 09:30")
                    }
                }
                6 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FieldLabel("Проверьте данные")
                        KeyValueLine("Дата", formatRuDate(date))
                        KeyValueLine("Маршрут", route)
                        KeyValueLine("Тип", catalog.typeLabel(typeId))
                        KeyValueLine("Отделение сегодня", preferredDept.toString())
                        KeyValueLine(
                            "Напарники",
                            selectedPartnerIds.mapNotNull { selectedPartnerNames[it] }
                                .joinToString(", ")
                                .ifBlank { "—" }
                        )
                        KeyValueLine("Начало", startTime)
                    }
                }
            }
        }

        PrimaryActionButton(
            text = when {
                loading -> "Сохранение…"
                step < 6 -> "Далее"
                else -> "Создать смену"
            },
            enabled = !loading && when (step) {
                4 -> !com.company.shtopor.vzvod.domain.CheckingShiftPolicy.partnersRequired(typeId)
                    || selectedPartnerIds.isNotEmpty()
                5 -> startTime.matches(Regex("^\\d{1,2}:\\d{2}$"))
                6 -> (!com.company.shtopor.vzvod.domain.CheckingShiftPolicy.partnersRequired(typeId)
                    || selectedPartnerIds.isNotEmpty()) && date.isNotBlank() && startTime.isNotBlank()
                else -> true
            },
            onClick = {
                if (step < 6) {
                    if (step == 5 && !normalizeTime(startTime).also { startTime = it }.matches(Regex("^\\d{2}:\\d{2}$"))) {
                        error = "Время начала в формате ЧЧ:ММ"
                        return@PrimaryActionButton
                    }
                    step++
                } else {
                    loading = true
                    scope.launch {
                        val normalizedTime = normalizeTime(startTime)
                        startTime = normalizedTime
                        val partnerIds = selectedPartnerIds.toList()
                        val result = container.repository.createShift(
                            BotShiftUpsertRequest(
                                date = date.trim(),
                                routeId = route,
                                typeOfShiftId = typeId,
                                startTime = normalizedTime,
                                partnerServiceInfoId = partnerIds.firstOrNull(),
                                partnerServiceInfoIds = partnerIds
                            )
                        )
                        loading = false
                        when (result) {
                            is ApiResult.Ok -> onDone()
                            is ApiResult.Err -> error = result.userMessage()
                        }
                    }
                }
            }
        )
        SecondaryActionButton(
            text = if (step == 1) "Отмена" else "Назад",
            onClick = {
                if (step == 1) onCancel() else step--
            }
        )
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null }, onDismiss = { error = null })
    }
}

@Composable
private fun ColleagueMultiPicker(
    colleagues: List<BotColleagueItem>,
    preferredDepartment: Int,
    selectedIds: Set<String>,
    onToggle: (BotColleagueItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colleagues.forEach { item ->
            val selected = selectedIds.contains(item.serviceInfoId)
            val dep = item.department
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MistDeep else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(item) }
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggle(item) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.shortName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when {
                                dep == preferredDepartment -> "Отделение $dep · сегодня"
                                dep != null -> "Отделение $dep"
                                else -> "Отделение —"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    ChoiceDropdown(options, selected, onSelect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Выберите") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(opt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun normalizeTime(raw: String): String {
    val parts = raw.trim().split(':')
    if (parts.size != 2) return raw.trim()
    val h = parts[0].toIntOrNull() ?: return raw.trim()
    val m = parts[1].toIntOrNull() ?: return raw.trim()
    if (h !in 0..23 || m !in 0..59) return raw.trim()
    return "%02d:%02d".format(h, m)
}

private fun formatRuDate(iso: String): String =
    runCatching {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault(iso)
