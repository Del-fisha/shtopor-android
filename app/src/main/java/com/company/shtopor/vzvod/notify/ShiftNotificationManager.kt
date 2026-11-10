package com.company.shtopor.vzvod.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.company.shtopor.vzvod.MainActivity
import com.company.shtopor.vzvod.R
import com.company.shtopor.vzvod.data.remote.dto.BotCheckableRouteItem
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.domain.CheckingRouteButtonLabel
import com.company.shtopor.vzvod.domain.CheckingShiftPolicy
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Постоянное уведомление активной смены — через FGS оказывается вверху шторки (Ongoing).
 */
class ShiftForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val json = intent?.getStringExtra(EXTRA_SHIFT_JSON)
        val shift = json?.let { runCatching { shiftAdapter.fromJson(it) }.getOrNull() }
        if (shift == null || !shift.isOpen) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        val routesJson = intent.getStringExtra(EXTRA_ROUTES_JSON)
        val routes = routesJson?.let { runCatching { routesAdapter.fromJson(it) }.getOrNull() }.orEmpty()
        val notification = ShiftNotificationManager.buildNotification(this, shift, routes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ShiftNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(ShiftNotificationManager.NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun stopSelfSafely() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    companion object {
        private const val EXTRA_SHIFT_JSON = "shift_json"
        private const val EXTRA_ROUTES_JSON = "routes_json"
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val shiftAdapter = moshi.adapter(BotShiftItem::class.java)
        private val routesAdapter = moshi.adapter<List<BotCheckableRouteItem>>(
            Types.newParameterizedType(List::class.java, BotCheckableRouteItem::class.java)
        )

        fun startOrUpdate(
            context: Context,
            shift: BotShiftItem,
            routes: List<BotCheckableRouteItem> = emptyList()
        ) {
            if (!shift.isOpen) {
                stop(context)
                return
            }
            ShiftNotificationManager.ensureChannel(context)
            val intent = Intent(context, ShiftForegroundService::class.java).apply {
                putExtra(EXTRA_SHIFT_JSON, shiftAdapter.toJson(shift))
                putExtra(EXTRA_ROUTES_JSON, routesAdapter.toJson(routes))
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShiftForegroundService::class.java))
            ShiftNotificationManager.cancel(context)
        }
    }
}

object ShiftNotificationManager {
    /** Новый channel: старый LOW нельзя повысить после создания. */
    const val CHANNEL_ID = "vzvod_active_shift_max_v2"
    const val NOTIFICATION_ID = 42001

    const val ACTION_IBD_INC = "com.company.shtopor.vzvod.action.IBD_INC"
    const val ACTION_IBD_DEC = "com.company.shtopor.vzvod.action.IBD_DEC"
    const val ACTION_MIGRANT_INC = "com.company.shtopor.vzvod.action.MIGRANT_INC"
    const val ACTION_MIGRANT_DEC = "com.company.shtopor.vzvod.action.MIGRANT_DEC"
    const val ACTION_MIGRANT_IBDR_INC = "com.company.shtopor.vzvod.action.MIGRANT_IBDR_INC"
    const val ACTION_MIGRANT_IBDR_DEC = "com.company.shtopor.vzvod.action.MIGRANT_IBDR_DEC"
    const val ACTION_ROUTE_CHECK = "com.company.shtopor.vzvod.action.ROUTE_CHECK"

    const val EXTRA_SHIFT_ID = "shift_id"
    const val EXTRA_ROUTE_ID = "route_id"
    const val EXTRA_OPEN = "open_target"
    const val OPEN_ADMIN = "admin"
    const val OPEN_CRIMINAL = "criminal"
    const val OPEN_SHIFTS = "shifts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Удаляем старый тихий канал, если был
        runCatching { manager.deleteNotificationChannel("vzvod_active_shift") }
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Активная смена (счётчики)",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Счётчики открытой смены поверх других уведомлений"
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun showOrUpdate(
        context: Context,
        shift: BotShiftItem,
        routes: List<BotCheckableRouteItem> = emptyList()
    ) {
        if (!shift.isOpen) {
            ShiftForegroundService.stop(context)
            return
        }
        ensureChannel(context)
        ShiftForegroundService.startOrUpdate(context, shift, routes)
        val nm = NotificationManagerCompat.from(context.applicationContext)
        if (Build.VERSION.SDK_INT >= 33 && !nm.areNotificationsEnabled()) {
            android.util.Log.w("ShiftNotif", "POST_NOTIFICATIONS not granted")
            return
        }
        runCatching {
            nm.notify(NOTIFICATION_ID, buildNotification(context.applicationContext, shift, routes))
        }.onFailure {
            android.util.Log.e("ShiftNotif", "notify failed", it)
        }
    }

