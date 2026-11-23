package com.company.shtopor.vzvod.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.data.remote.dto.BotAddressResponse
import com.company.shtopor.vzvod.data.remote.dto.BotEducationDto
import com.company.shtopor.vzvod.data.remote.dto.BotProfileResponse
import com.company.shtopor.vzvod.data.remote.dto.BotVehicleItem
import com.company.shtopor.vzvod.domain.AppCatalog
import com.company.shtopor.vzvod.domain.ProfileDisplayFormat
import com.company.shtopor.vzvod.ui.theme.InkMuted
import com.company.shtopor.vzvod.ui.theme.VzvodSans

private val MedPassed = Color(0xFF2E7D32)
private val MedFailed = Color(0xFFC62828)
private val MissingInk = InkMuted.copy(alpha = 0.72f)

@Composable
fun EmployeeCard(
    profile: BotProfileResponse?,
    education: BotEducationDto?,
    vehicles: List<BotVehicleItem>,
    catalog: AppCatalog
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Сотрудник", fontWeight = FontWeight.Bold)
        Text(profile?.displayName ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        if (profile?.displayName.isNullOrBlank()) {
            MissingText()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val passed = profile?.medicalExamination == true
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (passed) MedPassed else MedFailed)
            )
            Text("Профосмотр", fontWeight = FontWeight.Bold)
            Text(if (passed) "Пройден" else "Не пройден")
        }

        BlockGap()
        FactLine("Дата рождения", ProfileDisplayFormat.birthLine(profile?.dateOfBirth).takeUnless { it == "—" })
        FactLine("Должность", profile?.post)
        FactLine("Звание", profile?.rank)
        FactLine(
            "Отделение",
            ProfileDisplayFormat.departmentNumbered(profile?.department, profile?.departmentNumber)
                .takeUnless { it == "—" }
        )
        FactLine("Нагрудный знак", profile?.breastplate)
        FactLine("БШ", profile?.token)
        FactLine("СПЛ", profile?.spl)
        val issued = profile?.idCardIssued
        val until = profile?.idCardUntil
        FactLine(
            "Удостоверение",
            if (issued.isNullOrBlank() && until.isNullOrBlank()) {
                null
            } else {
                ProfileDisplayFormat.idCardRange(issued, until)
            }
        )
        FactLine("Выслуга", profile?.seniorityLabel)
        FactLine("Телефон", ProfileDisplayFormat.phone(profile?.mobilePhone))
        FactLine(
            "Ближайшая станция метро",
            ProfileDisplayFormat.metroLabel(
                nearestMetroLabel = profile?.nearestMetroLabel,
                nearestMetroId = profile?.nearestMetro,
                catalogLabel = catalog.metroLabel(profile?.nearestMetro)
            ).takeUnless { it == "—" }
        )

        BlockGap()
        AddressBlock("Проживание", profile?.habitation)
        BlockGap()
        AddressBlock("Регистрация", profile?.registration)
        BlockGap()
        EducationBlock(education)
        BlockGap()
        VehiclesBlock(vehicles)
    }
}

@Composable
private fun BlockGap() {
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun FactLine(label: String, value: String?) {
    val missing = value.isNullOrBlank() || value == "—"
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            label,
            fontFamily = VzvodSans,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        if (missing) MissingText() else Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MissingText() {
    Text(
        "Нет данных",
        color = MissingInk,
        fontStyle = FontStyle.Italic
    )
}

@Composable
private fun AddressBlock(title: String, address: BotAddressResponse?) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        val line = ProfileDisplayFormat.addressLine(
            city = address?.city,
            street = address?.street,
            body = address?.body,
            house = address?.houseNumber,
            flat = address?.flat
        )
        if (line == "—") MissingText() else Text(line)
    }
}

@Composable
private fun EducationBlock(education: BotEducationDto?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Последнее образование", fontWeight = FontWeight.Bold)
        if (education == null) {
            MissingText()
        } else {
            FactLine("Тип", education.typeLabel)
            FactLine("Статус", education.statusLabel)
            val span = ProfileDisplayFormat.educationSpan(education.started, education.until)
            val name = education.nameOfInstitution?.takeIf { it.isNotBlank() }
            FactLine(
                "Учебное заведение",
                when {
                    name == null && span == null -> null
                    name == null -> span
                    span == null -> name
                    else -> "$name ($span)"
                }
            )
        }
    }
}

@Composable
private fun VehiclesBlock(vehicles: List<BotVehicleItem>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Транспорт", fontWeight = FontWeight.Bold)
        if (vehicles.isEmpty()) {
            MissingText()
        } else {
            vehicles.forEach { vehicle ->
                FactLine("Госномер", vehicle.stateNumber)
                FactLine("Марка", vehicle.brand)
                FactLine("Модель", vehicle.model)
            }
        }
    }
}
