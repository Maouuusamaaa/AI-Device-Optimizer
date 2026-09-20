# AI Cloud Foundation Contracts

Stage 3 defines the boundary between the local Android optimizer and cloud AI.

The cloud may observe normalized telemetry and produce an advisory recommendation. It must never receive an execution capability and must never bypass local Policy Simulation, Safety Gate, or Action Engine.

Flow: DeviceSnapshot -> CloudObservation -> AI Advisor -> CloudRecommendation -> Local Policy Simulation -> Safety Gate -> Action Engine.

Contracts are model-agnostic, schema-versioned, evidence-referenced, uncertainty-aware, allowlist-based, and offline-compatible.