    fun buildNotification(
        context: Context,
        shift: BotShiftItem,
        routes: List<BotCheckableRouteItem> = emptyList()
    ): Notification {
        val app = context.applicationContext
        val shiftId = shift.id
        val checking = CheckingShiftPolicy.isChecking(shift.typeOfShiftId)
        val content = if (checking) {
            buildCheckingContent(app, shift, routes)
        } else {
            buildMetricsContent(app, shift)
        }
        val summary = if (checking) {
            checkingSummary(routes)
        } else {
            "ИБДР ${shift.ibdr ?: 0} · мигрант ${shift.migrant ?: 0} · " +
                "АП ${shift.administrativeViolationsCount ?: 0} · УП ${shift.criminalViolationsCount ?: 0}"
        }

        return NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shift)
            .setContentTitle(if (checking) "Проверки маршрутов" else "Открытая смена")
            .setContentText(summary)
            .setSubText(if (checking) "Тап = время проверки" else "Счётчики службы")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(content)
            .setCustomBigContentView(content)
            .setContentIntent(activityOpen(app, OPEN_SHIFTS, shiftId, 1))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSortKey("0_vzvod_shift")
            .build()
    }

    private fun buildMetricsContent(app: Context, shift: BotShiftItem): RemoteViews {
        val shiftId = shift.id
        val summary =
            "ИБДР ${shift.ibdr ?: 0} · мигрант ${shift.migrant ?: 0} · " +
                "АП ${shift.administrativeViolationsCount ?: 0} · УП ${shift.criminalViolationsCount ?: 0}"
        return RemoteViews(app.packageName, R.layout.notification_shift).apply {
            setTextViewText(
                R.id.notif_title,
                "Смена · ${shift.route ?: "маршрут"} · ${shift.startTime ?: "—"}"
            )
            setTextViewText(R.id.notif_summary, summary)
            setOnClickPendingIntent(R.id.btn_ibd_dec, broadcast(app, ACTION_IBD_DEC, shiftId, 11))
            setOnClickPendingIntent(R.id.btn_ibd_inc, broadcast(app, ACTION_IBD_INC, shiftId, 12))
            setOnClickPendingIntent(R.id.btn_migrant_dec, broadcast(app, ACTION_MIGRANT_DEC, shiftId, 13))
            setOnClickPendingIntent(R.id.btn_migrant_inc, broadcast(app, ACTION_MIGRANT_INC, shiftId, 14))
            setOnClickPendingIntent(
                R.id.btn_migrant_ibdr_dec,
                broadcast(app, ACTION_MIGRANT_IBDR_DEC, shiftId, 15)
            )
            setOnClickPendingIntent(
                R.id.btn_migrant_ibdr_inc,
                broadcast(app, ACTION_MIGRANT_IBDR_INC, shiftId, 16)
            )
            setOnClickPendingIntent(R.id.btn_admin, activityOpen(app, OPEN_ADMIN, shiftId, 21))
            setOnClickPendingIntent(R.id.btn_criminal, activityOpen(app, OPEN_CRIMINAL, shiftId, 22))
        }
    }

    private fun buildCheckingContent(
        app: Context,
        shift: BotShiftItem,
        routes: List<BotCheckableRouteItem>
    ): RemoteViews {
        val shiftId = shift.id
        return RemoteViews(app.packageName, R.layout.notification_shift_checking).apply {
            setTextViewText(
                R.id.notif_title,
                "Проверки · ${shift.startTime ?: "—"}"
            )
            setTextViewText(R.id.notif_summary, checkingSummary(routes))
            removeAllViews(R.id.routes_container)
            routes.take(8).forEachIndexed { index, route ->
                val label = routeButtonLabel(route)
                val row = RemoteViews(app.packageName, R.layout.notification_route_check_btn).apply {
                    setTextViewText(R.id.btn_route_check, label)
                    setOnClickPendingIntent(
                        R.id.btn_route_check,
                        routeCheckBroadcast(app, shiftId, route.routeId, 100 + index)
                    )
                }
                addView(R.id.routes_container, row)
            }
        }
    }

    private fun checkingSummary(routes: List<BotCheckableRouteItem>): String {
        if (routes.isEmpty()) return "Маршрутов сегодня нет"
        val withChecks = routes.count { !it.checks.isNullOrEmpty() }
        return "Маршрутов ${routes.size} · с проверками $withChecks · тап = время"
    }

    private fun routeButtonLabel(route: BotCheckableRouteItem): String {
        val name = route.routeLabel?.takeIf { it.isNotBlank() } ?: route.routeId
        val times = route.checks.orEmpty().map { it.checkedAt }
        return CheckingRouteButtonLabel.format(name, times)
    }

    private fun broadcast(context: Context, action: String, shiftId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ShiftActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_SHIFT_ID, shiftId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun routeCheckBroadcast(
        context: Context,
        shiftId: String,
        routeId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, ShiftActionReceiver::class.java).apply {
            action = ACTION_ROUTE_CHECK
            putExtra(EXTRA_SHIFT_ID, shiftId)
            putExtra(EXTRA_ROUTE_ID, routeId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityOpen(context: Context, target: String, shiftId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_OPEN, target)
            putExtra(EXTRA_SHIFT_ID, shiftId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
