package com.company.shtopor.vzvod.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.ApiResult
import com.company.shtopor.vzvod.data.remote.dto.BotAddressPatch
import com.company.shtopor.vzvod.data.remote.dto.BotAddressResponse
import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import com.company.shtopor.vzvod.data.remote.dto.BotEducationUpsertRequest
import com.company.shtopor.vzvod.data.remote.dto.BotProfilePatchRequest
import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleItem
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleUpsertRequest
import com.company.shtopor.vzvod.data.remote.userMessage
import com.company.shtopor.vzvod.di.AppContainer
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.domain.EducationTypes
import com.company.shtopor.vzvod.domain.ProfileDisplayFormat
import com.company.shtopor.vzvod.ui.components.ConfirmDialog
import com.company.shtopor.vzvod.ui.components.ErrorRetryDialog
import com.company.shtopor.vzvod.ui.components.PageScaffold
import com.company.shtopor.vzvod.ui.components.PrimaryActionButton
import com.company.shtopor.vzvod.ui.components.SecondaryActionButton
import com.company.shtopor.vzvod.ui.components.SectionTitle
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.shift.SimpleDropdown
import com.company.shtopor.vzvod.ui.theme.InkMuted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class ProfileSection { NONE, BREASTPLATE, MEDICAL, CONTACTS, EDUCATION, VEHICLES }

