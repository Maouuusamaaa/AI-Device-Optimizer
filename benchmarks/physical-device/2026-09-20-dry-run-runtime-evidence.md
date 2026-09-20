# Dry-Run Runtime Evidence — 2026-09-20

## Scope

This record documents physical-device runtime validation of the connected read-only pipeline on the real Android development device.

The validation covers:

DeviceSnapshot → DeviceState → LocalPolicyEngine → DryRunPolicyProposal

No privileged mutation or Action Engine execution was enabled.

## Environment

- Device: ITEL itel P661N
- Android API: 33
- Application package: com.maouuusama.ai.device.optimizer
- Shizuku provider: shizuku
- Git branch: feat/connect-monitor-policy-dry-run
- Tested head for the multi-sample run: 2c4f9b16bd29e4c92da643c1ffcbffee1ef4a638
- APK installation: successful
- Unit tests: successful
- Debug APK assembly: successful

## Multi-Sample Runtime Observation

Three persisted runtime samples were captured after restarting the application. The samples span 77,488 ms in total.

| Sample | Timestamp (ms) | Interval from previous | Telemetry | Provider | System available RAM (KB) | App available RAM (MB) | Swap used (KB) | CPU utilization | Policy | Mode | Action execution |
|---|---:|---:|---|---|---:|---:|---:|---:|---|---|---|
| 1 | 1789875276984 | — | AVAILABLE | shizuku | 2300380 | 2313 | 1073152 | 56.736046 | device.normal | DRY_RUN | false |
| 2 | 1789875329749 | 52,765 ms | AVAILABLE | shizuku | 2220780 | 2191 | 1075456 | 52.55924 | device.normal | DRY_RUN | false |
| 3 | 1789875354472 | 24,723 ms | AVAILABLE | shizuku | 2201596 | 2171 | 1093120 | 53.820564 | device.normal | DRY_RUN | false |

All three samples also reported:

- last_system_process_count: 100
- last_system_telemetry_status: AVAILABLE
- last_system_telemetry_provider: shizuku
- last_policy_id: device.normal
- last_policy_severity: INFO
- last_policy_mode: DRY_RUN
- last_action_execution_allowed: false
- last_has_action_proposal: false
- last_proposed_action_ids: empty

The first-to-third sample timestamp span is 77,488 ms (~77.5 seconds).

## Interpretation

The three physical samples consistently observed successful read-only system telemetry through Shizuku and successful propagation through the local dry-run policy pipeline.

The policy state remained device.normal / INFO across all three samples, and no action proposal was emitted.

last_action_execution_allowed=false remained enforced across all samples. This confirms the measurement-only boundary was preserved during the observed runtime window.

CPU, available RAM, and swap usage varied between samples. These are descriptive observations of device state and are not evidence that the application caused those changes.

This evidence does not demonstrate optimization benefit, performance improvement, or causation. It validates the observed runtime telemetry and dry-run policy path across three samples.

Three samples are sufficient for this runtime observation milestone but are not sufficient to establish long-term stability or benchmark an optimization effect.

## Validation Commands

The physical APK was installed with:

rish -c "pm install -r /data/local/tmp/app-debug-latest.apk"

The application was restarted with:

rish -c "am force-stop com.maouuusama.ai.device.optimizer"

rish -c "am start -n com.maouuusama.ai.device.optimizer/.MainActivity"

The persisted runtime state was inspected with:

rish -c "run-as com.maouuusama.ai.device.optimizer cat shared_prefs/optimizer_agent.xml"

Three persisted samples were captured at approximately 10-second observation steps after startup.

## Status

Confirmed for this three-sample observation:

- Build: PASS
- Unit tests: PASS
- APK installation: PASS
- Shizuku telemetry: AVAILABLE in all samples
- Read-only telemetry collection: PASS in all samples
- Monitor-to-policy connection: PASS in all samples
- Dry-run boundary: ENFORCED in all samples
- Action execution: DISABLED in all samples
- Action proposal: NONE in all samples

Next measurement step: use controlled repeated measurements if the project needs a stability claim or an optimization-effect benchmark.