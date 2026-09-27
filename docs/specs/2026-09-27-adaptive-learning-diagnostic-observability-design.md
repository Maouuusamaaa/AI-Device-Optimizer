# Adaptive Learning Runtime Diagnostic Design

**Date:** 2026-09-27
**Target milestone:** 0.1.18 follow-up
**Status:** Approved for implementation

## 1. Goal

Expose a read-only Android diagnostic for the 0.1.18 Adaptive Learning Runtime so a device operator can verify that the persistent Knowledge State is being loaded, incrementally processed, and persisted without granting any new execution authority.

The diagnostic supplements the existing descriptive decision-history summary. It must distinguish source history from materialized adaptive-learning Knowledge State.

## 2. Scope

The diagnostic reports:
- Knowledge State persistence availability.
- processed evidence count;
- abstention count;
- current advisory candidate count;
- current Knowledge State fingerprint;
- last runtime pass result;
- whether the most recent pass was deferred;
- whether the current state can be reloaded and its fingerprint validated.

The UI remains read-only. No diagnostic control may execute a candidate, select a policy, alter Safety Gate results, mutate ActionCatalog, request new permissions, or invoke privileged Android APIs.

## 3. Architecture

MainActivity reads a small diagnostic snapshot backed by the same AdaptiveLearningKnowledgeStore used by the background learner and the in-memory AdaptiveLearningRuntime.lastResult.

The diagnostic snapshot:
1. loads the persistent Knowledge State;
2. validates the persisted fingerprint through the existing store;
3. reads the runtime last result;
4. exposes only descriptive fields to the UI.

A missing Knowledge State is reported as NOT_INITIALIZED. A corrupt or unsupported state is reported as ERROR with the exception type/message; the diagnostic does not delete or repair the file.

## 4. UI

The existing Refresh adaptive learning summary remains the source-history view.

A new read-only section immediately below it is labeled Adaptive learning runtime diagnostic.

It shows:
- Knowledge state: READY | NOT_INITIALIZED | ERROR
- Processed evidence: count
- Abstentions: count
- Advisory candidates: count
- State fingerprint: SHA-256
- Last pass: processed=n, abstained=n, deferred=bool, candidates=n
- Persistence validation: OK | ERROR

The fingerprint is displayed for auditability but is not user-editable.

## 5. Runtime Semantics

The diagnostic does not trigger a learning pass. The background service remains the sole scheduler.

AdaptiveLearningRuntime.lastResult is descriptive only. If the process restarts, the diagnostic may show the runtime result as not observed in this process while still reporting the persisted Knowledge State accurately.

## 6. Testing

Add JVM tests for:
- missing state reporting;
- valid state reporting and fingerprint validation;
- corrupt state reporting as error without replacement;
- diagnostic rendering for an observed runtime result;
- diagnostic rendering when no runtime result has been observed.

Existing Adaptive Learning, Safety Gate, Action Engine, and measurement tests remain unchanged.

## 7. Safety Boundary

The diagnostic has no path to:
- Action Engine;
- privileged Android APIs;
- policy selection;
- Safety Gate bypass;
- permission escalation;
- ActionCatalog mutation;
- Cloud AI.

It is observation-only.