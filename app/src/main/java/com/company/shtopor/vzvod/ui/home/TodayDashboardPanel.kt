package com.company.shtopor.vzvod.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.dto.BotLabeledCount
import com.company.shtopor.vzvod.data.remote.dto.BotTodayDashboardResponse
import com.company.shtopor.vzvod.data.remote.dto.BotTodayRouteRow
import com.company.shtopor.vzvod.data.remote.dto.BotTodayTotals
import com.company.shtopor.vzvod.domain.ArticleCountFormatter
import com.company.shtopor.vzvod.domain.LabeledCount
import com.company.shtopor.vzvod.domain.TodayDashboardSection
import com.company.shtopor.vzvod.domain.TodayDashboardTapTarget
import com.company.shtopor.vzvod.domain.TodayDashboardUiPolicy
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted

@Composable
fun TodayDashboardPanel(
    dashboard: BotTodayDashboardResponse,
    title: String = "Сводка за сегодня"
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Отделение ${dashboard.departmentNumber ?: "—"} · ${dashboard.operationalDate ?: "—"}",
                color = InkMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        TodayDashboardUiPolicy.sectionOrder().forEach { section ->
            when (section) {
                TodayDashboardSection.TOTALS -> {
                    dashboard.totals?.let { TotalsDashboardCard(it) }
                }
                TodayDashboardSection.ROUTES -> {
                    RoutesDashboardSection(dashboard.routes.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun RoutesDashboardSection(routes: List<BotTodayRouteRow>) {
    var open by remember { mutableStateOf(!TodayDashboardUiPolicy.sectionCollapsedByDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CollapsibleTrayHeader(
            title = "Маршруты",
            expanded = open,
            onToggle = { open = !open }
        )
        if (open) {
            if (routes.isEmpty()) {
                SoftPanel(surfaceAlpha = 0.88f) {
                    Text(text = "Нет смен за операционные сутки", color = InkMuted)
                }
            } else {
                routes.forEach { route -> RouteDashboardCard(route) }
            }
        }
    }
}

@Composable
private fun CollapsibleTrayHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    SoftPanel(surfaceAlpha = 0.88f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "▾" else "▸",
                style = MaterialTheme.typography.titleMedium,
                color = InkMuted
            )
        }
    }
}

@Composable
private fun RouteDashboardCard(route: BotTodayRouteRow) {
    var employeesOpen by remember(route.routeLabel) { mutableStateOf(false) }
    var apOpen by remember(route.routeLabel) { mutableStateOf(false) }
    var upOpen by remember(route.routeLabel) { mutableStateOf(false) }

    SoftPanel(surfaceAlpha = 0.88f) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val routeExpandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.ROUTE_LABEL)
            Text(
                text = route.routeLabel ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (routeExpandable) Modifier.clickable { employeesOpen = !employeesOpen }
                        else Modifier
                    )
            )
            if (employeesOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val employees = route.employees.orEmpty()
                    if (employees.isEmpty()) {
                        Text(text = "Нет сотрудников", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        employees.forEach { name ->
                            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        }
                    }
                }
            }

            HorizontalDivider(color = InkMuted.copy(alpha = 0.25f))

            MetricLine(
                target = TodayDashboardTapTarget.IBDR,
                value = route.ibdr ?: 0,
                expandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.IBDR),
                open = false,
                onToggle = {}
            )
            MetricLine(
                target = TodayDashboardTapTarget.MIGRANT,
                value = route.migrant ?: 0,
                expandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.MIGRANT),
                open = false,
                onToggle = {}
            )
            MetricLine(
                target = TodayDashboardTapTarget.AP,
                value = route.administrative ?: 0,
                expandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.AP),
                open = apOpen,
                onToggle = { apOpen = !apOpen }
            )
            if (apOpen) {
                ArticleLines(route.administrativeArticles)
            }
            MetricLine(
                target = TodayDashboardTapTarget.UP,
                value = route.criminal ?: 0,
                expandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.UP),
                open = upOpen,
                onToggle = { upOpen = !upOpen }
            )
            if (upOpen) {
                ArticleLines(route.criminalTypes)
            }
            MetricLine(
                target = TodayDashboardTapTarget.STATEMENTS,
                value = route.statements ?: 0,
                expandable = TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.STATEMENTS),
                open = false,
                onToggle = {}
            )
        }
    }
}

@Composable
private fun TotalsDashboardCard(totals: BotTodayTotals) {
    var open by remember { mutableStateOf(!TodayDashboardUiPolicy.sectionCollapsedByDefault()) }
    var apOpen by remember { mutableStateOf(false) }
    var upOpen by remember { mutableStateOf(false) }

    SoftPanel(surfaceAlpha = 0.88f) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TodayDashboardUiPolicy.totalsTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (open) "▾" else "▸",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkMuted
                )
            }
            if (open) {
            HorizontalDivider(color = InkMuted.copy(alpha = 0.25f))

            MetricLine(
                target = TodayDashboardTapTarget.IBDR,
                value = totals.ibdr ?: 0,
                expandable = TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.IBDR),
                open = false,
                onToggle = {}
            )
            MetricLine(
                target = TodayDashboardTapTarget.MIGRANT,
                value = totals.migrant ?: 0,
                expandable = TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.MIGRANT),
                open = false,
                onToggle = {}
            )
            MetricLine(
                target = TodayDashboardTapTarget.AP,
                value = totals.administrative ?: 0,
                expandable = TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.AP),
                open = apOpen,
                onToggle = { apOpen = !apOpen }
            )
            if (apOpen) {
                ArticleLines(totals.administrativeArticles)
            }
            MetricLine(
                target = TodayDashboardTapTarget.UP,
                value = totals.criminal ?: 0,
                expandable = TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.UP),
                open = upOpen,
                onToggle = { upOpen = !upOpen }
            )
            if (upOpen) {
                ArticleLines(totals.criminalTypes)
            }
            MetricLine(
                target = TodayDashboardTapTarget.STATEMENTS,
                value = totals.statements ?: 0,
                expandable = TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.STATEMENTS),
                open = false,
                onToggle = {}
            )
            }
        }
    }
}

@Composable
private fun MetricLine(
    target: TodayDashboardTapTarget,
    value: Int,
    expandable: Boolean,
    open: Boolean,
    onToggle: () -> Unit
) {
    val label = TodayDashboardUiPolicy.label(target)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expandable) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (open) FontWeight.Bold else FontWeight.Medium,
            color = if (expandable) MaterialTheme.colorScheme.onSurface else InkMuted
        )
    }
}

@Composable
private fun ArticleLines(items: List<BotLabeledCount>?) {
    val lines = ArticleCountFormatter.fromCounts(
        items.orEmpty().map { LabeledCount(it.label.orEmpty(), it.count ?: 0) }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (lines.isEmpty()) {
            Text(text = "Нет статей", color = InkMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
