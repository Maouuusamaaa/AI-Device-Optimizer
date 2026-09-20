# Stage 2 Controlled Experiment Framework

Stage 2 formalizes the protocol for moving from passive monitoring toward
real-device optimization experiments without prematurely enabling mutation.

Each experiment declares a unique experiment ID, one candidate action with
safety metadata, baseline/intervention/post phases, an explicit measurement
plan, and an execution state.

## Safety contract

The current implementation requires:

- execution.enabled=false
- execution.deviceMutationAllowed=false
- policySelectionAllowed=false
- executionAllowed=false

The protocol therefore cannot authorize an Android mutation. The intervention
phase is a reserved experimental slot, not permission to execute an action.

The existing controlled-observation runner can populate the three measurement
phases while this contract remains in force.

## What this milestone establishes

This is a protocol boundary, not an optimizer. It provides a stable input
contract for the future real Action Engine and makes measurement requirements
explicit before implementation.

No effectiveness, causal, ranking, or policy-selection claim is generated.

## Next gate before real mutation

Before enabling an actual action, the repository should add:

1. a concrete action implementation;
2. an allowlist entry with explicit permission and risk metadata;
3. device-state preconditions;
4. rollback verification;
5. an intervention-specific measurement adapter;
6. a kill switch and failure recovery path;
7. real-device validation with the action disabled by default.

Only after those checks should an action-specific experiment be considered.


## Runner integration

The repository now provides `scripts/controlled-experiment-runner.py`. It connects the Stage 2 protocol contract to the existing read-only Rish measurement routine and the controlled-observation analyzer.

The runner requires a validated protocol with:

- `execution.enabled=false`
- `execution.deviceMutationAllowed=false`
- `policySelectionAllowed=false`
- `executionAllowed=false`

It then executes the same read-only measurement routine in three phases:

1. `baseline`
2. `intervention`
3. `post`

The `intervention` phase is deliberately a reserved slot. The runner does not call the Action Engine, does not change Android state, and does not grant permissions.

A run produces this evidence structure:

```text
stage2-run/
├── manifest.json
├── baseline/
│   ├── rish-baseline-*.txt
│   ├── rish-baseline-*.json
│   └── rish-baseline-*.csv
├── intervention/
│   ├── rish-baseline-*.txt
│   ├── rish-baseline-*.json
│   └── rish-baseline-*.csv
├── post/
│   ├── rish-baseline-*.txt
│   ├── rish-baseline-*.json
│   └── rish-baseline-*.csv
├── controlled-report.json
└── controlled-report.md
```

The manifest records the experiment ID, candidate action ID, phase purposes, and the non-authorizing execution state. The analyzer validates device identity, sample counts, and reported statistics before producing descriptive comparisons.

Example protocol: `docs/examples/stage2-controlled-experiment.json`.

### Execution

Validate a definition without touching the device:

```bash
python3 scripts/experiment-protocol.py docs/examples/stage2-controlled-experiment.json
```

Run the controlled physical experiment only when a real Rish/Termux environment is intentionally being measured:

```bash
python3 scripts/controlled-experiment-runner.py docs/examples/stage2-controlled-experiment.json
```

The runner remains measurement-only. A successful run is evidence that the protocol and measurement pipeline executed; it is not evidence that an optimization improved the device.
