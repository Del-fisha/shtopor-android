package com.company.shtopor.vzvod.domain

/**
 * Фоновые стадии «Петровича» на вкладке Смены.
 *
 * Stage 2: уголовка ≥ 1 ИЛИ ИБДР ≥ 50 ИЛИ админок ≥ 2.
 * Stage 3: относительно снимка на момент входа в stage 2 —
 *   уголовка ≥ base+1 ИЛИ ИБДР ≥ base+50 ИЛИ админок ≥ base+1.
 *
 * Вверх — *_change после доигрывания цикла.
 * Вниз и холодный старт — сразу нужный loop без change.
 */
data class PetrovichMetrics(
    val ibdr: Int,
    val admin: Int,
    val criminal: Int
) {
    companion object {
        fun of(ibdr: Int?, admin: Int?, criminal: Int?): PetrovichMetrics =
            PetrovichMetrics(
                ibdr = (ibdr ?: 0).coerceAtLeast(0),
                admin = (admin ?: 0).coerceAtLeast(0),
                criminal = (criminal ?: 0).coerceAtLeast(0)
            )
    }
}

data class PetrovichEvaluation(
    val stage: Int,
    /** Снимок метрик при входе в stage 2; null на stage 1. */
    val stage2Baseline: PetrovichMetrics?
)

sealed class PetrovichPlayback {
    data class Loop(val stage: Int) : PetrovichPlayback()
    data class FinishCurrentThenChange(val fromStage: Int, val toStage: Int) : PetrovichPlayback()
    data class PlayChangeOnce(val fromStage: Int, val toStage: Int) : PetrovichPlayback()
}

object PetrovichStageLogic {
    fun meetsStage2(m: PetrovichMetrics): Boolean =
        m.criminal >= 1 || m.ibdr >= 50 || m.admin >= 2

    fun meetsStage3(m: PetrovichMetrics, stage2Baseline: PetrovichMetrics): Boolean =
        m.criminal >= stage2Baseline.criminal + 1 ||
            m.ibdr >= stage2Baseline.ibdr + 50 ||
            m.admin >= stage2Baseline.admin + 1

    /**
     * @param storedBaseline сохранённый снимок входа в stage 2 (может быть null).
     * @param captureBaselineIfMissing если stage≥2 и baseline нет — зафиксировать текущие метрики
     *        (живой переход 1→2) либо восстановить для холодного старта.
     */
    fun evaluate(
        metrics: PetrovichMetrics,
        storedBaseline: PetrovichMetrics?
    ): PetrovichEvaluation {
        if (!meetsStage2(metrics)) {
            return PetrovichEvaluation(stage = 1, stage2Baseline = null)
        }
        val baseline = storedBaseline ?: metrics
        return if (meetsStage3(metrics, baseline)) {
            PetrovichEvaluation(stage = 3, stage2Baseline = baseline)
        } else {
            PetrovichEvaluation(stage = 2, stage2Baseline = baseline)
        }
    }

    /** Холодный старт / возврат в приложение: сразу loop целевой стадии. */
    fun initialPlayback(stage: Int): PetrovichPlayback =
        PetrovichPlayback.Loop(stage.coerceIn(1, 3))

    fun loopAsset(stage: Int): String = when (stage.coerceIn(1, 3)) {
        1 -> "petrovich/petrovich_1.gif"
        2 -> "petrovich/petrovich_2.gif"
        else -> "petrovich/petrovich_3.gif"
    }

    fun changeAsset(from: Int, to: Int): String? = when {
        from == 1 && to == 2 -> "petrovich/petrovich_1_change.gif"
        from == 2 && to == 3 -> "petrovich/petrovich_2_change.gif"
        else -> null
    }

    fun resolve(playback: PetrovichPlayback, targetStage: Int): PetrovichPlayback {
        val target = targetStage.coerceIn(1, 3)
        return when (playback) {
            is PetrovichPlayback.Loop -> when {
                target > playback.stage -> {
                    val next = (playback.stage + 1).coerceAtMost(target)
                    PetrovichPlayback.FinishCurrentThenChange(fromStage = playback.stage, toStage = next)
                }
                target < playback.stage -> PetrovichPlayback.Loop(target)
                else -> playback
            }
            is PetrovichPlayback.FinishCurrentThenChange -> when {
                target < playback.toStage && target <= playback.fromStage ->
                    PetrovichPlayback.Loop(target)
                target < playback.toStage -> PetrovichPlayback.Loop(target)
                else -> playback
            }
            is PetrovichPlayback.PlayChangeOnce -> when {
                target < playback.toStage -> PetrovichPlayback.Loop(target)
                else -> playback
            }
        }
    }

    fun onLoopCycleEnded(playback: PetrovichPlayback): PetrovichPlayback = when (playback) {
        is PetrovichPlayback.FinishCurrentThenChange ->
            PetrovichPlayback.PlayChangeOnce(playback.fromStage, playback.toStage)
        else -> playback
    }

    fun onChangeEnded(playback: PetrovichPlayback): PetrovichPlayback = when (playback) {
        is PetrovichPlayback.PlayChangeOnce -> PetrovichPlayback.Loop(playback.toStage)
        else -> playback
    }

    fun assetPath(playback: PetrovichPlayback): String = when (playback) {
        is PetrovichPlayback.Loop -> loopAsset(playback.stage)
        is PetrovichPlayback.FinishCurrentThenChange -> loopAsset(playback.fromStage)
        is PetrovichPlayback.PlayChangeOnce ->
            changeAsset(playback.fromStage, playback.toStage) ?: loopAsset(playback.toStage)
    }

    fun loopsInfinitely(playback: PetrovichPlayback): Boolean =
        playback is PetrovichPlayback.Loop
}
