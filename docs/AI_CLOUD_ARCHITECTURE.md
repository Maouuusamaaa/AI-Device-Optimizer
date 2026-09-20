# Stage 3 — AI Cloud Foundation

## Purpose

Stage 3 establishes the cloud-AI boundary before model training, hosted inference, or device mutation is enabled.

## Architecture

Android Monitor → Local Diagnosis → Cloud Observation → AI Advisor → Cloud Recommendation → Local Policy Simulation → Safety Gate → Action Engine → Measurement

The cloud is advisory. The local device remains authoritative for permissions, action allowlisting, safety policy, execution, and outcome measurement.

## Contracts

The observation contract contains a stable observation ID, capture timestamp, Android API level, CPU architecture, and normalized telemetry. Raw logs, credentials, tokens, private files, and unrestricted Android shell capabilities are outside the contract.

Training rows preserve an observation reference and explicit provenance. The inference contract identifies the model and observation while keeping output advisory-only. The evaluation protocol requires deterministic held-out splits and reports coverage, abstention, recommendation validity, and action-ID validity.

## Model strategy

The first model implementation is intentionally not fixed to a large language model. A deterministic baseline, compact classifier, ranking model, or later hosted model can implement the same adapter interface. Model selection must be driven by reproducible evaluation and resource constraints rather than model size alone.

## Local authority boundary

A cloud recommendation is not an authorization. Unknown action IDs, invalid evidence, incompatible device state, missing permissions, insufficient confidence, and safety-policy violations must be rejected locally.

## Dataset and training

Training data must be reproducible from versioned artifacts, retain provenance references, and use deterministic evaluation splits. Private device data must not be committed to the public repository.

## Offline fallback

Cloud unavailability must leave local diagnosis and safe policies functional. Cloud availability is never a prerequisite for device safety.

## Stage 3 completion criteria

- versioned observation/recommendation contract
- reproducible dataset row contract
- model adapter and inference contracts
- evaluation protocol
- dependency-free validation and safety tests
- documented local authority boundary
- CI validation
- no device mutation enabled
