# Diagnosis Engine

The Diagnosis Engine is the measurement-to-classification boundary for the optimizer.

Flow:

DeviceSnapshot -> DeviceState -> DiagnosisEngine -> Diagnosis -> Local Policy / Dry Run -> Safety Gate -> Action Engine

The engine is deliberately measurement-only. It does not execute commands, mutate device state, grant permissions, or bypass the safety gate.

Each diagnosis contains:
- a stable condition ID;
- severity;
- confidence in the classification;
- concrete evidence from the measured DeviceState;
- an optional observation-only candidate action.

Current conditions mirror the experimentally defined local thresholds:
- memory.critical: available RAM < 1000 MiB.
- memory.pressure: available RAM <= 1500 MiB and not critical.
- battery.low: battery <= 20% and the device is not charging.
- device.normal: no configured condition is present.

Confidence is intentionally conservative and currently reflects only whether the primary measurement required for the classification is valid. It is not a probability of future device behavior.

No condition in this milestone authorizes an optimization action. Candidate actions remain observation-only and are still subject to the dry-run safety gate.

This milestone does not infer thermal, storage, network, or gaming problems merely because those fields exist. Additional diagnoses should be added only when a measurable rule and validation plan exist.
