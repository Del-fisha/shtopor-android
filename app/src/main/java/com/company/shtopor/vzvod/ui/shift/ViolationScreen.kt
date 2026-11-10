package com.company.shtopor.vzvod.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.company.shtopor.vzvod.data.remote.dto.BotEnumOption
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import kotlinx.coroutines.launch

@Composable
fun ViolationScreen(
    container: AppContainer,
    shiftId: String,
    administrative: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var impacts by remember { mutableStateOf<List<BotEnumOption>>(emptyList()) }
    var articles by remember { mutableStateOf<List<BotEnumOption>>(emptyList()) }
    var types by remember { mutableStateOf<List<BotEnumOption>>(emptyList()) }
    var impactId by remember { mutableStateOf<Int?>(null) }
    var secondId by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (val r = container.repository.violationOptions()) {
            is ApiResult.Ok -> {
                impacts = r.value.impacts.orEmpty()
                articles = r.value.administrativeArticles.orEmpty()
                types = r.value.criminalTypes.orEmpty()
            }
            is ApiResult.Err -> error = r.userMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle(
            if (administrative) "Административное правонарушение" else "Уголовное правонарушение",
            "Выберите применение и статью / тип. Сохраняется офлайн до появления сети."
        )
        SoftPanel {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Применение", style = MaterialTheme.typography.titleMedium)
                SimpleDropdown(
                    options = impacts.map { it.label ?: it.id.toString() },
                    selected = impacts.firstOrNull { it.id == impactId }?.label ?: "Выберите"
                ) { label ->
                    impactId = impacts.first { (it.label ?: it.id.toString()) == label }.id
                }
                if (administrative) {
                    Text("Статья КоАП", style = MaterialTheme.typography.titleMedium)
                    SimpleDropdown(
                        options = articles.map { it.label ?: it.id.toString() },
                        selected = articles.firstOrNull { it.id == secondId }?.label ?: "Выберите"
                    ) { label ->
                        secondId = articles.first { (it.label ?: it.id.toString()) == label }.id
                    }
                } else {
                    Text("Тип", style = MaterialTheme.typography.titleMedium)
                    SimpleDropdown(
                        options = types.map { it.label ?: it.id.toString() },
                        selected = types.firstOrNull { it.id == secondId }?.label ?: "Выберите"
                    ) { label ->
                        secondId = types.first { (it.label ?: it.id.toString()) == label }.id
                    }
                }
            }
        }
        PrimaryActionButton(
            text = if (loading) "Сохранение…" else "Зафиксировать",
            enabled = !loading && impactId != null && secondId != null,
            onClick = {
                loading = true
                scope.launch {
                    val result = if (administrative) {
                        container.repository.createAdminViolation(shiftId, impactId!!, secondId!!)
                    } else {
                        container.repository.createCriminalViolation(shiftId, impactId!!, secondId!!)
                    }
                    loading = false
                    when (result) {
                        is ApiResult.Ok -> onDone()
                        is ApiResult.Err -> error = result.userMessage()
                    }
                }
            }
        )
        SecondaryActionButton("Отмена", onClick = onCancel)
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null }, onDismiss = { error = null })
    }
}
