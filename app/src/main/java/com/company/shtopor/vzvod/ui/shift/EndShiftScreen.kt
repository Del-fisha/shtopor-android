package com.company.shtopor.vzvod.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.ShiftMetricsLogic
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.FieldHint
import com.company.shtopor.vzvod.ui.components.FieldLabel
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.Ink
import com.company.shtopor.vzvod.ui.theme.MistDeep
import com.company.shtopor.vzvod.ui.theme.Steel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun EndShiftScreen(
    container: AppContainer,
    shift: BotShiftItem,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var endTime by remember {
        mutableStateOf(LocalTime.now().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    var ibd by remember { mutableStateOf((shift.ibdr ?: 0).toString()) }
    var statements by remember { mutableStateOf((shift.countOfStatements ?: 0).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val adminCount = shift.administrativeViolationsCount ?: 0
    val criminalCount = shift.criminalViolationsCount ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Завершение смены", "Проверьте показатели перед закрытием")
        SoftPanel {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    ShiftMetricsLogic.formatSummary(
                        shift.ibdr,
                        shift.migrant,
                        shift.countOfStatements,
                        shift.countOfClaims,
                        adminCount,
                        criminalCount
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                FieldLabel("Время окончания")
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    placeholder = { Text("ЧЧ:ММ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FieldLabel("ИБДР (итог)")
                OutlinedTextField(
                    value = ibd,
                    onValueChange = { ibd = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FieldLabel("Заявки (итог)")
                OutlinedTextField(
                    value = statements,
                    onValueChange = { statements = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FieldHint("Нарушения фиксируются отдельными записями и здесь только отображаются.")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ViolationHighlightCard(
                        title = "АП",
                        value = adminCount,
                        modifier = Modifier.weight(1f)
                    )
                    ViolationHighlightCard(
                        title = "УП",
                        value = criminalCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        PrimaryActionButton(
            text = if (loading) "Закрытие…" else "Закрыть смену",
            enabled = !loading,
            onClick = {
                loading = true
                scope.launch {
                    val ibdVal = ibd.toIntOrNull()
                    val stVal = statements.toIntOrNull()
                    container.repository.setAbsoluteMetrics(shift.id, ibdVal, stVal)
                    when (val r = container.repository.endShift(shift.id, endTime)) {
                        is ApiResult.Ok -> {
                            loading = false
                            onDone()
                        }
                        is ApiResult.Err -> {
                            loading = false
                            error = r.userMessage()
                        }
                    }
                }
            }
        )
        SecondaryActionButton("Отмена", onClick = onCancel)
        Spacer(Modifier.height(24.dp))
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null }, onDismiss = { error = null })
    }
}

@Composable
private fun ViolationHighlightCard(
    title: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MistDeep,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier
                .background(MistDeep)
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    color = Steel
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    lineHeight = 50.sp,
                    color = Ink
                )
            )
        }
    }
}
