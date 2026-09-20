# Persistent Learning History

The adaptive-learning history now has a versioned local persistence boundary.

Schema:
- binary format with magic header
- schemaVersion: 1
- ordered entries
- each entry contains timestamp, conditions, diagnoses, DRY_RUN decisions, simulated/blocked actions, and descriptive measurement reports.

Persistence uses the application's private files directory, so no storage permission is required. The store is bounded to 1000 entries by default.

Integrity rules:
- unsupported schema versions are rejected rather than guessed;
- malformed history is surfaced as an error rather than silently discarded;
- writes use a temporary file followed by rename;
- only DRY_RUN decisions and descriptive_only measurement reports satisfy the history contract.

This remains a data collection boundary. Persistence does not execute actions, authorize device mutation, create rewards, rank actions, or claim causal effectiveness.

Before historical data can influence optimization, a later milestone should define versioned features, explicit outcome labels, minimum sample requirements, confounder handling, validation/holdout rules, and a safety review.
