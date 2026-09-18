# Local Policy Engine

The Local Policy Engine converts read-only device state into explicit, testable policy decisions. It does not execute actions.

## Safety boundary

The engine may propose an action identifier, but it cannot call privileged APIs or mutate device state. Every action identifier must eventually pass an explicit Action Engine allowlist.

## Initial policies

- memory.low: available RAM is at or below 15 percent of total RAM.
- battery.low: battery is at or below 20 percent while not charging.
- workload.gaming: gaming is active, so background-pressure actions are not proposed from gaming alone.
- device.normal: no configured local threshold requires intervention.

These thresholds are provisional configuration, not universal optimization truths. They must be validated against real-device benchmark data.

Android provides current available and total memory through ActivityManager.MemoryInfo, including a lowMemory indicator. Battery state can be read from the protected ACTION_BATTERY_CHANGED sticky broadcast through registerReceiver. See the Android documentation for these platform APIs.

No AI-generated policy may bypass these local safety boundaries.
