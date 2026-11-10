package com.company.shtopor.vzvod.domain

object CalendarMonthSwipe {
    const val THRESHOLD_PX = 80f

    /** +1 = следующий месяц, -1 = предыдущий, 0 = без смены. */
    fun monthDeltaForDrag(accumulatedDx: Float): Int = when {
        accumulatedDx <= -THRESHOLD_PX -> 1
        accumulatedDx >= THRESHOLD_PX -> -1
        else -> 0
    }
}
