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

Android documents Process.getElapsedCpuTime() as elapsed CPU time for the process. PSS is sampled process-memory telemetry and should not be interpreted as an exact peak-memory measurement.

## Experimental control

Default configuration:

- model: Qwen3 0.6B Q4_0;
- runtime: pinned llama.cpp b10982;
- context: 4096 tokens;
- maximum generation: 64 tokens;
- threads: 2 and 4;
- repetitions: 2 per configuration;
- delay between runs: 10 seconds;
- fixed benchmark prompt;
- model must pass the existing size and SHA-256 verification before execution.

The benchmark alternates the configured thread counts within each repetition so one configuration is not measured only at the beginning or end of the entire experiment.

The current Android runtime loads the GGUF model for every inference call. Therefore loadMs is deliberately reported separately from prompt processing and generation. The Stage 9 result is a cold-call measurement of the current implementation, not a claim about a future persistent-model service.

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

llama.cpp's performance guidance recommends measuring thread counts rather than assuming that more threads are faster. Its benchmark tooling also separates prompt processing from token generation, which is why Stage 9 keeps those phases separate.

No model-admission decision is encoded in this benchmark. Physical evidence must be reviewed before changing the local AI architecture.

## Output

Results are saved under the app's external-files benchmark directory as:

local-inference-<benchmarkId>-<timestamp>.json

The JSON is suitable for later aggregation into the project's benchmarks/results evaluation pipeline. The GGUF model itself is never copied into the repository.
