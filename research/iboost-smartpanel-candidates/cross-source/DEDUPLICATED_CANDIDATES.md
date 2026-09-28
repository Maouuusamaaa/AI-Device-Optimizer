# Cross-Source Deduplicated Candidate Queue

The same release found on multiple public sources is one logical candidate.

| Logical candidate | VersionCode | Provenance | Queue status |
|---|---:|---|---|
| 15.2.1.052 | 150201052 | APKMirror, APKPure, APKCombo | pending binary verification |
| 15.2.0.117 | 150200117 | APKMirror, APKCombo | pending binary verification |
| 3.8.2.077 | 30802077 | APKMirror, APKCombo | pending binary verification |

## Selection rule

A newer candidate replaces an older candidate only after the newer candidate itself passes the required verification gates. A failed newer candidate does not invalidate an older candidate that has already passed.

No installation approval is implied by this queue.
