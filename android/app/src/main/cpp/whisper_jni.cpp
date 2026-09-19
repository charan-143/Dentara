// JNI bridge between WhisperNative.kt and whisper.cpp.
//
// Deliberately thin: it owns a whisper_context handle and runs one decode. Every policy
// decision that affects what ends up in a patient chart (threading, retries, how a failure
// is surfaced) lives in Kotlin where it can be read and tested.
//
// A failed decode returns null. It never returns a partial or placeholder transcript.

#include <jni.h>
#include <android/log.h>

#include <string>
#include <vector>

#include "whisper.h"

#define TAG "DentaraWhisper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

void forward_whisper_log(enum ggml_log_level level, const char *text, void * /*user_data*/) {
    if (text == nullptr) {
        return;
    }
    if (level == GGML_LOG_LEVEL_ERROR) {
        LOGE("%s", text);
    } else {
        LOGI("%s", text);
    }
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_thornburydental_speech_WhisperNative_nativeInit(
        JNIEnv *env, jobject /*thiz*/, jstring model_path) {

    whisper_log_set(forward_whisper_log, nullptr);

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) {
        return 0;
    }

    whisper_context_params cparams = whisper_context_default_params();
    // CPU only. GPU backends vary too much across the handsets a clinic actually owns,
    // and a decode that silently degrades is worse than one that is a little slower.
    cparams.use_gpu = false;

    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params returned null");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_example_thornburydental_speech_WhisperNative_nativeRelease(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong ctx_ptr) {
    if (ctx_ptr == 0) {
        return;
    }
    whisper_free(reinterpret_cast<whisper_context *>(ctx_ptr));
}

/**
 * Decodes 16 kHz mono PCM16 samples.
 *
 * @return the transcript, or null if the decode failed. Null means "no transcript",
 *         never "empty transcript" - the caller must tell those apart.
 */
JNIEXPORT jstring JNICALL
Java_com_example_thornburydental_speech_WhisperNative_nativeTranscribe(
        JNIEnv *env, jobject /*thiz*/,
        jlong ctx_ptr, jshortArray pcm, jint n_threads, jstring language, jstring prompt) {

    auto *ctx = reinterpret_cast<whisper_context *>(ctx_ptr);
    if (ctx == nullptr || pcm == nullptr) {
        return nullptr;
    }

    const jsize sample_count = env->GetArrayLength(pcm);
    if (sample_count <= 0) {
        return nullptr;
    }

    std::vector<float> samples(static_cast<size_t>(sample_count));
    {
        jshort *raw = env->GetShortArrayElements(pcm, nullptr);
        if (raw == nullptr) {
            return nullptr;
        }
        for (jsize i = 0; i < sample_count; ++i) {
            samples[static_cast<size_t>(i)] = static_cast<float>(raw[i]) / 32768.0f;
        }
        env->ReleaseShortArrayElements(pcm, raw, JNI_ABORT);
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads        = n_threads > 0 ? n_threads : 4;
    params.translate        = false;
    params.no_context       = true;   // each dictation is independent; do not condition on the last one
    params.no_timestamps    = true;
    params.single_segment   = true;
    params.print_special    = false;
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = false;
    params.suppress_blank   = true;
    params.temperature      = 0.0f;
    params.temperature_inc  = 0.0f;  // disable fallback re-decoding iterations
    params.audio_ctx        = 768;   // reduced context for fast utterance recognition (2-3x speedup)

    // params.language and params.initial_prompt hold borrowed pointers, so both UTF strings
    // must outlive the whisper_full call below.
    const char *lang = (language == nullptr) ? nullptr : env->GetStringUTFChars(language, nullptr);
    if (lang != nullptr) {
        params.language = lang;
    }

    const char *initial_prompt = (prompt == nullptr) ? nullptr : env->GetStringUTFChars(prompt, nullptr);
    if (initial_prompt != nullptr) {
        params.initial_prompt = initial_prompt;
    }

    const int rc = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));

    if (lang != nullptr) {
        env->ReleaseStringUTFChars(language, lang);
    }
    if (initial_prompt != nullptr) {
        env->ReleaseStringUTFChars(prompt, initial_prompt);
    }

    if (rc != 0) {
        LOGE("whisper_full failed with code %d", rc);
        return nullptr;
    }

    std::string transcript;
    const int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char *segment = whisper_full_get_segment_text(ctx, i);
        if (segment != nullptr) {
            transcript += segment;
        }
    }

    return env->NewStringUTF(transcript.c_str());
}

} // extern "C"
