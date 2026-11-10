package com.company.shtopor.vzvod

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.company.shtopor.vzvod.notify.ShiftNavCommand
import com.company.shtopor.vzvod.notify.ShiftNotificationManager
import com.company.shtopor.vzvod.ui.VzvodRoot
import com.company.shtopor.vzvod.ui.theme.VzvodTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var launchIntentTick by mutableStateOf(0)

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val app = application as? VzvodApp ?: return@registerForActivityResult
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { app.container.repository.syncActiveShiftNotification() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        handleNavIntent(intent)
        val app = application as VzvodApp
        setContent {
            VzvodTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VzvodRoot(
                        container = app.container,
                        launchIntentTick = launchIntentTick
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavIntent(intent)
        launchIntentTick++
    }

    private fun handleNavIntent(intent: Intent?) {
        if (intent == null) return
        val app = application as? VzvodApp ?: return
        val target = intent.getStringExtra(ShiftNotificationManager.EXTRA_OPEN) ?: return
        val shiftId = intent.getStringExtra(ShiftNotificationManager.EXTRA_SHIFT_ID).orEmpty()
        when (target) {
            ShiftNotificationManager.OPEN_ADMIN -> {
                if (shiftId.isNotBlank()) {
                    app.container.emitNavCommand(ShiftNavCommand.OpenAdmin(shiftId))
                }
            }
            ShiftNotificationManager.OPEN_CRIMINAL -> {
                if (shiftId.isNotBlank()) {
                    app.container.emitNavCommand(ShiftNavCommand.OpenCriminal(shiftId))
                }
            }
        }
        intent.removeExtra(ShiftNotificationManager.EXTRA_OPEN)
        intent.removeExtra(ShiftNotificationManager.EXTRA_SHIFT_ID)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
