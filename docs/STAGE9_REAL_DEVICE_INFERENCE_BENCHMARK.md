# Stage 9 — Real-Device Qwen3 Inference Benchmark

## Scope

Stage 9 turns the existing physical-device Qwen3 0.6B inference path into a controlled measurement experiment.

The benchmark compares two CPU thread configurations, 2 and 4 threads, with two repetitions per configuration by default. The configurations are measured rather than treated as an optimization decision. The benchmark uses the same fixed prompt and the same model/runtime for every run.

The benchmark is read-only. It does not change Android settings, permissions, thermal policy, process state, or optimizer actions.

## Measurements

Each inference sample records:

- unique observation ID;
- exact start/end timestamps;
- thread count and repetition;
- wall-clock inference duration;
- process CPU-time delta from android.os.Process.getElapsedCpuTime();
- process PSS before/after and delta from android.os.Debug.getPss();
- available RAM before/after;
- battery percentage before/after;
- battery temperature before/after;
- thermal status before/after;
- prompt token count;
- generated token count;
- model-load time;
- tokenization time;
- context initialization time;
- prompt-decode time;
- generation time;
- generation tokens/second;
- total native runtime;
- whether the model output contained <think>;
- advisory-only safety flags.

## Experimental control

Default configuration:

- model: Qwen3 0.6B Q4_0;
- runtime: pinned llama.cpp b10982;
- context: 4096 tokens;
- maximum generation: 64 tokens;
- threads: 2 and 4;
- repetitions: 2 per configuration;
- delay between runs: 15 seconds;
- controlled start guard: not charging, thermal status NONE, battery temperature < 39 °C;
- maximum cooldown wait: 90 seconds;
- fixed benchmark prompt;
- model must pass the existing size and SHA-256 verification before execution.

The hardened benchmark uses a balanced schedule of 2-thread, 4-thread, 4-thread, 2-thread for the default two repetitions. Before each measured sample, the benchmark requires charging=false, thermal status NONE when available, and battery temperature below 39 °C; it waits up to 90 seconds for those conditions rather than mutating device state.

The benchmark runs from a profileable, non-debuggable release build for performance measurements. The current Android runtime loads the GGUF model for every inference call. Therefore loadMs is deliberately reported separately from prompt processing and generation.

## Safety

Every native result is rejected by the benchmark unless:

- ok=true;
- advisoryOnly=true;
- executionRequested=false;
- deviceMutationAllowed=false.

The benchmark does not pass model output to the Action Engine.

## Interpretation

The benchmark must report measurements before any optimization change is made. In particular:

- compare generation tokens/second and generation time across thread configurations;
- inspect model-load time separately;
- inspect process CPU-time and PSS deltas;
- record battery and thermal conditions;
- check repeated-run variability;
- preserve failed/aborted runs rather than replacing them with invented values.

No model-admission decision is encoded in this benchmark. Physical evidence must be reviewed before changing the local AI architecture.

## Output

Results are first saved privately under the app's external-files benchmark directory as:

local-inference-<benchmarkId>-<timestamp>.json

After each completed Stage 9 run, the app also exports the same JSON into shared Downloads through MediaStore:

/storage/emulated/0/Download/AI-Device-Optimizer/benchmarks/

This shared export is intended for Termux and other user tools that need to inspect benchmark evidence. It does not grant Termux access to the app's private Android/data directory, and it does not require broad storage access for the optimizer app on Android 10/API 29 or newer.

The JSON is suitable for later aggregation into the project's benchmarks/results evaluation pipeline. The GGUF model itself is never copied into the repository.


## Qwen3 non-thinking prompt hardening

Stage 9 uses Qwen3's documented hard non-thinking generation-prefix pattern rather than relying only on the soft `/no_think` instruction. The prompt ends the assistant prefix with an empty `<think>...</think>` block before generation. This keeps the benchmark's reasoning-mode contract explicit and makes `modelOutputContainsThink` a direct validation signal for the generated continuation.

The benchmark remains advisory-only:
- `advisoryOnly=true`
- `executionRequested=false`
- `deviceMutationAllowed=false`

A benchmark run is not considered non-thinking compliant if the generated continuation itself contains a `<think>` block.
