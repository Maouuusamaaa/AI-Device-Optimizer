# Dry-Run Runtime Evidence — 2026-09-20

## Scope

This record documents one successful physical-device runtime validation of the connected read-only pipeline on the real Android development device.

The validation covers:

DeviceSnapshot → DeviceState → LocalPolicyEngine → DryRunPolicyProposal

No privileged mutation or Action Engine execution was enabled.

## Environment

- Device: ITEL itel P661N
- Android API: 33
- Application package: com.maouuusama.ai.device.optimizer
- Shizuku provider: shizuku
- Git branch: feat/connect-monitor-policy-dry-run
- Tested head: 2c4f9b16bd29e4c92da643c1ffcbffee1ef4a638
- APK installation: successful
- Unit tests: successful
- Debug APK assembly: successful

## Runtime Observation

The application persisted the following values after startup and monitoring:

- last_system_telemetry_status: AVAILABLE
- last_system_telemetry_provider: shizuku
- last_system_mem_available_kb: 2334228
- last_system_process_count: 100
- last_system_swap_used_kb: 1050112
- last_system_cpu_utilization: 51.701584
- last_total_ram_mb: 5634
- last_available_ram_mb: 2304
- last_policy_id: device.normal
- last_policy_severity: INFO
- last_policy_mode: DRY_RUN
- last_action_execution_allowed: false
- last_has_action_proposal: false
- last_proposed_action_ids: empty
- last_top_process: AI Device Optimizer (26179 KB PSS)

## Interpretation

The runtime evidence confirms that the application successfully obtained read-only system telemetry through Shizuku and propagated the resulting device snapshot into the local dry-run policy pipeline.

The observed policy state was device.normal, so this particular sample did not generate a proposed action.

last_action_execution_allowed=false confirms that the measurement-only safety boundary remained enforced during the physical-device test.

This evidence does not demonstrate optimization benefit or causation. It only validates successful telemetry collection and policy evaluation for the observed runtime sample.

## Validation Commands

The physical APK was installed with:

rish -c "pm install -r /data/local/tmp/app-debug-latest.apk"

The application was restarted with:

rish -c "am force-stop com.maouuusama.ai.device.optimizer"

rish -c "am start -n com.maouuusama.ai.device.optimizer/.MainActivity"

The persisted runtime state was inspected with:

rish -c "run-as com.maouuusama.ai.device.optimizer cat shared_prefs/optimizer_agent.xml"

## Status

Confirmed for this observation:

- Build: PASS
- Unit tests: PASS
- APK installation: PASS
- Shizuku telemetry: AVAILABLE
- Read-only telemetry collection: PASS
- Monitor-to-policy connection: PASS
- Dry-run boundary: ENFORCED
- Action execution: DISABLED

Next measurement step: repeat this validation across multiple independent samples and preserve the same device identity, workload, and comparable conditions before treating the runtime path as stable.