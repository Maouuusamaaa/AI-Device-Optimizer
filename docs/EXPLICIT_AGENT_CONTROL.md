# Explicit Background Agent Control

This follow-up hardens the runtime lifecycle of the read-only background monitoring agent.

## Behavior

- The agent remains enabled by default for backward compatibility.
- MainActivity exposes an explicit Stop background agent control.
- Stopping writes a persistent local preference and calls Android stopService() for the optimizer service.
- Reopening MainActivity does not automatically restart the service while the user-stop preference is set.
- The same control becomes Start background agent and restores the normal foreground-service startup path.
- The existing paired benchmark lifecycle retains its existing redelivery behavior; this change does not add a second restart mechanism.

## Safety boundary

This control only starts or stops the optimizer's own monitoring service. It does not mutate SmartPanel/Game Mode state, other applications, permissions, or device performance settings.

## Runtime validation required

1. Start agent -> process exists and telemetry continues.
2. Stop agent -> process exits and monitoring notification disappears.
3. Reopen MainActivity -> stopped state persists and the service does not restart.
4. Start agent again -> process and telemetry resume.
5. Shizuku remains read-only and reconnect behavior is tested independently.

CI compilation is necessary but cannot substitute for these device lifecycle checks.