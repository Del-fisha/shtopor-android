package com.company.shtopor.vzvod.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotAddressResponse
import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.company.shtopor.vzvod.data.remote.dto.BotStaffItem
import com.company.shtopor.vzvod.data.remote.dto.BotStaffWorkResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleItem
import com.company.shtopor.vzvod.data.remote.dto.BotVocationItem
import com.company.shtopor.vzvod.data.remote.dto.BotWorkTotals
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.domain.ProfileDisplayFormat
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.launch

@Composable
fun StaffDirectoryScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onError: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(0) }
    var items by remember { mutableStateOf<List<BotStaffItem>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var viewedId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(targetPage: Int, search: String) {
        scope.launch {
            when (val result = container.repository.loadStaff(search, targetPage)) {
                is ApiResult.Ok -> {
                    page = result.value.page ?: targetPage
                    items = if (targetPage == 0) {
                        result.value.items.orEmpty()
                    } else {
                        items + result.value.items.orEmpty()
                    }
                    hasMore = result.value.hasMore == true
                    applied = search
                }
                is ApiResult.Err -> onError(result.userMessage())
            }
        }
    }

    LaunchedEffect(Unit) { load(0, "") }

    BackHandler {
        if (viewedId != null) viewedId = null else onBack()
    }

    val selected = viewedId
    if (selected != null) {
        StaffProfileScreen(
            container = container,
            userId = selected,
            onBack = { viewedId = null },
            onError = onError
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Карточка другого сотрудника", "Только просмотр.")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Фамилия или ФИО") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryActionButton("Найти", onClick = { load(0, query) })
        if (applied.isNotBlank()) {
            SecondaryActionButton("Показать всех", onClick = {
                query = ""
                load(0, "")
            })
        }
        if (items.isEmpty()) {
            Text("Никого не найдено", color = InkMuted)
        } else {
            items.forEach { person ->
                val name = person.displayName?.takeIf { it.isNotBlank() } ?: "Сотрудник"
                val rank = person.rank?.takeIf { it.isNotBlank() }
                val post = person.post?.takeIf { it.isNotBlank() } ?: "—"
                SoftPanel(Modifier.clickable { viewedId = person.userId }) {
                    Text(
                        if (rank == null) name else "$name ($rank)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "$post, ${ProfileDisplayFormat.departmentNumbered(person.department, null)}",
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        if (hasMore) {
            SecondaryActionButton("Ещё", onClick = { load(page + 1, applied) })
        }
        SecondaryActionButton("Назад к своему профилю", onClick = onBack)
    }
}

@Composable
private fun StaffProfileScreen(
    container: AppContainer,
    userId: String,
    onBack: () -> Unit,
    onError: (String) -> Unit
) {
    var profile by remember(userId) { mutableStateOf<BotProfileResponse?>(null) }
    var education by remember(userId) { mutableStateOf<BotEducationDto?>(null) }
    var vehicles by remember(userId) { mutableStateOf<List<BotVehicleItem>>(emptyList()) }
    var catalog by remember { mutableStateOf(AppCatalog.fallback()) }
    var work by remember(userId) { mutableStateOf<BotStaffWorkResponse?>(null) }
    var workError by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        when (val result = container.repository.loadStaffProfile(userId)) {
            is ApiResult.Ok -> profile = result.value
            is ApiResult.Err -> onError(result.userMessage())
        }
        when (val result = container.repository.loadStaffEducation(userId)) {
            is ApiResult.Ok -> education = result.value
            is ApiResult.Err -> Unit
        }
        when (val result = container.repository.loadStaffVehicles(userId)) {
            is ApiResult.Ok -> vehicles = result.value
            is ApiResult.Err -> Unit
        }
        when (val result = container.repository.catalogOptions()) {
            is ApiResult.Ok -> catalog = result.value
            is ApiResult.Err -> Unit
        }
        if (container.repository.role().isCommander) {
            when (val result = container.repository.loadStaffWork(userId)) {
                is ApiResult.Ok -> work = result.value
                is ApiResult.Err -> workError = result.userMessage()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Карточка другого сотрудника")
        SoftPanel {
            EmployeeCard(profile, education, vehicles, catalog)
        }
        if (container.repository.role().isCommander) {
            StaffWorkSection(work, workError)
        }
        SecondaryActionButton("К списку", onClick = onBack)
    }
}

@Composable
private fun StaffField(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun StaffWorkSection(work: BotStaffWorkResponse?, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Результаты", "Месяц с 1-го числа, год с 1 января. «Всего» не обнуляется.")
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
        }
        if (work == null && error == null) {
            Text("Загрузка…", color = InkMuted)
        }
        if (work != null) {
        WorkPeriod("Этот месяц", work.month)
        WorkPeriod("Этот год", work.yearTotals)
        WorkPeriod("Всего", work.allTime)
        SoftPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Отпуск ${work.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StaffField("Положено", work.vacationEntitled.toString())
                StaffField("Осталось", work.vacationRemaining.toString())
                val vacations = work.vacationsThisYear.orEmpty()
                Text("В этом году", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (vacations.isEmpty()) {
                    Text("Отпусков в этом году нет", color = InkMuted)
                } else {
                    vacations.forEach { VacationLine(it) }
                }
            }
        }
        }
    }
}

@Composable
private fun WorkPeriod(title: String, totals: BotWorkTotals?) {
    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkMetric("ИБДР", totals?.ibdr ?: 0, Modifier.weight(1f))
                WorkMetric("АП", totals?.administrative ?: 0, Modifier.weight(1f))
                WorkMetric("УП", totals?.criminal ?: 0, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorkMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(vertical = 4.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VacationLine(item: BotVocationItem) {
    val range = "${ProfileDisplayFormat.idCardDate(item.startDate)} — ${ProfileDisplayFormat.idCardDate(item.endDate)}"
    val days = item.countOfDays?.let { "$it дн." }
    val type = item.typeLabel?.takeIf { it.isNotBlank() }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(range, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
        Text(
            listOfNotNull(days, type).joinToString(" · ").ifBlank { "—" },
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StaffAddress(title: String, address: BotAddressResponse?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        StaffField("Город", address?.city ?: "—")
        StaffField("Улица", address?.street ?: "—")
        StaffField("Дом", address?.houseNumber ?: "—")
        StaffField("Квартира", address?.flat ?: "—")
        if (!address?.summary.isNullOrBlank()) {
            StaffField("Адрес", address?.summary.orEmpty())
        }
    }
}

@Composable
private fun StaffEducation(education: BotEducationDto?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Образование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (education == null) {
            Text("Нет данных", color = InkMuted)
        } else {
            StaffField("Тип", education.typeLabel ?: "—")
            StaffField("Статус", education.statusLabel ?: "—")
            StaffField("Учебное заведение", education.nameOfInstitution ?: "—")
            StaffField("Начало", education.started ?: "—")
            StaffField("Окончание", education.until ?: "—")
        }
    }
}

@Composable
private fun StaffVehicles(vehicles: List<BotVehicleItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Транспорт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (vehicles.isEmpty()) {
            Text("Нет данных", color = InkMuted)
        } else {
            vehicles.forEach { vehicle ->
                StaffField("Госномер", vehicle.stateNumber ?: "—")
                StaffField("Марка", vehicle.brand ?: "—")
                StaffField("Модель", vehicle.model ?: "—")
            }
        }
    }
}
