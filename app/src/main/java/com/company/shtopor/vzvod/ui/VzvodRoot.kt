package com.company.shtopor.vzvod.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.notify.ShiftNavCommand
import com.company.shtopor.vzvod.sync.SyncScheduler
import com.company.shtopor.vzvod.ui.auth.LoginScreen
import com.company.shtopor.vzvod.ui.events.CreateEventScreen
import com.company.shtopor.vzvod.ui.shift.CreateShiftScreen
import com.company.shtopor.vzvod.ui.shift.EndShiftScreen
import com.company.shtopor.vzvod.ui.shift.ViolationScreen
import kotlinx.coroutines.delay

private object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val CREATE_SHIFT = "create_shift"
    const val CREATE_EVENT = "create_event"
    const val END_SHIFT = "end_shift"
    const val ADMIN = "admin/{shiftId}"
    const val CRIMINAL = "criminal/{shiftId}"
}

@Composable
fun VzvodRoot(
    container: AppContainer,
    launchIntentTick: Int = 0
) {
    val nav = rememberNavController()
    val online by container.networkMonitor.online.collectAsState(initial = container.networkMonitor.isOnlineNow())
    val pendingOps by container.repository.pendingOpsFlow().collectAsState(initial = 0)
    var pendingEndShift by remember { mutableStateOf<BotShiftItem?>(null) }
    val start = if (container.session.isLoggedIn) Routes.MAIN else Routes.LOGIN

    // Синк при появлении сети И пока в очереди есть незакрытые операции
    // (если online не мигал false→true, всё равно сольём очередь).
    LaunchedEffect(online, pendingOps) {
        if (online && container.session.isLoggedIn && pendingOps > 0) {
            delay(500)
            var attempt = 0
            while (attempt < 3) {
                val ok = container.repository.syncOutboxNow()
                if (ok) break
                attempt++
                delay(1000L * (attempt + 1))
            }
            SyncScheduler.enqueueImmediate(container.appContext)
        } else if (online && container.session.isLoggedIn) {
            SyncScheduler.enqueueImmediate(container.appContext)
        }
    }

    LaunchedEffect(Unit) {
        if (container.session.isLoggedIn) {
            container.repository.syncActiveShiftNotification()
        }
        container.navCommands.collect { cmd ->
            if (!container.session.isLoggedIn) return@collect
            when (cmd) {
                is ShiftNavCommand.OpenAdmin -> {
                    nav.navigate(Routes.MAIN) { launchSingleTop = true }
                    nav.navigate("admin/${cmd.shiftId}")
                }
                is ShiftNavCommand.OpenCriminal -> {
                    nav.navigate(Routes.MAIN) { launchSingleTop = true }
                    nav.navigate("criminal/${cmd.shiftId}")
                }
                is ShiftNavCommand.OpenEndShift -> Unit
            }
        }
    }

    // Токен отвергнут сервером (смена пароля / перевыпуск) → экран входа.
    LaunchedEffect(Unit) {
        container.sessionExpired.collect {
            nav.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(launchIntentTick) {
        // tick triggers re-read of commands already emitted from onNewIntent
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = start) {
                composable(Routes.LOGIN) {
                    LoginScreen(container) {
                        nav.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
                composable(Routes.MAIN) {
                    LaunchedEffect(Unit) {
                        // Сверка активной смены с Core при каждом входе на главный экран.
                        container.repository.refreshShifts()
                        container.repository.syncActiveShiftNotification()
                        container.repository.catalogOptions()
                    }
                    MainShell(
                        container = container,
                        online = online,
                        onOpenCreateShift = { nav.navigate(Routes.CREATE_SHIFT) },
                        onOpenEndShift = {
                            pendingEndShift = it
                            nav.navigate(Routes.END_SHIFT)
                        },
                        onOpenAdmin = { nav.navigate("admin/$it") },
                        onOpenCriminal = { nav.navigate("criminal/$it") },
                        onOpenCreateEvent = { nav.navigate(Routes.CREATE_EVENT) },
                        onLogout = {
                            nav.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.CREATE_SHIFT) {
                    CreateShiftScreen(
                        container = container,
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }
                composable(Routes.CREATE_EVENT) {
                    CreateEventScreen(
                        container = container,
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }
                composable(Routes.END_SHIFT) {
                    val shift = pendingEndShift
                    if (shift == null) {
                        LaunchedEffect(Unit) {
                            if (!nav.popBackStack(Routes.MAIN, inclusive = false)) {
                                nav.navigate(Routes.MAIN) {
                                    popUpTo(Routes.LOGIN) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        EndShiftScreen(
                            container = container,
                            shift = shift,
                            onDone = {
                                nav.popBackStack(Routes.MAIN, inclusive = false)
                                pendingEndShift = null
                            },
                            onCancel = {
                                nav.popBackStack(Routes.MAIN, inclusive = false)
                                pendingEndShift = null
                            }
                        )
                    }
                }
                composable(
                    Routes.ADMIN,
                    arguments = listOf(navArgument("shiftId") { type = NavType.StringType })
                ) { entry ->
                    ViolationScreen(
                        container = container,
                        shiftId = entry.arguments?.getString("shiftId").orEmpty(),
                        administrative = true,
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }
                composable(
                    Routes.CRIMINAL,
                    arguments = listOf(navArgument("shiftId") { type = NavType.StringType })
                ) { entry ->
                    ViolationScreen(
                        container = container,
                        shiftId = entry.arguments?.getString("shiftId").orEmpty(),
                        administrative = false,
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
