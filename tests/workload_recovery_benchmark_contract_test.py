#!/usr/bin/env python3
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
BENCHMARK = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/WorkloadRecoveryBenchmark.kt"
WRITER = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/WorkloadRecoveryBenchmarkJsonWriter.kt"

class WorkloadRecoveryBenchmarkContractTest(unittest.TestCase):
    def test_protocol_has_three_phases(self):
        text = BENCHMARK.read_text(encoding="utf-8")
        for phase in ("baseline", "workload", "recovery"):
            self.assertIn('onPhase("' + phase + '")', text)
        self.assertIn("DEFAULT_BASELINE_DURATION_MS = 60_000L", text)
        self.assertIn("DEFAULT_WORKLOAD_DURATION_MS = 5 * 60_000L", text)
        self.assertIn("DEFAULT_RECOVERY_DURATION_MS = 2 * 60_000L", text)

    def test_scheduler_uses_collection_aware_deadline(self):
        text = BENCHMARK.read_text(encoding="utf-8")
        self.assertIn("nextSampleNs = sampleStartedNs + intervalMs * 1_000_000L", text)
        self.assertNotIn("Thread.sleep(intervalMs)", text)

    def test_writer_preserves_phase_and_pss(self):
        text = WRITER.read_text(encoding="utf-8")
        self.assertIn('.put("phase", wrapped.phase)', text)
        self.assertIn('.put("pssKb", process.pssKb)', text)
        self.assertIn('protocol", "workload_recovery_memory_observation"', text)

if __name__ == "__main__":
    unittest.main()