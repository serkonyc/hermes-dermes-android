// JNI bridge to llama.cpp for Dermes Pocket Brain chat (pure C).
// ChatML prompt is built in Java; here: tokenize -> prefill -> stream decode.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdatomic.h>
#include <android/log.h>
#include <llama.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "DermesLLM", __VA_ARGS__)

typedef struct {
    struct llama_model   *model;
    struct llama_context *ctx;
    const struct llama_vocab *vocab;
    atomic_int abort_flag;
} Handle;

static void throw_exc(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (cls) (*env)->ThrowNew(env, cls, msg);
}

JNIEXPORT jlong JNICALL
Java_ai_serge_dermes_ChatActivity_nativeLoad(JNIEnv *env, jclass cls,
        jstring jpath, jint nCtx, jint nThreads) {
    (void)cls;
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    llama_backend_init();
    struct llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;                       // CPU only
    mp.load_mode = LLAMA_LOAD_MODE_MMAP;       // OS evicts pages under RAM pressure
    struct llama_model *model = llama_model_load_from_file(path, mp);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    if (!model) { throw_exc(env, "model load failed (OOM or corrupt GGUF)"); return 0; }

    struct llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t)nCtx;
    cparams.n_batch = 512;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;
    struct llama_context *ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        llama_model_free(model); llama_backend_free();
        throw_exc(env, "context alloc failed (OOM)"); return 0;
    }
    Handle *h = calloc(1, sizeof(Handle));
    h->model = model; h->ctx = ctx; h->vocab = llama_model_get_vocab(model);
    atomic_init(&h->abort_flag, 0);
    LOGI("loaded ctx=%d threads=%d", nCtx, nThreads);
    return (jlong)(intptr_t)h;
}

JNIEXPORT void JNICALL
Java_ai_serge_dermes_ChatActivity_nativeAbort(JNIEnv *env, jclass cls, jlong pH) {
    (void)env; (void)cls;
    atomic_store(&((Handle *)(intptr_t)pH)->abort_flag, 1);
}

JNIEXPORT void JNICALL
Java_ai_serge_dermes_ChatActivity_nativeFree(JNIEnv *env, jclass cls, jlong pH) {
    (void)env; (void)cls;
    Handle *h = (Handle *)(intptr_t)pH;
    if (!h) return;
    llama_free(h->ctx);
    llama_model_free(h->model);
    llama_backend_free();
    free(h);
    LOGI("unloaded, memory released");
}

JNIEXPORT jstring JNICALL
Java_ai_serge_dermes_ChatActivity_nativeGenerate(JNIEnv *env, jclass cls, jlong pH,
        jstring jprompt, jobject cb, jmethodID mid, jint maxTokens) {
    (void)cls;
    Handle *h = (Handle *)(intptr_t)pH;
    if (!h || !h->ctx) { throw_exc(env, "not loaded"); return NULL; }
    atomic_store(&h->abort_flag, 0);

    const char *prompt = (*env)->GetStringUTFChars(env, jprompt, NULL);
    int cap = (int)strlen(prompt) / 2 + 32;
    llama_token *toks = malloc((size_t)cap * sizeof(llama_token));
    int n = llama_tokenize(h->vocab, prompt, (int32_t)strlen(prompt),
                           toks, cap, false, true);
    (*env)->ReleaseStringUTFChars(env, jprompt, prompt);
    if (n < 0) { free(toks); throw_exc(env, "tokenize failed"); return NULL; }

    int room = (int)llama_n_ctx(h->ctx) - maxTokens - 8;
    if (n > room) { int drop = n - room; n -= drop; memmove(toks, toks + drop, (size_t)n * sizeof(llama_token)); }
    LOGI("prompt tokens=%d", n);

    struct llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    char *out = malloc(16384);
    out[0] = 0; int outLen = 0;
    int generated = 0;
    const int STEP = 128;

    for (int p = 0; p < n && !atomic_load(&h->abort_flag); p += STEP) {
        int chunk = (n - p < STEP) ? n - p : STEP;
        struct llama_batch b = llama_batch_get_one(toks + p, chunk);
        if (llama_decode(h->ctx, b) != 0) break;
    }

    if (!atomic_load(&h->abort_flag)) {
        while (generated < maxTokens && outLen < 16000) {
            if (atomic_load(&h->abort_flag)) break;
            llama_token tok = llama_sampler_sample(smpl, h->ctx, -1);
            llama_sampler_accept(smpl, tok);
            if (llama_vocab_is_eog(h->vocab, tok)) break;
            char buf[128];
            int len = llama_token_to_piece(h->vocab, tok, buf, sizeof(buf), 0, true);
            if (len > 0 && outLen + len < 16000) {
                memcpy(out + outLen, buf, (size_t)len); outLen += len; out[outLen] = 0;
                jstring js = (*env)->NewStringUTF(env, out);
                (*env)->CallVoidMethod(env, cb, mid, js);
                (*env)->DeleteLocalRef(env, js);
                if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); break; }
            }
            struct llama_batch b = llama_batch_get_one(&tok, 1);
            if (llama_decode(h->ctx, b) != 0) break;
            generated++;
        }
    }

    llama_sampler_free(smpl);
    free(toks);
    llama_memory_clear(llama_get_memory(h->ctx), false); // re-prefill next turn; KV stays tiny
    jstring res = (*env)->NewStringUTF(env, out);
    free(out);
    return res;
}
