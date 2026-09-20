#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <string>
#include <vector>
#include "llama.h"

namespace {
constexpr const char * TAG = "AILocalRuntime";
constexpr int MAX_CONTEXT = 8192;
constexpr int MAX_MAX_TOKENS = 256;

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
            case '"': output += "\\""; break;
            case '\n': output += "\\n"; break;
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_maouuusama_ai_device_optimizer_localai_LocalLlamaRuntime_nativeGenerate(
        JNIEnv * env, jobject,
        jstring model_path_value, jstring prompt_value,
        jint context_tokens_value, jint max_tokens_value) {

    const std::string model_path = jstring_to_string(env, model_path_value);
    const std::string prompt = jstring_to_string(env, prompt_value);
    const int context_tokens = std::clamp(static_cast<int>(context_tokens_value), 256, MAX_CONTEXT);
    const int max_tokens = std::clamp(static_cast<int>(max_tokens_value), 1, MAX_MAX_TOKENS);

    if (model_path.empty() || prompt.empty()) {
        return make_result(env, R"({"ok":false,"error":"model_path_and_prompt_required"})");
    }

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    llama_model * model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (model == nullptr) {
        log_error("Failed to load GGUF model");
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"model_load_failed"})");
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    if (vocab == nullptr) {
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"vocab_unavailable"})");
    }

    const int n_prompt = -llama_tokenize(
        vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0 || n_prompt >= context_tokens) {
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"prompt_exceeds_context"})");
    }

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(n_prompt));
    if (llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                       prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"tokenization_failed"})");
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = static_cast<uint32_t>(context_tokens);
    context_params.n_batch = static_cast<uint32_t>(std::min(context_tokens, std::max(n_prompt, 1)));
    context_params.n_threads = 4;
    context_params.n_threads_batch = 4;

    llama_context * context = llama_init_from_model(model, context_params);
    if (context == nullptr) {
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"context_init_failed"})");
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    if (sampler == nullptr) {
        llama_free(context);
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"sampler_init_failed"})");
    }
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    llama_batch batch = llama_batch_get_one(
        prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));
    if (llama_decode(context, batch) != 0) {
        llama_sampler_free(sampler);
        llama_free(context);
        llama_model_free(model);
        llama_backend_free();
        return make_result(env, R"({"ok":false,"error":"prompt_decode_failed"})");
    }

    std::string output;
    output.reserve(static_cast<size_t>(max_tokens) * 4);
    for (int i = 0; i < max_tokens; ++i) {
        const llama_token token = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        char buffer[512];
        const int piece_size = llama_token_to_piece(
            vocab, token, buffer, sizeof(buffer), 0, true);
        if (piece_size < 0) break;
        output.append(buffer, static_cast<size_t>(piece_size));

        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(context, batch) != 0) break;
    }

    const std::string result =
        std::string(R"({"ok":true,"output":")") + json_escape(output) +
        R"(","model":"qwen3-0.6b-q4_0","advisoryOnly":true,"executionRequested":false,"deviceMutationAllowed":false})";

    llama_sampler_free(sampler);
    llama_free(context);
    llama_model_free(model);
    llama_backend_free();
    return make_result(env, result);
}
