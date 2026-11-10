package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetrovichStageLogicTest {

    @Test
    fun evaluate_stage1_byDefault() {
        val e = PetrovichStageLogic.evaluate(PetrovichMetrics(2, 1, 0), storedBaseline = null)
        assertEquals(1, e.stage)
        assertNull(e.stage2Baseline)
    }

    @Test
    fun evaluate_example_criminalOpensStage2_andCapturesBaseline() {
        // ИБДР=2, Адм=1, УП=1 → stage 2, baseline = текущие метрики
        val m = PetrovichMetrics(ibdr = 2, admin = 1, criminal = 1)
        val e = PetrovichStageLogic.evaluate(m, storedBaseline = null)
        assertEquals(2, e.stage)
        assertEquals(m, e.stage2Baseline)
    }

    @Test
    fun evaluate_stage3_relativeToBaseline_ibdrPlus50() {
        val baseline = PetrovichMetrics(ibdr = 2, admin = 1, criminal = 1)
        val e = PetrovichStageLogic.evaluate(
            PetrovichMetrics(ibdr = 52, admin = 1, criminal = 1),
            storedBaseline = baseline
        )
        assertEquals(3, e.stage)
        assertEquals(baseline, e.stage2Baseline)
    }

    @Test
    fun evaluate_stage3_relative_adminPlus1_or_criminalPlus1() {
        val baseline = PetrovichMetrics(ibdr = 2, admin = 1, criminal = 1)
        assertEquals(
            3,
            PetrovichStageLogic.evaluate(
                PetrovichMetrics(2, 2, 1),
                baseline
            ).stage
        )
        assertEquals(
            3,
            PetrovichStageLogic.evaluate(
                PetrovichMetrics(2, 1, 2),
                baseline
            ).stage
        )
    }

    @Test
    fun evaluate_keepsStage2_whenBelowRelativeThresholds() {
        val baseline = PetrovichMetrics(ibdr = 2, admin = 1, criminal = 1)
        val e = PetrovichStageLogic.evaluate(
            PetrovichMetrics(ibdr = 51, admin = 1, criminal = 1),
            storedBaseline = baseline
        )
        assertEquals(2, e.stage)
    }

    @Test
    fun evaluate_downgradeClearsBaseline() {
        val e = PetrovichStageLogic.evaluate(
            PetrovichMetrics(ibdr = 0, admin = 0, criminal = 0),
            storedBaseline = PetrovichMetrics(2, 1, 1)
        )
        assertEquals(1, e.stage)
        assertNull(e.stage2Baseline)
    }

    @Test
    fun initialPlayback_isLoopWithoutChange() {
        assertEquals(PetrovichPlayback.Loop(2), PetrovichStageLogic.initialPlayback(2))
        assertEquals(PetrovichPlayback.Loop(3), PetrovichStageLogic.initialPlayback(3))
    }

    @Test
    fun resolve_upgradeQueuesFinishThenChange() {
        val next = PetrovichStageLogic.resolve(PetrovichPlayback.Loop(1), targetStage = 2)
        assertEquals(PetrovichPlayback.FinishCurrentThenChange(1, 2), next)
    }

    @Test
    fun resolve_downgradeImmediateWithoutChange() {
        assertEquals(
            PetrovichPlayback.Loop(2),
            PetrovichStageLogic.resolve(PetrovichPlayback.Loop(3), targetStage = 2)
        )
        assertEquals(
            PetrovichPlayback.Loop(1),
            PetrovichStageLogic.resolve(PetrovichPlayback.Loop(2), targetStage = 1)
        )
    }

    @Test
    fun changeAssets_onlyForUpgrades() {
        assertEquals("petrovich/petrovich_1_change.gif", PetrovichStageLogic.changeAsset(1, 2))
        assertEquals("petrovich/petrovich_2_change.gif", PetrovichStageLogic.changeAsset(2, 3))
        assertNull(PetrovichStageLogic.changeAsset(2, 1))
    }

    @Test
    fun onLoopThenChange_flow() {
        val finish = PetrovichPlayback.FinishCurrentThenChange(1, 2)
        val change = PetrovichStageLogic.onLoopCycleEnded(finish)
        assertEquals(PetrovichPlayback.PlayChangeOnce(1, 2), change)
        assertEquals(PetrovichPlayback.Loop(2), PetrovichStageLogic.onChangeEnded(change))
        assertTrue(PetrovichStageLogic.loopsInfinitely(PetrovichPlayback.Loop(2)))
    }
}
