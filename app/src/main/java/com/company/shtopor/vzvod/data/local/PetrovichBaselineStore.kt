package com.company.shtopor.vzvod.data.local

import android.content.Context
import com.company.shtopor.vzvod.domain.PetrovichMetrics

/**
 * Снимок метрик на момент входа в stage 2 (для относительных порогов stage 3).
 */
class PetrovichBaselineStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("petrovich_baseline", Context.MODE_PRIVATE)

    fun load(shiftId: String): PetrovichMetrics? {
        if (shiftId.isBlank()) return null
        val key = key(shiftId)
        if (!prefs.contains("$key.ibdr")) return null
        return PetrovichMetrics(
            ibdr = prefs.getInt("$key.ibdr", 0),
            admin = prefs.getInt("$key.admin", 0),
            criminal = prefs.getInt("$key.criminal", 0)
        )
    }

    fun save(shiftId: String, baseline: PetrovichMetrics) {
        if (shiftId.isBlank()) return
        val key = key(shiftId)
        prefs.edit()
            .putInt("$key.ibdr", baseline.ibdr)
            .putInt("$key.admin", baseline.admin)
            .putInt("$key.criminal", baseline.criminal)
            .apply()
    }

    fun clear(shiftId: String) {
        if (shiftId.isBlank()) return
        val key = key(shiftId)
        prefs.edit()
            .remove("$key.ibdr")
            .remove("$key.admin")
            .remove("$key.criminal")
            .apply()
    }

    private fun key(shiftId: String) = "shift_$shiftId"
}
