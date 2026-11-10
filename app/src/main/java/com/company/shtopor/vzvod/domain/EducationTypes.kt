package com.company.shtopor.vzvod.domain

data class EducationTypeOption(val id: String, val label: String)

object EducationTypes {
    val all: List<EducationTypeOption> = listOf(
        EducationTypeOption("A", "Среднее (9 классов)"),
        EducationTypeOption("B", "Среднее (11 классов)"),
        EducationTypeOption("C", "Среднее специальное"),
        EducationTypeOption("D", "Университет")
    )

    fun label(typeId: String?): String =
        all.firstOrNull { it.id == typeId }?.label ?: "—"
}
