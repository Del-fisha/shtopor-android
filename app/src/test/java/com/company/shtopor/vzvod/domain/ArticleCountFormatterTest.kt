package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleCountFormatterTest {

    @Test
    fun groupsDuplicatesAsCountLines() {
        val lines = ArticleCountFormatter.formatLines(
            listOf("20.20", "18.8", "20.20", "20.20", "18.8")
        )
        assertEquals(listOf("20.20 x 3", "18.8 x 2"), lines)
    }

    @Test
    fun keepsSingleWithoutExtraNoiseOrderStable() {
        val lines = ArticleCountFormatter.formatLines(listOf("19.3", "20.21"))
        assertEquals(listOf("19.3 x 1", "20.21 x 1"), lines)
    }

    @Test
    fun fromLabeledCounts() {
        val lines = ArticleCountFormatter.fromCounts(
            listOf(
                LabeledCount("20.20", 3),
                LabeledCount("18.8", 2)
            )
        )
        assertEquals(listOf("20.20 x 3", "18.8 x 2"), lines)
    }
}
