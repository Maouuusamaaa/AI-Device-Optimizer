# Handoff Protocol

A handoff is required when governed work is incomplete and another person/AI/chat must resume it, or when a significant experiment, release, incident, or blocker remains active.

Use HANDOFF-#### and record as-of commit, branch, task IDs, scope/non-goals, completed work/evidence, changed files, last known green CI, failures/negative results, blockers/risks/uncertainties, assumptions, pending approvals, next safe step, stop conditions, and required reading.

The receiver verifies the referenced commit/state and acknowledges or rejects the handoff. Rejected handoffs record why. Handoffs are historical records and are superseded rather than silently edited away.
