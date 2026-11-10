package com.company.shtopor.vzvod.di

import android.content.Context
import androidx.room.Room
import com.company.shtopor.vzvod.data.local.AppDatabase
import com.company.shtopor.vzvod.data.remote.ApiClientFactory
import com.company.shtopor.vzvod.data.remote.NetworkMonitor
import com.company.shtopor.vzvod.data.repo.VzvodRepository
import com.company.shtopor.vzvod.data.secure.SecureSessionStore
import com.company.shtopor.vzvod.domain.SessionAuthPolicy
import com.company.shtopor.vzvod.notify.ShiftForegroundService
import com.company.shtopor.vzvod.notify.ShiftNavCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppContainer(context: Context) {
    val appContext = context.applicationContext

    val session = SecureSessionStore(appContext)
    val networkMonitor = NetworkMonitor(appContext)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    val apiFactory = ApiClientFactory(session) {
        expireSessionFromNetwork()
    }

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "vzvod_mobile.db"
    ).fallbackToDestructiveMigration().build()

    private val _navCommands = MutableSharedFlow<ShiftNavCommand>(extraBufferCapacity = 4)
    val navCommands: SharedFlow<ShiftNavCommand> = _navCommands.asSharedFlow()

    fun emitNavCommand(command: ShiftNavCommand) {
        _navCommands.tryEmit(command)
    }

    val repository = VzvodRepository(
        apiFactory = apiFactory,
        session = session,
        db = database,
        networkMonitor = networkMonitor,
        appContext = appContext
    )

    /** Каждый холодный старт процесса — снова логин/пароль. */
    fun clearSessionForColdStart() {
        if (!SessionAuthPolicy.mustReauthOnProcessStart()) return
        if (!session.isLoggedIn) return
        session.clearSession()
        ShiftForegroundService.stop(appContext)
    }

    private fun expireSessionFromNetwork() {
        if (!session.isLoggedIn) return
        session.clearSession()
        ShiftForegroundService.stop(appContext)
        _sessionExpired.tryEmit(Unit)
    }
}
