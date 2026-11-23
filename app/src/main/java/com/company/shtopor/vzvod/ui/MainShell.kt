package com.company.shtopor.vzvod.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.ui.annual.AnnualScreen
import com.company.shtopor.vzvod.ui.calendar.CalendarScreen
import com.company.shtopor.vzvod.ui.game.GameScreen
import com.company.shtopor.vzvod.ui.home.ShiftsScreen
import com.company.shtopor.vzvod.ui.profile.ProfileScreen
import com.company.shtopor.vzvod.ui.vacations.VacationsScreen
import kotlinx.coroutines.launch

private fun tabIcon(tabId: String): ImageVector = when (tabId) {
    AppSections.TAB_VACATIONS -> Icons.Outlined.BeachAccess
    AppSections.TAB_CALENDAR -> Icons.Outlined.CalendarMonth
    AppSections.TAB_PROFILE -> Icons.Outlined.Person
    else -> Icons.Outlined.WorkOutline
}

@Composable
fun MainShell(
    container: AppContainer,
    online: Boolean,
    onOpenCreateShift: () -> Unit,
    onOpenEndShift: (BotShiftItem) -> Unit,
    onOpenAdmin: (String) -> Unit,
    onOpenCriminal: (String) -> Unit,
    onOpenCreateEvent: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf(ShellHistory()) }
    val section = AppSections.byId(history.areaId)

    BackHandler(enabled = !drawerState.isOpen && history.canGoBack()) {
        history = history.back()
    }
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(220.dp)) {
                Text(
                    "Разделы",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                AppSections.all.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = history.areaId == item.id,
                        onClick = {
                            val defaultTab = item.tabs.firstOrNull()?.id
                            history = history.openArea(item.id, defaultTab)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Меню")
                }
            },
            bottomBar = {
                if (section.tabs.isNotEmpty()) {
                    NavigationBar {
                        section.tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = history.tabId == tab.id,
                                onClick = { history = history.selectTab(tab.id) },
                                icon = { Icon(tabIcon(tab.id), contentDescription = tab.label) },
                                label = {
                                    Text(
                                        tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (section.id) {
                AppSections.GAME -> GameScreen(contentModifier)
                AppSections.ANNUAL -> AnnualScreen(
                    container = container,
                    online = online,
                    tabId = history.tabId,
                    modifier = contentModifier
                )
                else -> when (history.tabId) {
                    AppSections.TAB_VACATIONS -> VacationsScreen(
                        container = container,
                        online = online,
                        modifier = contentModifier
                    )
                    AppSections.TAB_CALENDAR -> CalendarScreen(
                        container = container,
                        online = online,
                        modifier = contentModifier,
                        onOpenCreateEvent = onOpenCreateEvent
                    )
                    AppSections.TAB_PROFILE -> ProfileScreen(
                        container = container,
                        online = online,
                        modifier = contentModifier,
                        onLogout = onLogout
                    )
                    else -> ShiftsScreen(
                        container = container,
                        online = online,
                        modifier = contentModifier,
                        onOpenCreateShift = onOpenCreateShift,
                        onOpenEndShift = onOpenEndShift,
                        onOpenAdmin = onOpenAdmin,
                        onOpenCriminal = onOpenCriminal
                    )
                }
            }
        }
    }
}
