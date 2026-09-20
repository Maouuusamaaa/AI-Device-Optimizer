# Descriptive Evaluation Report

This report converts an already validated offline dataset into deterministic descriptive statistics.

It reports:
- observation count;
- condition, action, outcome, charging, and workload coverage;
- battery-temperature count/average/minimum/maximum;
- SHA-256 dataset fingerprint.

The script rejects duplicate observation IDs, non-chronological records, malformed metadata, and non-finite numeric values.

The report is measurement-only. It does not estimate action effectiveness, infer causality, rank actions, select policies, authorize execution, or mutate a device.

The physical-device DRY_RUN history must not be presented as evidence that an optimization caused a performance change.
