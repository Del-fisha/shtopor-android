package com.company.shtopor.vzvod.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotEventCreateRequest
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.OperationalDay
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.shift.SimpleDropdown
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class EventTypeOption(val id: String, val label: String)

@Composable
fun CreateEventScreen(
    container: AppContainer,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val typeOptions = remember {
        listOf(
            EventTypeOption("OTHER", "Иное"),
            EventTypeOption("SPORT", "Спорт"),
            EventTypeOption("CONCERT", "Концерт"),
            EventTypeOption("FORUM", "Форум"),
            EventTypeOption("MIGRANT", "ОПМ")
        )
    }
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(OperationalDay.today().toString()) }
    var time by remember {
        mutableStateOf(LocalTime.now().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    var place by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var typeId by remember { mutableStateOf("OTHER") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedTypeLabel = typeOptions.firstOrNull { it.id == typeId }?.label ?: "Иное"
    val autoDept = remember(date) { OperationalDay.departmentFromDate(date) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Добавить усиление", "Создание мероприятия")
        SoftPanel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата (гггг-мм-дд)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Смена отделения (авто): $autoDept",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Время (чч:мм)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Тип", style = MaterialTheme.typography.titleSmall)
                SimpleDropdown(
                    options = typeOptions.map { it.label },
                    selected = selectedTypeLabel,
                    onSelect = { label ->
                        typeId = typeOptions.firstOrNull { it.label == label }?.id ?: "OTHER"
                    }
                )
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Место") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                PrimaryActionButton(
                    "Сохранить",
                    enabled = !saving && name.isNotBlank() && date.isNotBlank(),
                    onClick = {
                        saving = true
                        scope.launch {
                            when (
                                val r = container.repository.createEvent(
                                    BotEventCreateRequest(
                                        name = name.trim(),
                                        date = date.trim(),
                                        time = time.trim().ifBlank { null },
                                        place = place.trim().ifBlank { null },
                                        description = description.trim().ifBlank { null },
                                        eventType = typeId
                                    )
                                )
                            ) {
                                is ApiResult.Ok -> onDone()
                                is ApiResult.Err -> {
                                    error = r.userMessage()
                                    saving = false
                                }
                            }
                        }
                    }
                )
                SecondaryActionButton("Отмена", onClick = onCancel)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null }, onDismiss = { error = null })
    }
}
