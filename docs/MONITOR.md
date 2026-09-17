# Monitor Layer

The first implementation milestone is read-only device telemetry.

The Monitor layer collects a small, controlled snapshot:

- Android API level
- manufacturer and model
- total RAM
- available RAM
- battery percentage
- charging state
- collection timestamp

No optimization or mutation is performed.

## Safety boundary

Monitor code must remain independent from the Action Engine. A telemetry failure must not trigger a device mutation.

Future telemetry can be added only after its Android API availability, permission requirements, measurement quality, and runtime overhead are documented.

## Next benchmark

Before enabling automatic actions, collect repeated snapshots on a real device and compare:

1. idle device
2. normal application use
3. sustained workload
4. gaming workload

The benchmark should measure the optimizer's own RAM and CPU overhead as well as device-level effects.
