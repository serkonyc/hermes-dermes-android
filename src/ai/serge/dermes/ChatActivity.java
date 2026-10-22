package ai.serge.dermes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * In-app chat with the downloaded GGUF via llama.cpp (libdermesllm.so).
 * Phone-safety contract: the model lives in native memory, NOT java heap.
 *  - loaded lazily on first message, on a background thread;
 *  - unloaded 90s after idle, 20s after onPause, IMMEDIATELY on onDestroy
 *    and on onTrimMemory(>=RUNNING_LOW) — the phone never carries it around;
 *  - one generate at a time; Stop aborts the decode loop mid-token;
 *  - mmap + CPU-only, KV capped at 2048 ctx / 512 out tokens.
 * Model files live in getExternalFilesDir("models") — they survive app updates.
 */
public class ChatActivity extends Activity {

    static { System.loadLibrary("dermesllm"); }

    private static native long nativeLoad(String path, int nCtx, int nThreads);
    private static native void nativeFree(long h);
    private static native void nativeAbort(long h);
    private static native String nativeGenerate(long h, String prompt, Object cb,
            java.lang.reflect.Method m, int maxTokens);

    // Safety knobs (Fold-class 16GB phone): 4 threads, 2048 ctx, <=512 out tokens.
    private static final int N_CTX = 2048, N_THREADS = 4, MAX_TOKENS = 512;
    private static final long UNLOAD_IDLE_MS = 90_000L, UNLOAD_PAUSE_MS = 20_000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<String[]> hist = new ArrayList<>(); // {role, text}
    private long h;
    private int state; // 0 unloaded 1 loading 2 ready 3 generating
    private LinearLayout msgs;
    private ScrollView scroll;
    private EditText input;
    private Button send;
    private TextView banner, pending;
    private final Runnable unloadTask = this::unloadNow;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        HistoryActivity.accumulate(this, 1, 0);

        banner = new TextView(this);
        banner.setTextSize(11f);
        banner.setTextColor(UI.TEXT_2);
        banner.setPadding(dp(16), dp(6), dp(16), dp(6));
        banner.setText(modelFile() == null
                ? "no model in storage — download one in Pocket Brain"
                : "model unloads after 90s idle · CPU only · memory freed on close");

        msgs = new LinearLayout(this);
        msgs.setOrientation(LinearLayout.VERTICAL);
        msgs.setPadding(dp(14), dp(4), dp(14), dp(4));
        scroll = new ScrollView(this);
        scroll.addView(msgs);

