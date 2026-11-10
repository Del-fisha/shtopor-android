package com.company.shtopor.vzvod.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.company.shtopor.vzvod.data.remote.dto.BotShiftItem
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.ui.calendar.CalendarScreen
import com.company.shtopor.vzvod.ui.home.ShiftsScreen
import com.company.shtopor.vzvod.ui.profile.ProfileScreen
import com.company.shtopor.vzvod.ui.vacations.VacationsScreen

private enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    SHIFTS("Смены", Icons.Outlined.WorkOutline),
    VACATIONS("Отпуска", Icons.Outlined.BeachAccess),
    CALENDAR("Календарь", Icons.Outlined.CalendarMonth),
    PROFILE("Профиль", Icons.Outlined.Person)
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
    var tab by remember { mutableStateOf(MainTab.SHIFTS) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                item.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (tab) {
            MainTab.SHIFTS -> ShiftsScreen(
                container = container,
                online = online,
                modifier = contentModifier,
                onOpenCreateShift = onOpenCreateShift,
                onOpenEndShift = onOpenEndShift,
                onOpenAdmin = onOpenAdmin,
                onOpenCriminal = onOpenCriminal
            )
            MainTab.VACATIONS -> VacationsScreen(
                container = container,
                online = online,
                modifier = contentModifier
            )
            MainTab.CALENDAR -> CalendarScreen(
                container = container,
                online = online,
                modifier = contentModifier,
                onOpenCreateEvent = onOpenCreateEvent
            )
            MainTab.PROFILE -> ProfileScreen(
                container = container,
                online = online,
                modifier = contentModifier,
                onLogout = onLogout
            )
        }
    }
}
