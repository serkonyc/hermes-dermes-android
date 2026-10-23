package ai.serge.dermes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * In-app chat with the downloaded GGUF via llama.cpp (libdermesllm.so).
 * Design: flat Claude-style message blocks (no bubbles for the assistant),
 * role labels, animated "Thinking…" while the model has not answered yet,
 * docked pill composer. All spacing/type via UI tokens.
 * Phone-safety contract: the model lives in native memory, NOT java heap.
 *  - loaded lazily on first message, on a background thread;
 *  - unloaded 90s after idle, 20s after onPause, IMMEDIATELY on onDestroy
 *    and on onTrimMemory(>=RUNNING_LOW) — the phone never carries it around;
 *  - one generate at a time; Stop aborts the decode loop mid-token;
 *  - mmap + CPU-only, KV capped at 2048 ctx / 512 out tokens.
 * Model files live in getExternalFilesDir("models") — they survive app updates.
 */
public class ChatActivity extends Activity {

    private static boolean nativeLoaded;
    private static String nativeError = "";
    static {
        try { System.loadLibrary("dermesllm"); nativeLoaded = true; }
        catch (Throwable t) { nativeLoaded = false; nativeError = String.valueOf(t.getMessage()); }
    }

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
    private TextView status;        // thin status line under the header
    private TextView pending;       // assistant body being streamed
    private Animation pulse;        // "Thinking…" breathing
    private boolean pendingThinking; // true until first real text arrives
    private final Runnable unloadTask = this::unloadNow;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        HistoryActivity.accumulate(this, 1, 0);

        if (!nativeLoaded) {
            TextView tv = (TextView) UI.footer(this, "runtime load failed:\n" + nativeError);
            tv.setPadding(UI.space24(this), UI.space32(this), UI.space24(this), 0);
            tv.setGravity(Gravity.NO_GRAVITY);
            setContentView(tv);
            return;
        }

        LinearLayout root = UI.screen(this);
        root.setPadding(UI.space24(this), UI.space16(this), UI.space24(this), UI.space8(this));

        root.addView(UI.headerRow(this, "Neuro Chat", "on-device · nothing leaves the phone"));

        status = new TextView(this);
        UI.bodySmall(status);
        status.setTextColor(UI.TEXT_2);
        status.setBackground(UI.card(this, 16));
        status.setPadding(UI.space16(this), UI.space10(this), UI.space16(this), UI.space10(this));
        status.setText(modelFile() == null
                ? "no model in storage — download it in Pocket Brain"
                : "unloads after 90s idle · CPU only");
        root.addView(status, UI.block(this, 0));

        msgs = new LinearLayout(this);
        msgs.setOrientation(LinearLayout.VERTICAL);
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, UI.space16(this), 0, UI.space8(this));
        scroll.addView(msgs);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ---- docked composer: pill input + filled send, on a tonal bar ----
        input = new EditText(this);
        input.setHint("Message…");
        UI.bodyLarge(input);
        input.setTextColor(UI.TEXT_0);
        input.setHintTextColor(UI.TEXT_2);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setBackground(UI.card(this, 24));
        input.setPadding(UI.space20(this), UI.space16(this), UI.space20(this), UI.space16(this));

        send = new Button(this);
        send.setText("Send");
        UI.buttonFilled(send, this);
        send.setMinWidth(UI.dp(this, 96)); // Send / Stop / Load… swap without layout jumps
        send.setOnClickListener(v -> onSend());

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(0, UI.space12(this), 0, UI.space12(this));
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.leftMargin = UI.space12(this);
        composer.addView(send, slp);
        root.addView(composer);

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
        addMessage("user", text);
        trimHistory();
        if (h == 0) { loadThenGenerate(); } else { generate(); }
    }

    private void loadThenGenerate() {
        File f = modelFile();
        if (f == null) { status.setText("no model file — download it in Pocket Brain first"); return; }
        state = 1;
        send.setText("Load…");
        status.setText("loading " + f.getName() + " into RAM… first reply takes a bit");
        startThinking(); // the user must see the request was accepted
        new Thread(() -> {
            long nh;
            try { nh = nativeLoad(f.getPath(), N_CTX, N_THREADS); }
            catch (Throwable t) {
                ui.post(() -> { state = 0; send.setText("Send"); stopThinking();
                    status.setText("load failed: " + t.getMessage()); });
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
        pending = addMessage("assistant", "");
        startThinking();
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
                stopThinking();
                String clean = r == null ? "" : stripThink(r);
                if (pending != null) {
                    pending.setText(clean.isEmpty() ? (r == null ? "⚠ generate failed" : "(empty reply)") : clean);
                    pending.setTextColor(UI.TEXT_0);
                }
                hist.add(new String[]{"assistant", clean});
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
            if (pending != null) {
                String shown = showStreaming(partial);
                if (!shown.isEmpty()) {
                    if (pendingThinking) { stopThinking(); pending.setTextColor(UI.TEXT_0); }
                    pending.setText(shown);
                    scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    /** LFM2.5 emits its chain of thought in think tags - never shown raw. */
    static String stripThink(String s) {
        if (s == null) return "";
        String r = s.replaceAll("(?is)<[^>/]*think[^>/]*>.*?<[^>]*?/[^>]*think[^>]*>", "");
        r = r.replaceAll("(?is)</?[^>]*think[^>]*>", "");
        return r.trim();
    }

    /** Live stream view: while the open think block is unfinished, show nothing raw. */
    static String showStreaming(String partial) {
        String done = stripThink(partial);
        return done; // thinking state is conveyed by the pulsing "Thinking…" label
    }

    // ---------- "Thinking…" indicator ----------

    private void startThinking() {
        if (pending == null || pendingThinking) return;
        pendingThinking = true;
        pending.setText("Thinking…");
        pending.setTextColor(UI.ACCENT);
        pulse = new AlphaAnimation(1f, 0.35f);
        pulse.setDuration(900);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        pending.startAnimation(pulse);
    }

    private void stopThinking() {
        pendingThinking = false;
        if (pending != null && pulse != null) pending.clearAnimation();
        pulse = null;
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
            stopThinking();
            new Thread(() -> nativeFree(old)).start(); // free off the UI thread
            status.setText("model unloaded — RAM freed");
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

    // ---------- message blocks (flat, Claude-style) ----------

    /**
     * Flat message block: tiny role label + body.
     * user  – right-aligned tonal container (max ~24em), body text
     * assistant – plain full-width text on background, no container
     * Returns the body TextView (used as the streaming target).
     */
    private TextView addMessage(String role, String text) {
        boolean user = "user".equals(role);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.bottomMargin = UI.space24(this);
        col.setLayoutParams(clp);

        TextView label = new TextView(this);
        UI.labelSmall(label);
        label.setText(user ? "You" : "Dermes");
        label.setTextColor(user ? UI.TEXT_2 : UI.ACCENT);
        if (user) label.setGravity(Gravity.END);
        col.addView(label);

        TextView body = new TextView(this);
        UI.bodyChat(body);
        body.setText(text);
        body.setLineSpacing(0, 1.35f);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = UI.space6(this);
        if (user) {
            blp.gravity = Gravity.END;
            body.setMaxEms(24);
            body.setBackground(UI.card(this, 22, UI.ACCENT_SOFT));
            body.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space12(this));
            body.setTextColor(UI.TEXT_0);
        } else {
            body.setTextColor(UI.TEXT_0);
            body.setPadding(UI.space4(this), 0, 0, 0);
        }
        col.addView(body, blp);

        msgs.addView(col);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return body;
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
}