        input = new EditText(this);
        input.setHint("Message…");
        input.setTextSize(14f);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(UI.TEXT_2);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setBackground(UI.card(this, 14));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));

        send = new Button(this);
        send.setText("Send");
        send.setOnClickListener(v -> onSend());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(6), dp(14), dp(10));
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(send);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UI.headerRow(this, "Neuro Chat", "on-device · nothing leaves the phone"));
        root.addView(banner);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(row);
        setContentView(root);
    }

    // ---------- send / generate ----------

    private void onSend() {
        if (state == 1 || state == 3) { // generating -> this button is Stop
            if (h != 0) nativeAbort(h);
            return;
        }
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        hist.add(new String[]{"user", text});
        bubble("user", text);
        trimHistory();
        if (h == 0) { loadThenGenerate(); } else { generate(); }
    }

    private void loadThenGenerate() {
        File f = modelFile();
        if (f == null) { banner.setText("no model file — download it in Pocket Brain first"); return; }
        state = 1;
        send.setText("Load…");
        banner.setText("loading " + f.getName() + " into RAM…");
        new Thread(() -> {
            long nh;
            try { nh = nativeLoad(f.getPath(), N_CTX, N_THREADS); }
            catch (Throwable t) {
                ui.post(() -> { state = 0; send.setText("Send");
                    banner.setText("load failed: " + t.getMessage()); });
                return;
            }
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) { new Thread(() -> nativeFree(nh)).start(); return; }
                h = nh; state = 2; generate();
            });
        }).start();
    }

    private void generate() {
        state = 3;
        send.setText("Stop");
        scheduleUnload(UNLOAD_IDLE_MS); // safety net if generation hangs
        pending = bubble("assistant", "");
        final String prompt = buildPrompt();
        new Thread(() -> {
            String reply;
            try {
                java.lang.reflect.Method m = ChatActivity.class
                        .getDeclaredMethod("onToken", String.class);
                reply = nativeGenerate(h, prompt, this, m, MAX_TOKENS);
            } catch (Throwable t) { reply = null; }
            final String r = reply;
            ui.post(() -> {
                state = h != 0 ? 2 : 0; // if a trim/pause unload slipped in, stay unloaded
                send.setText("Send");
                if (pending != null) pending.setText(r == null ? "⚠ generate failed" : r);
                hist.add(new String[]{"assistant", r == null ? "" : r});
                pending = null;
                scheduleUnload(UNLOAD_IDLE_MS);
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            });
        }).start();
    }

    // called from native per streamed token group (full partial)
    @SuppressWarnings("unused")
    private void onToken(String partial) {
        ui.post(() -> {
            if (pending != null) { pending.setText(partial);
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN)); }
        });
    }

    // ---------- memory lifecycle ----------

    private void scheduleUnload(long ms) {
        ui.removeCallbacks(unloadTask);
        ui.postDelayed(unloadTask, ms);
    }

    private synchronized void unloadNow() {
        if (state == 3 && h != 0) { // decoder still running — never free the handle mid-decode
            nativeAbort(h);
            ui.postDelayed(unloadTask, 10_000); // give the loop 10s to unwind, then free
            return;
        }
        if (h != 0) {
            long old = h; h = 0; state = 0;
            ui.removeCallbacks(unloadTask);
            send.setText("Send");
            new Thread(() -> nativeFree(old)).start(); // free off the UI thread
            banner.setText("model unloaded — RAM freed");
        } else state = 0;
    }

    @Override protected void onPause() {
        super.onPause();
        if (h != 0) {
            if (state == 3 && h != 0) nativeAbort(h);
            scheduleUnload(UNLOAD_PAUSE_MS); // quick release if user walks away
        }
    }

    @Override protected void onResume() {
        super.onResume();
        ui.removeCallbacks(unloadTask); // came back in time — keep the model
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (h != 0 && (level >= TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_COMPLETE)) {
            if (state == 3) nativeAbort(h);
            unloadNow(); // the OS is starving — give 5 GB back this second
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacks(unloadTask);
        if (h != 0) { long old = h; h = 0; new Thread(() -> nativeFree(old)).start(); }
    }

    // ---------- chat plumbing ----------

    private TextView bubble(String role, String text) {
        TextView tv = new TextView(this);
        tv.setTextSize(14f);
        tv.setText(text);
        tv.setBackground(UI.card(this, 14));
        tv.setPadding(dp(12), dp(8), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.bottomMargin = dp(6);
        if ("user".equals(role)) {
            tv.setTextColor(0xFF0B0E14); tv.setBackgroundColor(UI.ACCENT);
            lp.gravity = Gravity.END;
        } else {
            tv.setTextColor(UI.TEXT_1); lp.gravity = Gravity.START;
        }
        msgs.addView(tv, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return tv;
    }

    private void trimHistory() {
        int total = 0;
        int i = hist.size();
        while (i > 1 && total < 5000) total += hist.get(--i)[1].length();
        if (i > 0) { for (int j = 0; j < i; j++) msgs.removeViewAt(0); hist.subList(0, i).clear(); }
    }

    private String buildPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("<|im_start|>system\nBe terse; reply in user language.\n");
        for (String[] m : hist) {
            sb.append("<|im_start|>" + m[0] + "\n" + m[1] + "\n");
        }
        sb.append("<|im_start|>assistant\n");
        return sb.toString();
    }

    private File modelFile() {
        File dir = new File(getExternalFilesDir(null), "models");
        File[] fs = dir.listFiles((d, n) -> n.endsWith(".gguf"));
        if (fs == null || fs.length == 0) return null;
        for (File f : fs) if (f.getName().startsWith("LFM2.5-8B")) return f;
        return fs[0]; // ponytail: prefer the winner; else newest-first would need sort
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
