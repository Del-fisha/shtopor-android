package com.company.shtopor.vzvod.notify

/**
 * Навигация из шторки уведомления активной смены.
 */
sealed class ShiftNavCommand {
    data class OpenAdmin(val shiftId: String) : ShiftNavCommand()
    data class OpenCriminal(val shiftId: String) : ShiftNavCommand()
    data class OpenEndShift(val shiftId: String) : ShiftNavCommand()
}
