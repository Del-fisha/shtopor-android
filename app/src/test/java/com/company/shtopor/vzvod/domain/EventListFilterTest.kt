package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EventListFilterTest {

    private val events = listOf(
        SampleEvent("A", shiftOfDepartment = 1),
        SampleEvent("B", shiftOfDepartment = 2),
        SampleEvent("C", shiftOfDepartment = 1),
        SampleEvent("D", shiftOfDepartment = null)
    )

    @Test
    fun filter_all_keepsEverythingSortedByCallerOrder() {
        val filtered = EventListFilter.filter(events, EventDepartmentFilter.ALL) { it.shiftOfDepartment }
        assertEquals(listOf("A", "B", "C", "D"), filtered.map { it.name })
    }

    @Test
    fun filter_department1() {
        val filtered = EventListFilter.filter(events, EventDepartmentFilter.DEP_1) { it.shiftOfDepartment }
        assertEquals(listOf("A", "C"), filtered.map { it.name })
    }

    @Test
    fun filter_department2() {
        val filtered = EventListFilter.filter(events, EventDepartmentFilter.DEP_2) { it.shiftOfDepartment }
        assertEquals(listOf("B"), filtered.map { it.name })
    }

    @Test
    fun shiftCaption_formatsDepartmentNumber() {
        assertEquals("смена 1 отделения", EventListFilter.shiftCaption(1))
        assertEquals("смена 2 отделения", EventListFilter.shiftCaption(2))
        assertEquals("смена — отделения", EventListFilter.shiftCaption(null))
    }

    private data class SampleEvent(val name: String, val shiftOfDepartment: Int?)
}
