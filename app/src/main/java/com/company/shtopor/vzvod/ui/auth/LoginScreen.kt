package com.company.shtopor.vzvod.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted
import com.company.shtopor.vzvod.ui.theme.Mist
import com.company.shtopor.vzvod.ui.theme.MistDeep
import com.company.shtopor.vzvod.ui.theme.Steel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    container: AppContainer,
    onLoggedIn: () -> Unit
) {
    var username by remember { mutableStateOf(container.session.username.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(container.session.baseUrl) }
    var showAdminSettings by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

    fun doLogin() {
        lastAction = { doLogin() }
        loading = true
        error = null
        container.session.baseUrl = baseUrl
        container.apiFactory.invalidate()
        scope.launch {
            when (val result = container.repository.login(username.trim(), password)) {
                is ApiResult.Ok -> {
                    loading = false
                    onLoggedIn()
                }
                is ApiResult.Err -> {
                    loading = false
                    error = result.userMessage()
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mist, MistDeep, Mist)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Взвод", style = MaterialTheme.typography.displayLarge, color = Steel)
            Spacer(Modifier.height(8.dp))
            Text(
                "Вход тем же логином и паролем, что в веб-системе",
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted
            )
            Spacer(Modifier.height(28.dp))
            SoftPanel {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Вход", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Логин") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    PrimaryActionButton(
                        text = if (loading) "Вход…" else "Войти",
                        onClick = { doLogin() },
                        enabled = !loading && username.isNotBlank() && password.isNotBlank()
                    )

                    TextButton(onClick = { showAdminSettings = !showAdminSettings }) {
                        Text(
                            if (showAdminSettings) "Скрыть настройки сервера"
                            else "Настройки сервера (для администратора)"
                        )
                    }

                    AnimatedVisibility(visible = showAdminSettings) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "По умолчанию приложение ходит на публичный сервер Взвод через интернет.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = { baseUrl = it },
                                label = { Text("Адрес сервера") },
                                supportingText = {
                                    Text("Обычно: http://www.vzvod-shtopor.ru")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "После входа роль определяется автоматически: командир (полный доступ) или полицейский.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted
            )
        }
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    error?.let {
        ErrorRetryDialog(
            message = it,
            onRetry = { error = null; lastAction?.invoke() },
            onDismiss = { error = null }
        )
    }
}
