# Offline Evaluation Metadata

Future controlled offline evaluation needs explicit metadata rather than inferred context.

Each record can carry:
- stable observation ID and timestamp;
- condition IDs and proposed action IDs;
- charging state;
- battery temperature;
- thermal status;
- network transport and validation state;
- interactive/foreground state;
- a declared workload label;
- an optional controlled outcome label.

`OfflineEvaluationMetadataFactory` maps only fields already collected by `DeviceSnapshot`. Missing values remain missing. The factory does not infer a workload or outcome from unrelated telemetry.

A controlled outcome is intentionally separate from descriptive before/after deltas. The current application remains DRY_RUN and does not generate real action-effect labels.

This metadata layer does not train models, estimate causal effects, rank policies, authorize actions, or execute device mutations.