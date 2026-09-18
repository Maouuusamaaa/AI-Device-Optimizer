# Action Engine

The Action Engine is the security boundary between policy decisions and device mutation.

## Current mode

The engine is **dry-run only**.

It can validate a proposed action against an explicit allowlist and return PROPOSED or BLOCKED. It never executes the action.

## Allowlist

The initial low-risk actions are observation identifiers:

- observe.background_pressure
- observe.power_pressure

These identifiers do not kill processes, change settings, invoke Shizuku, or modify system state.

## Safety rules

1. An action must exist in the allowlist.
2. Disabled actions are blocked.
3. Unknown action identifiers are blocked.
4. A policy with no action produces no proposal.
5. AI output must pass through Local Policy Engine and Action Engine before any future mutation layer.
6. Dry-run remains the default until real-device benchmark evidence and rollback behavior are established.

Future privileged actions must have explicit capability declarations, permission requirements, risk classification, validation, and rollback behavior.
