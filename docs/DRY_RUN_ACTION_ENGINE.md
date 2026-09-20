# Dry-Run Action Engine

This milestone is simulation-only. DryRunActionEngine converts policy proposals into action simulations without invoking Android APIs, shell commands, Shizuku/Rish, or mutating operations.

It reports allowlist membership, risk/permission/reversibility preconditions, measurement plan, rollback plan, and SIMULATED or BLOCKED status.

A gate with allowed=true is rejected. Therefore this class cannot authorize execution.

A future mutation path requires explicit authorization, preconditions, post-action measurement, timeout handling, and rollback verification.
