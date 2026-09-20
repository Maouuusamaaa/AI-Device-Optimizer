# Policy Simulation

Pipeline:

DeviceSnapshot -> DeviceState -> DiagnosisEngine -> PolicySimulator -> DryRunPolicyProposal -> DryRunSafetyGate -> Action Engine (disabled)

PolicySimulator translates each evidence-backed Diagnosis into a DRY_RUN PolicyDecision. It does not execute commands, mutate device state, or authorize execution.

The resulting SimulatedOptimizationPlan contains diagnoses, simulated policy decisions, candidate observation-only action IDs, and the SafetyGateResult.

The safety gate remains a hard boundary. Even a valid low-risk observation candidate is not executable in this milestone because action execution is globally disabled.

Future action execution must introduce an explicit execution mode, permission checks, reversibility checks, allowlists, and pre/post measurement validation.
