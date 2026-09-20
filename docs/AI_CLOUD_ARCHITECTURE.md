# Stage 3 — AI Cloud Foundation

## Purpose

Stage 3 establishes the cloud-AI boundary before model training, hosted inference, or device mutation is enabled.

## Architecture

Android Monitor → Local Diagnosis → Cloud Observation → AI Advisor → Cloud Recommendation → Local Policy Simulation → Safety Gate → Action Engine → Measurement

The cloud is advisory. The local device remains authoritative for permissions, action allowlisting, safety policy, execution, and outcome measurement.

## Contracts

The observation contract contains a stable observation ID, capture timestamp, Android API level, CPU architecture, and normalized telemetry. Raw logs, credentials, tokens, private files, and unrestricted Android shell capabilities are outside the contract.

The recommendation contract contains diagnosis, confidence from 0 to 1, evidence references, and a candidate action ID. It explicitly requires mode=ADVISORY_ONLY, execution.requested=false, and execution.deviceMutationAllowed=false.

## Local authority boundary

A cloud recommendation is not an authorization. Unknown action IDs, invalid evidence, incompatible device state, missing permissions, insufficient confidence, and safety-policy violations must be rejected locally.

## Dataset and training

Training data must be reproducible from versioned artifacts, retain provenance references, and use deterministic evaluation splits. Private device data must not be committed to the public repository.

The first model milestone should be model-agnostic: a compact supervised/ranking model or deterministic baseline can establish the inference and evaluation contracts before larger models are considered.

## Offline fallback

Cloud unavailability must leave local diagnosis and safe policies functional. Cloud availability is never a prerequisite for device safety.

## Stage 3 completion criteria

- versioned observation/recommendation contract
- dependency-free validation
- tests rejecting cloud execution requests
- documented local authority boundary
- CI validation
- no device mutation enabled
