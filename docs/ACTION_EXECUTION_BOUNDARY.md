# Action Execution Boundary

The optimizer now has an explicit boundary between policy proposals and device mutation.

At the current milestone, `ActionExecutor` is intentionally non-mutating. It passes proposals through the dry-run Safety Gate and returns `DISABLED` results for proposed actions. It never invokes a privileged command and never changes device state.

Invariants:
- DRY_RUN proposal -> Safety Gate -> ActionExecutor -> no device mutation.
- A non-EXECUTED result must have `changedDeviceState=false`.
- An Action Catalog entry does not itself grant execution permission.

Any future real action must be introduced separately with explicit permission requirements, a reversible rollback path, controlled measurement, Safety Gate preconditions, post-action verification, decision-log evidence, dedicated tests/CI, and a kill switch.

Until those conditions are satisfied, action execution remains disabled.
