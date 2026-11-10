package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationTypesTest {

    @Test
    fun catalog_hasFourTypesMatchingCore() {
        val ids = EducationTypes.all.map { it.id }
        assertEquals(listOf("A", "B", "C", "D"), ids)
        assertTrue(EducationTypes.all.all { it.label.isNotBlank() })
    }

    @Test
    fun label_fallsBackWhenUnknown() {
        assertEquals("—", EducationTypes.label(null))
        assertEquals("Среднее (9 классов)", EducationTypes.label("A"))
    }
}
