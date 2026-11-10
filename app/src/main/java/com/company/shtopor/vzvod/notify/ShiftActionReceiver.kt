package com.company.shtopor.vzvod.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.company.shtopor.vzvod.VzvodApp
import com.company.shtopor.vzvod.domain.CheckingShiftPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Кнопки ± / проверок в шторке: меняют счётчики или фиксируют проверку маршрута.
 */
class ShiftActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val app = context.applicationContext as? VzvodApp ?: return
        val shiftId = intent.getStringExtra(ShiftNotificationManager.EXTRA_SHIFT_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                val repo = app.container.repository
                when (intent.action) {
                    ShiftNotificationManager.ACTION_IBD_INC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustIbdr(shiftId, 1)
                    }
                    ShiftNotificationManager.ACTION_IBD_DEC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustIbdr(shiftId, -1)
                    }
                    ShiftNotificationManager.ACTION_MIGRANT_INC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustMigrant(shiftId, 1)
                    }
                    ShiftNotificationManager.ACTION_MIGRANT_DEC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustMigrant(shiftId, -1)
                    }
                    ShiftNotificationManager.ACTION_MIGRANT_IBDR_INC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustMigrantAndIbdr(shiftId, 1)
                    }
                    ShiftNotificationManager.ACTION_MIGRANT_IBDR_DEC -> {
                        ShiftHapticFeedback.tick(app)
                        repo.adjustMigrantAndIbdr(shiftId, -1)
                    }
                    ShiftNotificationManager.ACTION_ROUTE_CHECK -> {
                        val routeId = intent.getStringExtra(ShiftNotificationManager.EXTRA_ROUTE_ID) ?: return@launch
                        ShiftHapticFeedback.tick(app)
                        repo.recordRouteCheck(shiftId, routeId)
                    }
                }
                val open = repo.openShift()
                if (open != null) {
                    val routes = if (CheckingShiftPolicy.isChecking(open.typeOfShiftId)) {
                        repo.cachedCheckableRoutes(open.id).ifEmpty {
                            repo.loadCheckableRoutes(open.id)
                            repo.cachedCheckableRoutes(open.id)
                        }
                    } else {
                        emptyList()
                    }
                    ShiftNotificationManager.showOrUpdate(app, open, routes)
                } else {
                    ShiftNotificationManager.cancel(app)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
