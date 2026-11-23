package com.company.shtopor.vzvod.ui.annual

import java.time.LocalDate

internal enum class AnalysisHintKind {
    UNKNOWN, VALID, EXPIRED
}

internal data class AnalysisHint(val kind: AnalysisHintKind, val until: LocalDate? = null)

internal fun analysisHint(
    kind: String?,
    performed: LocalDate?,
    storedUntil: LocalDate?,
    today: LocalDate
): AnalysisHint {
    if (performed == null) {
        return AnalysisHint(AnalysisHintKind.UNKNOWN)
    }
    return when (kind) {
        "BLOOD" -> {
            val until = LocalDate.of(performed.year, 12, 31)
            if (performed.year == today.year) AnalysisHint(AnalysisHintKind.VALID, until)
            else AnalysisHint(AnalysisHintKind.EXPIRED)
        }
        "FLUOROGRAPHY" -> {
            val until = storedUntil ?: performed.plusYears(1)
            if (!today.isAfter(until)) AnalysisHint(AnalysisHintKind.VALID, until)
            else AnalysisHint(AnalysisHintKind.EXPIRED)
        }
        "ECG" -> {
            if (storedUntil == null) AnalysisHint(AnalysisHintKind.UNKNOWN)
            else if (!today.isAfter(storedUntil)) AnalysisHint(AnalysisHintKind.VALID, storedUntil)
            else AnalysisHint(AnalysisHintKind.EXPIRED)
        }
        else -> AnalysisHint(AnalysisHintKind.UNKNOWN)
    }
}
