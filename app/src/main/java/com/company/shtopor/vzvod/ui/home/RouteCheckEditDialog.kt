package com.company.shtopor.vzvod.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckItem
import com.company.shtopor.vzvod.data.remote.dto.BotRouteCheckerOption
import com.company.shtopor.vzvod.domain.RouteCheckEditLogic
import com.company.shtopor.vzvod.ui.theme.InkMuted

@Composable
fun RouteCheckEditDialog(
    routeLabel: String,
    check: BotRouteCheckItem,
    commanders: List<BotRouteCheckerOption>,
    onDismiss: () -> Unit,
    onSave: (checkedAt: String, checkerServiceInfoId: String?) -> Unit,
    onDelete: () -> Unit
) {
    val options = remember(check, commanders) {
        val list = commanders.toMutableList()
        val currentId = check.checkerServiceInfoId
        if (!currentId.isNullOrBlank() && list.none { it.serviceInfoId == currentId }) {
            list.add(
                BotRouteCheckerOption(
                    serviceInfoId = currentId,
                    label = check.checkerLabel?.takeIf { it.isNotBlank() } ?: "Текущий"
                )
            )
        }
        list
    }
    var time by remember(check.id) {
        mutableStateOf(RouteCheckEditLogic.displayTime(check.checkedAt).let { if (it == "—") "" else it })
    }
    var checkerId by remember(check.id, options) {
        mutableStateOf(check.checkerServiceInfoId ?: options.firstOrNull()?.serviceInfoId)
    }
    var confirmDelete by remember(check.id) { mutableStateOf(false) }
    var timeError by remember(check.id) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить проверку?") },
            text = { Text("$routeLabel · ${RouteCheckEditLogic.displayTime(check.checkedAt)}") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(routeLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Время и кто из командиров проверил", color = InkMuted)
                OutlinedTextField(
                    value = time,
                    onValueChange = {
                        time = it
                        timeError = false
                    },
                    label = { Text("Время ЧЧ:ММ") },
                    singleLine = true,
                    isError = timeError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (options.isNotEmpty()) {
                    Text("Проверил", style = MaterialTheme.typography.titleSmall)
                    options.forEach { option ->
                        val label = option.label?.takeIf { it.isNotBlank() } ?: "—"
                        val mark = if (option.serviceInfoId == checkerId) "● " else ""
                        TextButton(
                            onClick = { checkerId = option.serviceInfoId },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$mark$label")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = RouteCheckEditLogic.normalizeTime(time)
                if (normalized == null) {
                    timeError = true
                } else {
                    onSave(normalized, checkerId)
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { confirmDelete = true }) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
