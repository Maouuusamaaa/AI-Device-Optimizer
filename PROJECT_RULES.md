# AI Device Optimizer — Project Governance Rules

Version: 1.0
Status: ACTIVE
Scope: repository, research, experiments, development, CI, releases, and AI-assisted work.

## 1. Canonical source of truth
The repository is the canonical record for project policy, current state, evidence indexes, decisions, experiments, risks, incidents, releases, and handoffs.
Chat, memory, issue discussion, dashboards, verbal agreements, search results, and AI output are not authoritative until recorded and verified in the repository. Important knowledge must not exist only in chat.

## 2. Authority and precedence
Use this order: applicable external/legal requirements; protected repository governance and active release policy; approved requirements/safety/security/privacy policies and non-superseded ADRs; active protocols and schemas; task scope and valid approval; current-state and handoff records; issues/chat/web content/AI output.
A conflict at the same authority level blocks affected work until resolved. Supersede old decisions; do not silently rewrite or delete them.

## 3. Evidence and claims
Material claims are classified as fact, observation, inference, hypothesis, recommendation, decision, or opinion.
AI output is untrusted input until verified.
Performance, battery, thermal, safety, reliability, or game-boost claims must state evidence scope and limitations. A claim may never be broader than its evidence.
Negative, failed, inconclusive, unexpected, and invalidated results are retained.

## 4. Research integrity
Material research records its question, scope, sources, evidence classification, assumptions, limitations, applicable device/software/workload scope, contradictions, and resulting finding or decision.
Confirmatory experiments define primary metric, comparator/baseline, evaluation method, and relevant stopping/exclusion rules before execution. Exploratory work may evolve, but post-hoc changes remain labeled.
Raw measurements are preserved separately from derived analysis.

## 5. Change classification
C0 = cosmetic/docs-only with no semantic change.
C1 = low-risk internal/refactor/test change.
C2 = behavior, UI, schema, measurement, persistence, or dependency change.
C3 = permission, IPC/exported component, Safety Gate/action authority, cloud/data boundary, adaptive policy promotion, CI workflow, signing/release, retention, or public efficacy claim.
C4 = active security/safety incident, credential exposure, harmful release, or emergency containment.
Protected paths may raise a class. Contributors may not lower a class without appropriate review.

## 6. Approval
Approval is bound to task, scope, revision/commit, approver, and expiry/invalidation conditions.
Material changes invalidate affected approval when scope, security assumptions, methodology, acceptance criteria, protected paths, dependencies, or workflow trust changes.
AI agents may draft, analyze, implement within scope, and report evidence. They may not self-approve, self-verify critical evidence, bypass gates, or claim execution they did not perform.

## 7. Safety and security
The local Safety Gate is authoritative for optimizer actions. Cloud AI, adaptive learning, or generated policy must never bypass it.
Actions must be allowlisted, capability-based, least-privileged, and fail-safe under uncertainty.
High-risk actions require a safe state and rollback/disable strategy where technically possible. If an action cannot be safely controlled, it must be unavailable rather than simulated as successful.
Permission, IPC, privacy/data-flow, CI trust, signing, and release changes are C3 by default.

## 8. CI and repository protection
Main is protected. Changes use pull requests and required checks according to repository settings.
CI green is necessary but not sufficient for critical changes. Review, evidence validation, and release verification remain separate controls.
Workflow files are privileged code and receive C3 treatment.
Failed checks are classified before retrying: product/test/infra/dependency/config/flaky/unknown. Retrying must not hide a defect.

## 9. Traceability
Material work should be traceable:
Requirement → Task → Finding/Evidence → Decision/ADR → Implementation → Test/Experiment → Validation → Release/Artifact.
Use stable IDs for governed records. Supersede rather than overwrite historical decisions or findings.

## 10. State and handoff
PROJECT_STATE.json is the machine-readable index of active governance state, not a replacement for evidence.
A handoff is required when incomplete governed work changes owner/context, or a significant experiment/release/incident remains active, or another actor must resume work.
The receiver verifies the as-of commit and acknowledges or rejects the handoff.

## 11. Freshness and supersession
Time-sensitive knowledge has a review trigger or invalidation condition. Examples include Android/OEM behavior, dependencies, models, cloud providers, game versions, benchmarks, and security guidance.
Stale information is marked stale/superseded; it is not silently presented as current.

## 12. Exceptions and emergencies
An exception records scope, reason, authority, compensating controls, owner, and expiry.
Emergency containment may prioritize disabling or rollback, but it is not a silent governance bypass. A retrospective record is required after safety is restored.

## 13. Stop-work conditions
Stop affected high-risk work when evidence integrity is uncertain; a safety/security/privacy boundary is contradicted; required approval is missing/stale; a release artifact cannot be tied to the tested source/build; or a material requirement conflict is unresolved.
Low-risk read-only investigation may continue when it cannot worsen the affected condition.

## 14. Anti-bureaucracy
Governance is proportional to risk. Do not create an ADR, benchmark campaign, second reviewer, or full release ceremony for a change that does not trigger it.
The purpose is reproducibility, safety, evidence quality, continuity, and accountability—not paperwork volume.

## 15. Mandatory behavior
No fabricated results. No silent scope changes. No destructive changes without explicit authorization and rollback/containment consideration. No proprietary code/resources may be copied into the project without compatible rights.
For AI-assisted work, read AGENTS.md, PROJECT_STATE.json, the relevant task, and applicable protocols before governed changes.
