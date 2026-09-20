package com.maouuusama.ai.device.optimizer.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControlledMeasurementEvaluatorTest {
    private fun sample(phase: MeasurementPhase, startup: Double, ram: Double) = MeasurementSample(1000L, phase, startupMs = startup, availableRamMb = ram)

    @Test fun comparesCommonMetricsAcrossPhases() {
        val result = ControlledMeasurementEvaluator.compare(
            listOf(sample(MeasurementPhase.BASELINE, 100.0, 2000.0), sample(MeasurementPhase.BASELINE, 120.0, 2200.0)),
            listOf(sample(MeasurementPhase.EXPERIMENT, 90.0, 2100.0), sample(MeasurementPhase.EXPERIMENT, 110.0, 2300.0))
        )
        assertEquals(2, result.baselineCount); assertEquals(2, result.experimentCount)
        assertEquals(-10.0, result.metrics.first { it.metric == "startupMs" }.absoluteDelta, 0.001)
        assertEquals(-9.090909, result.metrics.first { it.metric == "startupMs" }.percentDelta!!, 0.001)
    }

    @Test fun omitsMetricsMissingFromEitherPhase() {
        val result = ControlledMeasurementEvaluator.compare(
            listOf(sample(MeasurementPhase.BASELINE, 100.0, 2000.0)),
            listOf(MeasurementSample(2L, MeasurementPhase.EXPERIMENT, availableRamMb = 2100.0))
        )
        assertEquals(listOf("availableRamMb"), result.metrics.map { it.metric })
    }

    @Test fun zeroBaselineHasNoPercentDelta() {
        val result = ControlledMeasurementEvaluator.compare(
            listOf(MeasurementSample(1L, MeasurementPhase.BASELINE, startupMs = 0.0)),
            listOf(MeasurementSample(2L, MeasurementPhase.EXPERIMENT, startupMs = 10.0))
        )
        assertNull(result.metrics.single().percentDelta)
    }
}
