# Experiment Protocol

Exploratory experiments discover behavior and generate hypotheses. Confirmatory experiments test a pre-defined claim.

Before a confirmatory experiment, record experiment ID, hypothesis/claim, scope, primary metric, baseline/comparator, workload, device/OS/build, relevant configuration, collection method, stopping/exclusion rules when applicable, expected analysis, and success/interpretation criteria.

During execution preserve raw measurements, do not overwrite source data, record deviations, failures, and unexpected results.

After execution derive analysis from raw data, compare against predeclared criteria, record uncertainty/limitations, classify outcome as supported/not-supported/inconclusive/invalidated, and link it to task/finding/decision/release.

Changing the primary metric or acceptance rule after seeing results requires a new exploratory/post-hoc record. Governance imposes no fixed number of runs/devices; replication depth must match the research question and claim scope.
