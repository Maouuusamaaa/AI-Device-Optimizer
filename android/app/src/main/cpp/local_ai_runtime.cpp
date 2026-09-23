#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <string>
#include <vector>
#include "llama.h"

namespace {
constexpr const char * TAG = "AILocalRuntime";
constexpr int MAX_CONTEXT = 8192;
constexpr int MAX_MAX_TOKENS = 256;
constexpr const char * NON_THINKING_TAG = "<think>";

using Clock = std::chrono::steady_clock;

long long elapsed_ms(const Clock::time_point start, const Clock::time_point end) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
}

void log_error(const std::string & message) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", message.c_str());
}

std::string jstring_to_string(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring make_result(JNIEnv * env, const std::string & value) {
    return env->NewStringUTF(value.c_str());
}

std::string json_escape(const std::string & input) {
    std::string output;
    output.reserve(input.size());
    for (unsigned char ch : input) {
        switch (ch) {
            case '\\': output += "\\\\"; break;
            case '"': output.push_back('\\'); output.push_back('"'); break;
            case '\r': output += "\\r"; break;
            case '\t': output += "\\t"; break;
            default: output += ch < 0x20 ? ' ' : static_cast<char>(ch);
        }
    }
    return output;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_maouuusama_ai_device_optimizer_localai_LocalLlamaRuntime_nativeVersion(
        JNIEnv * env, jobject) {
    llama_backend_init();
    const char * version = llama_version();
    return make_result(env, version == nullptr ? "unknown" : version);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_maouuusama_ai_device_optimizer_localai_LocalLlamaRuntime_nativeIsAvailable(
        JNIEnv *, jobject) {
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_maouuusama_ai_device_optimizer_localai_LocalLlamaRuntime_nativeResetRuntime(
        JNIEnv *, jobject) {
    // Generation already releases model/context/backend state after each call.
    // This explicit boundary is a diagnostic hook so the benchmark can verify
    // that an additional runtime cleanup does not leave a persistent native handle.
    llama_backend_free();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_maouuusama_ai_device_optimizer_localai_LocalLlamaRuntime_nativeGenerate(
        JNIEnv * env, jobject,
        jstring model_path_value, jstring prompt_value,
        jint context_tokens_value, jint max_tokens_value, jint threads_value,
        jboolean keep_backend_alive) {

    const auto total_start = Clock::now();
    const std::string model_path = jstring_to_string(env, model_path_value);
    const std::string prompt = jstring_to_string(env, prompt_value);
    const int context_tokens = std::clamp(static_cast<int>(context_tokens_value), 256, MAX_CONTEXT);
    const int max_tokens = std::clamp(static_cast<int>(max_tokens_value), 1, MAX_MAX_TOKENS);
    const int threads = std::clamp(static_cast<int>(threads_value), 1, 8);

    if (model_path.empty() || prompt.empty()) {
        return make_result(env, R"({"ok":false,"error":"model_path_and_prompt_required"})");
    }

    llama_backend_init();
    // Normal generation releases backend-global state. Lifecycle diagnostics keep it
    // alive so model/context cleanup and explicit reset can be observed separately.
    const auto cleanup_backend = [&]() {
        if (keep_backend_alive == JNI_FALSE) {
            llama_backend_free();
        }
    };

    const auto load_start = Clock::now();
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    llama_model * model = llama_model_load_from_file(model_path.c_str(), model_params);
    const auto load_end = Clock::now();

    if (model == nullptr) {
        log_error("Failed to load GGUF model");
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"model_load_failed"})");
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    if (vocab == nullptr) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"vocab_unavailable"})");
    }

    // Qwen3's documented hard switch is represented by an empty <think> block
    // in the formatted generation prompt. The Android runtime uses raw llama
    // completion rather than llama.cpp's chat-template/Jinja path, so add a
    // deterministic token-level guard as a second enforcement layer. This
    // prevents the model from starting a new <think> block even if the raw
    // prompt path does not interpret the chat-template switch.
    const auto think_tokenization = -llama_tokenize(
        vocab, NON_THINKING_TAG, sizeof("<think>") - 1, nullptr, 0, true, true);
    if (think_tokenization != 1) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"non_thinking_guard_unavailable"})");
    }
    llama_token think_token = LLAMA_TOKEN_NULL;
    if (llama_tokenize(
            vocab, NON_THINKING_TAG, sizeof("<think>") - 1,
            &think_token, 1, true, true) != 1) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"non_thinking_guard_tokenize_failed"})");
    }

    const auto tokenization_start = Clock::now();
    const int n_prompt = -llama_tokenize(
        vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0 || n_prompt >= context_tokens) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"prompt_exceeds_context"})");
    }

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(n_prompt));
    if (llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                       prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"tokenization_failed"})");
    }
    const auto tokenization_end = Clock::now();

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = static_cast<uint32_t>(context_tokens);
    context_params.n_batch = static_cast<uint32_t>(std::min(context_tokens, std::max(n_prompt, 1)));
    context_params.n_threads = threads;
    context_params.n_threads_batch = threads;

    const auto context_start = Clock::now();
    llama_context * context = llama_init_from_model(model, context_params);
    const auto context_end = Clock::now();

    if (context == nullptr) {
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"context_init_failed"})");
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    if (sampler == nullptr) {
        llama_free(context);
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"sampler_init_failed"})");
    }

    const llama_logit_bias non_thinking_bias{
        think_token,
        -INFINITY
    };
    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_logit_bias(
            llama_vocab_n_tokens(vocab),
            1,
            &non_thinking_bias));
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    const auto prompt_decode_start = Clock::now();
    llama_batch batch = llama_batch_get_one(
        prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));
    if (llama_decode(context, batch) != 0) {
        llama_sampler_free(sampler);
        llama_free(context);
        llama_model_free(model);
        cleanup_backend();
        return make_result(env, R"({"ok":false,"error":"prompt_decode_failed"})");
    }
    const auto prompt_decode_end = Clock::now();

    const auto generation_start = Clock::now();
    std::string output;
    output.reserve(static_cast<size_t>(max_tokens) * 4);
    int generated_tokens = 0;

    for (int i = 0; i < max_tokens; ++i) {
        llama_token token = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        char buffer[512];
        const int piece_size = llama_token_to_piece(
            vocab, token, buffer, sizeof(buffer), 0, true);
        if (piece_size < 0) break;
        output.append(buffer, static_cast<size_t>(piece_size));
        ++generated_tokens;

        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(context, batch) != 0) break;
    }
    const auto generation_end = Clock::now();

    const long long load_ms = elapsed_ms(load_start, load_end);
    const long long tokenization_ms = elapsed_ms(tokenization_start, tokenization_end);
    const long long context_init_ms = elapsed_ms(context_start, context_end);
    const long long prompt_decode_ms = elapsed_ms(prompt_decode_start, prompt_decode_end);
    const long long generation_ms = elapsed_ms(generation_start, generation_end);
    const long long total_native_ms = elapsed_ms(total_start, Clock::now());
    const double generation_tokens_per_second =
        generation_ms > 0 ? (generated_tokens * 1000.0) / static_cast<double>(generation_ms) : 0.0;

    const std::string result =
        std::string(R"({"ok":true,"output":")") + json_escape(output) +
        R"(","model":"qwen3-0.6b-q4_0","promptTokens":)" +
        std::to_string(n_prompt) +
        R"(,"generatedTokens":)" + std::to_string(generated_tokens) +
        R"(,"threads":)" + std::to_string(threads) +
        R"(,"loadMs":)" + std::to_string(load_ms) +
        R"(,"tokenizationMs":)" + std::to_string(tokenization_ms) +
        R"(,"contextInitMs":)" + std::to_string(context_init_ms) +
        R"(,"promptDecodeMs":)" + std::to_string(prompt_decode_ms) +
        R"(,"generationMs":)" + std::to_string(generation_ms) +
        R"(,"generationTokensPerSecond":)" + std::to_string(generation_tokens_per_second) +
        R"(,"totalNativeMs":)" + std::to_string(total_native_ms) +
        R"(,"modelOutputContainsThink":)" +
        (output.find("<think>") != std::string::npos ? "true" : "false") +
        R"(,"nonThinkingGuard":true,"advisoryOnly":true,"executionRequested":false,"deviceMutationAllowed":false})";

    llama_sampler_free(sampler);
    llama_free(context);
    llama_model_free(model);
    cleanup_backend();
    return make_result(env, result);
}
