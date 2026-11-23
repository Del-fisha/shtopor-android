package com.company.shtopor.vzvod.ui.annual

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.AnnualBoardDto
import com.company.shtopor.vzvod.data.remote.dto.AnnualItemDto
import com.company.shtopor.vzvod.data.remote.dto.AnnualPatchRequest
import com.company.shtopor.vzvod.data.remote.dto.AnnualPersonDto
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.ui.AppSections
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)
private val Track = Color(0xFFE6E6E6)
private val DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun AnnualScreen(
    container: AppContainer,
    online: Boolean,
    tabId: String?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var board by remember { mutableStateOf<AnnualBoardDto?>(null) }
    var people by remember { mutableStateOf<List<AnnualPersonDto>>(emptyList()) }
    var section by remember { mutableStateOf("ALL") }
    var department by remember { mutableStateOf<Int?>(null) }
    var sort by remember { mutableStateOf("DEFAULT") }

    fun reload() {
        scope.launch {
            error = null
            if (tabId == AppSections.TAB_ANNUAL_OTHERS) {
                when (val r = container.repository.loadAnnualOthers(section, department, sort)) {
                    is ApiResult.Ok -> people = r.value.people.orEmpty()
                    is ApiResult.Err -> error = r.userMessage()
                }
            } else {
                when (val r = container.repository.loadAnnual()) {
                    is ApiResult.Ok -> board = r.value
                    is ApiResult.Err -> error = r.userMessage()
                }
            }
        }
    }

    LaunchedEffect(tabId, section, department, sort) {
        reload()
    }

    fun patch(request: AnnualPatchRequest) {
        scope.launch {
            when (val r = container.repository.patchAnnual(request)) {
                is ApiResult.Ok -> {
                    board = r.value
                    error = null
                }
                is ApiResult.Err -> error = r.userMessage()
            }
        }
    }

    PageScaffold(online = online, pendingCount = 0, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Text(error!!, color = Red)
            }
            if (tabId == AppSections.TAB_ANNUAL_OTHERS) {
                OthersTab(people, section, department, sort, { section = it }, { department = it }, { sort = it })
            } else {
                OwnTab(tabId, board, ::patch)
            }
        }
    }
}

@Composable
private fun OwnTab(tabId: String?, board: AnnualBoardDto?, patch: (AnnualPatchRequest) -> Unit) {
    val items = board?.items.orEmpty()
    when (tabId) {
        AppSections.TAB_ANNUAL_CPD -> Group("ЦПД", items, "CPD", patch)
        AppSections.TAB_ANNUAL_TESTS -> Group("Зачёты", items, "TEST", patch)
        AppSections.TAB_ANNUAL_PSY -> Group("Психолог", items, "PSYCHOLOGIST", patch)
        else -> {
            SectionTitle("Профосмотр")
            Text(
                if (board?.medicalPassed == true) "Пройден" else "Не пройден",
                fontWeight = FontWeight.Bold,
                color = if (board?.medicalPassed == true) Green else Red
            )
            Group("Все врачи", items, "DOCTOR", patch)
            Group("Анализы", items, "ANALYSIS", patch)
        }
    }
}

@Composable
private fun Group(title: String, items: List<AnnualItemDto>, group: String, patch: (AnnualPatchRequest) -> Unit) {
    val rows = items.filter { it.group == group }
    if (rows.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    rows.forEach { item -> key(item.kind) { ItemRow(item, patch) } }
}

@Composable
private fun ItemRow(item: AnnualItemDto, patch: (AnnualPatchRequest) -> Unit) {
    val counts = item.counts == true
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.checked == true,
                onCheckedChange = { patch(AnnualPatchRequest(kind = item.kind, checked = it)) }
            )
            Column {
                Text(item.title.orEmpty(), fontWeight = FontWeight.Medium)
                Text(
                    if (counts) "Пройден" else "Не пройден",
                    color = if (counts) Green else Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (counts && !item.validUntil.isNullOrBlank()) {
            Text("Актуально до ${showDate(item.validUntil)}", color = Green, style = MaterialTheme.typography.bodySmall)
        }
        if (item.extendable == true) {
            SecondaryActionButton("Продлить", onClick = {
                patch(AnnualPatchRequest(kind = item.kind, extend = true))
            })
        }
        if (item.dateAllowed == true) {
            var text by remember(item.kind, item.performedOn) { mutableStateOf(showDate(item.performedOn)) }
            var dateError by remember(item.kind) { mutableStateOf<String?>(null) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Дата") },
                placeholder = { Text("дд.мм.гггг") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (dateError != null) {
                Text(dateError!!, color = Red, style = MaterialTheme.typography.bodySmall)
            }
            SecondaryActionButton("Сохранить дату", onClick = {
                val raw = text.trim()
                if (raw.isEmpty()) {
                    dateError = null
                    patch(AnnualPatchRequest(kind = item.kind, clearDate = true))
                } else {
                    val iso = runCatching { LocalDate.parse(raw, DMY).toString() }.getOrNull()
                    if (iso == null) {
                        dateError = "Дата в формате дд.мм.гггг"
                        return@SecondaryActionButton
                    }
                    dateError = null
                    patch(AnnualPatchRequest(kind = item.kind, performedOn = iso))
                }
            })
        }
    }
}

@Composable
private fun OthersTab(
    people: List<AnnualPersonDto>,
    section: String,
    department: Int?,
    sort: String,
    onSection: (String) -> Unit,
    onDepartment: (Int?) -> Unit,
    onSort: (String) -> Unit
) {
    SectionTitle("Другие сотрудники")
    ChipRow(
        listOf("ALL" to "Всё", "PROFOSMOTR" to "Профосмотр", "TESTS" to "Зачёты", "CPD" to "ЦПД", "PSYCHOLOGIST" to "Психолог"),
        section,
        onSection
    )
    ChipRow(
        listOf("all" to "Оба", "1" to "1-е", "2" to "2-е"),
        if (department == null) "all" else department.toString(),
        { value -> onDepartment(value.toIntOrNull()) }
    )
    ChipRow(
        listOf(
            "DEFAULT" to "По должности",
            "PERCENT_ASC" to "Процент от меньшего",
            "PERCENT_DESC" to "Процент от большего"
        ),
        sort,
        onSort
    )
    if (people.isEmpty()) {
        Text("Нет сотрудников")
        return
    }
    var previous: Int? = Int.MIN_VALUE
    var first = true
    people.forEach { person ->
        val number = person.departmentNumber
        if (department == null && (first || previous != number)) {
            Text(departmentTitle(number), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            previous = number
            first = false
        }
        PercentBar(person.percent ?: 0)
        Text(person.shortFio.orEmpty(), fontWeight = FontWeight.Bold)
        person.lines.orEmpty().forEach { line ->
            Text("${line.title}: ${line.text}")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
private fun PercentBar(percent: Int) {
    val fraction = percent.coerceIn(0, 100) / 100f
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Track)
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(Green)
            )
        }
    }
}

private fun showDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return runCatching { LocalDate.parse(raw.take(10)).format(DMY) }.getOrDefault(raw)
}

private fun departmentTitle(number: Int?): String = if (number == null) "Без отделения" else "$number-е отделение"
