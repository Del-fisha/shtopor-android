package com.company.shtopor.vzvod.domain

/**
 * Пока по смене есть несинхронизированные операции, локальные счётчики
 * нельзя затирать ответом сервера (иначе UI «откатывается», а очередь ещё не ушла).
 */
object LocalMetricsSyncPolicy {
    fun shouldOverwriteLocalFromServer(pendingOpsForShift: Int): Boolean =
        pendingOpsForShift <= 0
}
