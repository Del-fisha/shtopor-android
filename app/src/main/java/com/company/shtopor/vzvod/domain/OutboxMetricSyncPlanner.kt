package com.company.shtopor.vzvod.domain

/**
 * Офлайн ±1 по IBD/заявкам нельзя безопасно реплеить: таймаут после успеха на Core
 * даёт дубль. При синке пушим абсолютные значения из local_metrics (идемпотентно).
 */
data class PendingOutboxOp(
    val id: String,
    val type: String,
    val shiftId: String?
)

data class LocalShiftMetrics(
    val ibdr: Int,
    val migrant: Int,
    val statements: Int,
    val claims: Int
)

data class MetricAbsoluteSync(
    val shiftId: String,
    val metrics: LocalShiftMetrics,
    val consumedOpIds: List<String>
)

data class OutboxSyncPlan(
    val absoluteMetricSyncs: List<MetricAbsoluteSync>,
    /** Операции, которые нужно выполнить поштучно (не метрики-дельты). */
    val remainingOps: List<PendingOutboxOp>
)

object OutboxMetricSyncPlanner {
    private val METRIC_DELTA_TYPES = setOf(
        "ADJUST_IBDR",
        "ADJUST_MIGRANT",
        "ADJUST_MIGRANT_AND_IBDR",
        "ADJUST_STATEMENTS",
        "ADJUST_CLAIMS"
    )

    fun isMetricDeltaType(type: String): Boolean = type in METRIC_DELTA_TYPES

    fun plan(
        pending: List<PendingOutboxOp>,
        localByShift: Map<String, LocalShiftMetrics>
    ): OutboxSyncPlan {
        val metricOps = pending.filter { isMetricDeltaType(it.type) && !it.shiftId.isNullOrBlank() }
        val metricIds = metricOps.map { it.id }.toSet()
        val nonMetric = pending.filter { it.id !in metricIds }

        val absolutes = mutableListOf<MetricAbsoluteSync>()
        val metricLeftover = mutableListOf<PendingOutboxOp>()
        for ((shiftId, ops) in metricOps.groupBy { it.shiftId!! }) {
            val local = localByShift[shiftId]
            if (local == null) {
                metricLeftover += ops
            } else {
                absolutes += MetricAbsoluteSync(
                    shiftId = shiftId,
                    metrics = local,
                    consumedOpIds = ops.map { it.id }
                )
            }
        }

        return OutboxSyncPlan(
            absoluteMetricSyncs = absolutes,
            remainingOps = nonMetric + metricLeftover
        )
    }

    /**
     * После ошибки API метрику можно класть в outbox: при flush уйдёт абсолют,
     * а не сумма дельт. (Раньше реплей ±1 после таймаута давал 5+5→13.)
     */
    fun shouldEnqueueMetricDeltaAfterError(): Boolean = true
}
