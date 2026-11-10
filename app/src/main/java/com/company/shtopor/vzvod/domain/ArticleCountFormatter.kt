package com.company.shtopor.vzvod.domain

data class LabeledCount(
    val label: String,
    val count: Int
)

object ArticleCountFormatter {
    fun formatLines(articleLabels: List<String>): List<String> {
        if (articleLabels.isEmpty()) return emptyList()
        val order = linkedMapOf<String, Int>()
        for (label in articleLabels) {
            val key = label.trim()
            if (key.isEmpty()) continue
            order[key] = (order[key] ?: 0) + 1
        }
        return order.map { (label, count) -> "$label x $count" }
    }

    fun fromCounts(counts: List<LabeledCount>): List<String> =
        counts
            .filter { it.label.isNotBlank() && it.count > 0 }
            .map { "${it.label.trim()} x ${it.count}" }
}
