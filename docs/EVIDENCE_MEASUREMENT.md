# Evidence Measurement Contract

## Purpose

The evidence measurement layer turns raw benchmark observations into auditable
diagnostic evidence without authorizing device mutation.

## Pipeline

`Raw Evidence → Contract Validation → Comparable Pair → Descriptive Comparison → Regression Classification → Provenance/History → Cloud Advisor Context → Local Policy Simulation → Safety Gate Input`

## Supported evidence

The canonical benchmark schema accepts versions 1 and 2. Version 2 is the format
currently emitted by the Android benchmark writer. Historical version 1 evidence
remains readable.

A comparable pair requires:

- valid evidence contracts;
- matching manufacturer/model;
- matching Android API level;
- matching workload;
- compatible schema version;
- required benchmark identity fields.

A missing or incompatible comparison does not imply health. It produces
`INSUFFICIENT_EVIDENCE`.

## Classifications

### NO_REGRESSION

No configured regression threshold was crossed.

### REGRESSION

At least one configured negative measurement signal crossed its threshold and
there was no contradictory improvement signal.

### MIXED

Valid evidence contains materially conflicting positive and negative measurement
signals. This classification is not authorization for optimization.

### INSUFFICIENT_EVIDENCE

There is no valid comparable pair or required comparison information is missing.

### INVALID_EVIDENCE

The source evidence does not satisfy the supported contract.

## Rules

Rules are versioned in `benchmarks/evidence_rules.json`.

Current rules use:

- available RAM decrease of 5% or more as a regression signal;
- PSS increase of 20% or more as a regression signal;
- corresponding strong improvements can create a MIXED result when another
  metric simultaneously signals regression.

These are descriptive classification thresholds, not optimization policies.

## Provenance

Every analysis record contains:

- analyzer version;
- source evidence SHA-256 values;
- benchmark schema version;
- rules version;
- deterministic analysis ID;
- classification and reasons;
- descriptive comparison data.

History is append-only. Duplicate analysis IDs are rejected without replacing
the original record.

## Cloud Advisor boundary

The 0.1.15 Cloud Advisor layer uses validated evidence context for advisory
reasoning only. Cloud output is untrusted candidate data and must pass local
contract validation and Policy Simulation before the Safety Gate can evaluate it.

The linkage is:

`evidenceId → advisorVersion → policyId → simulation result → local safety decision`

Cloud output cannot:

- authorize a candidate action;
- alter the local ActionCatalog;
- change local safety constraints;
- enable device mutation;
- replace local measurement evidence.

If the cloud is unavailable, the local measurement and dry-run policy path remains
usable.

## Safety boundary

The evidence layer has no privileged Android operation.

It cannot:

- invoke Action Engine;
- authorize a candidate action;
- bypass Safety Gate;
- infer a memory leak solely from PSS/RSS;
- replace raw evidence with an interpreted result.

## Real-device validation

The automated contract suite validates the existing P661N/API33 benchmark evidence
stored under `benchmarks/results/`. This verifies that real-device observations
can enter the hardened contract without introducing mutation.

The validation does not claim a new optimization benefit and does not reinterpret
the closed 0.1.13 lifecycle investigation.
## SmartPanel Game Mode evidence

On supported Transsion/itel builds, the Android monitor may read the SmartPanel
Game Mode read-only AppListProvider using the normal
`com.transsion.gamemode.permission.READ_APP_LIST` permission.

The snapshot records four fields:

- `gameModeProviderAvailable`: whether the provider returned a readable cursor;
- `gameModePackages`: package names returned by the provider;
- `gameModeCheckedPackages`: package names whose provider `ischeck` value parses
  conservatively as checked (`1` or case-insensitive `true`);
- `gameModeProviderError`: a diagnostic error string when the provider cannot
  be read.

These fields are observational evidence only. They do not authorize Game Mode
changes, Action Engine operations, privileged permissions, or system APK
modification. An empty checked set is not evidence that no games are configured
unless `gameModeProviderAvailable` is also true.

Runtime validation on the itel P661N/API 33 confirmed provider availability,
a non-null package set, matching checked-package serialization, and a null
provider error in schema version 4.

A controlled UI toggle validation was then performed for Minecraft
(package `com.mojang.minecraftpe`) using the SmartPanel Game Management UI:

1. With the Minecraft toggle ON, the application snapshot contained
   `com.mojang.minecraftpe` in `gameModeCheckedPackages`.
2. After switching the toggle OFF and waiting for a new monitor snapshot,
   `com.mojang.minecraftpe` remained in the provider's game list but was
   absent from `gameModeCheckedPackages`.
3. After switching the toggle ON again and waiting for another monitor
   snapshot, `com.mojang.minecraftpe` was again present in
   `gameModeCheckedPackages`.

The observed ON → OFF → ON sequence provides device-level evidence that the
provider's checked-state field, as conservatively parsed by the reader,
tracks the SmartPanel Game Management toggle for Minecraft on this
itel P661N/API 33 build. This is an observational correlation on one tested
device/build; it does not establish semantics for every Transsion/itel build
and does not authorize mutation. The reader remains read-only and the local
Safety Gate remains authoritative.