@Composable
fun ProfileScreen(
    container: AppContainer,
    online: Boolean,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    var pending by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf<BotProfileResponse?>(null) }
    var section by remember { mutableStateOf(ProfileSection.NONE) }
    var education by remember { mutableStateOf<BotEducationDto?>(null) }
    var vehicles by remember { mutableStateOf<List<BotVehicleItem>>(emptyList()) }
    var catalog by remember { mutableStateOf(AppCatalog.fallback()) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reloadExtras() {
        scope.launch {
            when (val r = container.repository.loadEducation()) {
                is ApiResult.Ok -> education = r.value
                is ApiResult.Err -> Unit
            }
            when (val r = container.repository.loadVehicles()) {
                is ApiResult.Ok -> vehicles = r.value
                is ApiResult.Err -> Unit
            }
            when (val r = container.repository.catalogOptions()) {
                is ApiResult.Ok -> catalog = r.value
                is ApiResult.Err -> Unit
            }
        }
    }

    fun reload() {
        scope.launch {
            when (val r = container.repository.refreshProfile()) {
                is ApiResult.Ok -> profile = r.value
                is ApiResult.Err -> {
                    profile = container.repository.cachedProfile()
                    error = r.userMessage()
                }
            }
            reloadExtras()
        }
    }

    LaunchedEffect(Unit) {
        reload()
        container.repository.pendingOpsFlow().collectLatest { pending = it }
    }

    PageScaffold(online = online, pendingCount = pending, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("Профиль")

            SoftPanel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(profile?.displayName ?: "—", style = MaterialTheme.typography.titleLarge)
                    ProfileField("Звание", profile?.rank ?: "—")
                    ProfileField("Должность", profile?.post ?: "—")
                    ProfileField("Отделение", profile?.department ?: "—")
                    ProfileField("Нагрудный знак", profile?.breastplate ?: "—")
                    ProfileField(
                        "Профосмотр",
                        if (profile?.medicalExamination == true) "пройден" else "не пройден"
                    )
                    ProfileField(
                        "Удостоверение",
                        ProfileDisplayFormat.idCardRange(profile?.idCardIssued, profile?.idCardUntil)
                    )
                    ProfileField("Телефон", profile?.mobilePhoneMasked ?: "—")
                    ProfileField(
                        "Ближайшая станция метро",
                        ProfileDisplayFormat.metroLabel(
                            nearestMetroLabel = profile?.nearestMetroLabel,
                            nearestMetroId = profile?.nearestMetro,
                            catalogLabel = catalog.metroLabel(profile?.nearestMetro)
                        )
                    )
                    AddressBlock("Проживание", profile?.habitation)
                    AddressBlock("Регистрация", profile?.registration)
                    EducationBlock(education)
                    VehiclesBlock(vehicles)
                }
            }

            SoftPanel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Изменить", style = MaterialTheme.typography.titleMedium)
                    SecondaryActionButton("Нагрудный знак", onClick = { section = ProfileSection.BREASTPLATE })
                    SecondaryActionButton("Профосмотр", onClick = { section = ProfileSection.MEDICAL })
                    SecondaryActionButton("Контакты и адреса", onClick = { section = ProfileSection.CONTACTS })
                    SecondaryActionButton("Образование", onClick = {
                        scope.launch {
                            when (val r = container.repository.loadEducation()) {
                                is ApiResult.Ok -> {
                                    education = r.value
                                    section = ProfileSection.EDUCATION
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    })
                    SecondaryActionButton("Транспорт", onClick = {
                        scope.launch {
                            when (val r = container.repository.loadVehicles()) {
                                is ApiResult.Ok -> {
                                    vehicles = r.value
                                    section = ProfileSection.VEHICLES
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    })
                }
            }

            when (section) {
                ProfileSection.BREASTPLATE -> BreastplateEditor(
                    initial = profile?.breastplate.orEmpty(),
                    onCancel = { section = ProfileSection.NONE },
                    onSave = { value ->
                        scope.launch {
                            when (val r = container.repository.updateProfile(
                                BotProfilePatchRequest(breastplate = value)
                            )) {
                                is ApiResult.Ok -> {
                                    profile = r.value
                                    message = "Нагрудный знак сохранён"
                                    section = ProfileSection.NONE
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    }
                )
                ProfileSection.MEDICAL -> SoftPanel {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Отметка о профосмотре", style = MaterialTheme.typography.titleMedium)
                        PrimaryActionButton("Пройден", onClick = {
                            scope.launch {
                                when (val r = container.repository.updateProfile(
                                    BotProfilePatchRequest(medicalExamination = true)
                                )) {
                                    is ApiResult.Ok -> {
                                        profile = r.value
                                        section = ProfileSection.NONE
                                    }
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        })
                        SecondaryActionButton("Не пройден", onClick = {
                            scope.launch {
                                when (val r = container.repository.updateProfile(
                                    BotProfilePatchRequest(medicalExamination = false)
                                )) {
                                    is ApiResult.Ok -> {
                                        profile = r.value
                                        section = ProfileSection.NONE
                                    }
                                    is ApiResult.Err -> error = r.userMessage()
                                }
                            }
                        })
                        SecondaryActionButton("Отмена", onClick = { section = ProfileSection.NONE })
                    }
                }
                ProfileSection.CONTACTS -> ContactsEditor(
                    profile = profile,
                    metroStations = catalog.metroStations,
                    onCancel = { section = ProfileSection.NONE },
                    onSave = { city, street, house, flat, regCity, regStreet, regHouse, regFlat, metroId ->
                        scope.launch {
                            val patch = BotProfilePatchRequest(
                                habitation = BotAddressPatch(
                                    city = city.ifBlank { null },
                                    street = street.ifBlank { null },
                                    houseNumber = house.ifBlank { null },
                                    flat = flat.ifBlank { null }
                                ),
                                registration = BotAddressPatch(
                                    city = regCity.ifBlank { null },
                                    street = regStreet.ifBlank { null },
                                    houseNumber = regHouse.ifBlank { null },
                                    flat = regFlat.ifBlank { null }
                                ),
                                nearestMetro = metroId
                            )
                            when (val r = container.repository.updateProfile(patch)) {
                                is ApiResult.Ok -> {
                                    profile = r.value
                                    message = "Адреса сохранены"
                                    section = ProfileSection.NONE
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    }
                )
                ProfileSection.EDUCATION -> EducationEditor(
                    education = education,
                    onCancel = { section = ProfileSection.NONE },
                    onSave = { req ->
                        scope.launch {
                            when (val r = container.repository.upsertEducation(req)) {
                                is ApiResult.Ok -> {
                                    education = r.value
                                    message = "Образование сохранено"
                                    section = ProfileSection.NONE
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    }
                )
                ProfileSection.VEHICLES -> VehiclesEditor(
                    vehicles = vehicles,
                    onCancel = { section = ProfileSection.NONE },
                    onCreate = { req ->
                        scope.launch {
                            when (val r = container.repository.createVehicle(req)) {
                                is ApiResult.Ok -> {
                                    vehicles = vehicles + r.value
                                    message = "Транспорт добавлен"
                                }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            when (val r = container.repository.deleteVehicle(id)) {
                                is ApiResult.Ok -> vehicles = vehicles.filterNot { it.id == id }
                                is ApiResult.Err -> error = r.userMessage()
                            }
                        }
                    }
                )
                ProfileSection.NONE -> Unit
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            SecondaryActionButton("Выйти из аккаунта", onClick = { confirmLogout = true })
            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Выйти?",
            message = "Сессия на этом устройстве будет завершена. Войти снова можно логином и паролем.",
            confirmLabel = "Выйти",
            onConfirm = {
                confirmLogout = false
                container.repository.logout()
                onLogout()
            },
            onDismiss = { confirmLogout = false }
        )
    }

    error?.let {
        ErrorRetryDialog(it, onRetry = { error = null; reload() }, onDismiss = { error = null })
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AddressBlock(title: String, address: BotAddressResponse?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        ProfileField("Город", address?.city ?: "—")
        ProfileField("Улица", address?.street ?: "—")
        ProfileField("Дом", address?.houseNumber ?: "—")
        ProfileField("Квартира", address?.flat ?: "—")
        if (!address?.summary.isNullOrBlank()) {
            ProfileField("Адрес", address?.summary.orEmpty())
        }
    }
}

@Composable
private fun EducationBlock(education: BotEducationDto?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Образование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (education == null) {
            Text("Нет данных", color = InkMuted)
        } else {
            ProfileField("Тип", education.typeLabel ?: "—")
            ProfileField("Статус", education.statusLabel ?: "—")
            ProfileField("Учебное заведение", education.nameOfInstitution ?: "—")
            ProfileField("Начало", education.started ?: "—")
            ProfileField("Окончание", education.until ?: "—")
        }
    }
}

@Composable
private fun VehiclesBlock(vehicles: List<BotVehicleItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Транспорт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (vehicles.isEmpty()) {
            Text("Нет данных", color = InkMuted)
        } else {
            vehicles.forEach { v ->
                ProfileField("Госномер", v.stateNumber ?: "—")
                ProfileField("Марка", v.brand ?: "—")
                ProfileField("Модель", v.model ?: "—")
            }
        }
    }
}

@Composable
private fun BreastplateEditor(
    initial: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }
    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Нагрудный знак", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 8) value = it.uppercase() },
                label = { Text("8 символов") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryActionButton("Сохранить", enabled = value.length == 8, onClick = { onSave(value) })
            SecondaryActionButton("Отмена", onClick = onCancel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsEditor(
    profile: BotProfileResponse?,
    metroStations: List<com.company.shtopor.vzvod.domain.IntCatalogOption>,
    onCancel: () -> Unit,
    onSave: (
        city: String,
        street: String,
        house: String,
        flat: String,
        regCity: String,
        regStreet: String,
        regHouse: String,
        regFlat: String,
        metroId: Int?
    ) -> Unit
) {
    var city by remember(profile) { mutableStateOf(profile?.habitation?.city.orEmpty()) }
    var street by remember(profile) { mutableStateOf(profile?.habitation?.street.orEmpty()) }
    var house by remember(profile) { mutableStateOf(profile?.habitation?.houseNumber.orEmpty()) }
    var flat by remember(profile) { mutableStateOf(profile?.habitation?.flat.orEmpty()) }
    var regCity by remember(profile) { mutableStateOf(profile?.registration?.city.orEmpty()) }
    var regStreet by remember(profile) { mutableStateOf(profile?.registration?.street.orEmpty()) }
    var regHouse by remember(profile) { mutableStateOf(profile?.registration?.houseNumber.orEmpty()) }
    var regFlat by remember(profile) { mutableStateOf(profile?.registration?.flat.orEmpty()) }
    var metroId by remember(profile) { mutableStateOf(profile?.nearestMetro) }

    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Контакты", style = MaterialTheme.typography.titleMedium)
            Text("Телефон меняется только через веб-админку", color = InkMuted)
            if (metroStations.isNotEmpty()) {
                val metroLabels = metroStations.map { it.label }
                val selectedLabel = metroStations.firstOrNull { it.id == metroId }?.label.orEmpty()
                Text("Ближайшая станция метро", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SimpleDropdown(
                    options = metroLabels,
                    selected = selectedLabel,
                    onSelect = { label ->
                        metroId = metroStations.firstOrNull { it.label == label }?.id
                    }
                )
            }
            Text("Проживание", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Город") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Улица") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = house, onValueChange = { house = it }, label = { Text("Дом") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = flat, onValueChange = { flat = it }, label = { Text("Квартира") }, modifier = Modifier.fillMaxWidth())
            Text("Регистрация", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = regCity, onValueChange = { regCity = it }, label = { Text("Город") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = regStreet, onValueChange = { regStreet = it }, label = { Text("Улица") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = regHouse, onValueChange = { regHouse = it }, label = { Text("Дом") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = regFlat, onValueChange = { regFlat = it }, label = { Text("Квартира") }, modifier = Modifier.fillMaxWidth())
            PrimaryActionButton("Сохранить", onClick = {
                onSave(city, street, house, flat, regCity, regStreet, regHouse, regFlat, metroId)
            })
            SecondaryActionButton("Отмена", onClick = onCancel)
        }
    }
}

@Composable
private fun EducationEditor(
    education: BotEducationDto?,
    onCancel: () -> Unit,
    onSave: (BotEducationUpsertRequest) -> Unit
) {
    var typeId by remember(education) {
        mutableStateOf(education?.typeId ?: EducationTypes.all.first().id)
    }
    var institution by remember(education) { mutableStateOf(education?.nameOfInstitution.orEmpty()) }
    var started by remember(education) { mutableStateOf(education?.started.orEmpty()) }
    var until by remember(education) { mutableStateOf(education?.until.orEmpty()) }
    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Образование", style = MaterialTheme.typography.titleMedium)
            Text("Тип", color = InkMuted)
            SimpleDropdown(
                options = EducationTypes.all.map { it.label },
                selected = EducationTypes.label(typeId)
            ) { label ->
                typeId = EducationTypes.all.first { it.label == label }.id
            }
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Учебное заведение") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = started,
                onValueChange = { started = it },
                label = { Text("Начало (гггг-мм-дд)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = until,
                onValueChange = { until = it },
                label = { Text("Окончание (гггг-мм-дд)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryActionButton("Сохранить", onClick = {
                onSave(
                    BotEducationUpsertRequest(
                        started = started.ifBlank { null },
                        until = until.ifBlank { null },
                        typeId = typeId,
                        nameOfInstitution = institution.ifBlank { null }
                    )
                )
            })
            SecondaryActionButton("Отмена", onClick = onCancel)
        }
    }
}

@Composable
private fun VehiclesEditor(
    vehicles: List<BotVehicleItem>,
    onCancel: () -> Unit,
    onCreate: (BotVehicleUpsertRequest) -> Unit,
    onDelete: (String) -> Unit
) {
    var stateNumber by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    SoftPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Транспорт", style = MaterialTheme.typography.titleMedium)
            if (vehicles.isEmpty()) {
                Text("Список пуст", color = InkMuted)
            } else {
                vehicles.forEach { v ->
                    SoftPanel {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(v.stateNumber ?: "—", style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(v.brand, v.model).joinToString(" ").ifBlank { "—" },
                                color = InkMuted
                            )
                            SecondaryActionButton("Удалить", onClick = { onDelete(v.id) })
                        }
                    }
                }
            }
            Text("Добавить", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = stateNumber,
                onValueChange = { stateNumber = it },
                label = { Text("Госномер") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Марка") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Модель") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryActionButton(
                "Добавить",
                enabled = stateNumber.isNotBlank(),
                onClick = {
                    onCreate(
                        BotVehicleUpsertRequest(
                            stateNumber = stateNumber.trim(),
                            brand = brand.ifBlank { null },
                            model = model.ifBlank { null }
                        )
                    )
                    stateNumber = ""
                    brand = ""
                    model = ""
                }
            )
            SecondaryActionButton("Закрыть", onClick = onCancel)
        }
    }
}
