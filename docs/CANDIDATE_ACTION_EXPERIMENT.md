# Candidate Action Experiment Contract

The first candidate is observe.remeasure_baseline. It is intentionally observation-only: it repeats controlled measurements and does not modify Android state.

Every future mutating candidate must declare preconditions, rollback, verification, a kill switch, permissions, and measurement requirements before implementation.

The candidate contract rejects executionEnabled=true. Enabling real execution is therefore a separate milestone and cannot happen accidentally through configuration.

Current candidate flow:

1. confirm telemetry availability;
2. record workload and device/build identity;
3. collect controlled baseline samples;
4. evaluate the candidate specification;
5. collect repeat/post samples;
6. verify invariants and preserve raw evidence;
7. keep execution disabled.

No privileged command is invoked by this contract.
