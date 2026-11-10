package com.company.shtopor.vzvod

import android.app.Application
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.notify.ShiftNotificationManager
import com.company.shtopor.vzvod.sync.SyncScheduler

class VzvodApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Новый запуск приложения → обязательный вход на сервер (актуальный пароль).
        container.clearSessionForColdStart()
        ShiftNotificationManager.ensureChannel(this)
        SyncScheduler.ensurePeriodic(this)
    }
}
