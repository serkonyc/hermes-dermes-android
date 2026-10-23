package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

    private static final int N_CTX = 2048, N_THREADS = 4, MAX_TOKENS = 512;
    private static final long UNLOAD_IDLE_MS = 90_000L, UNLOAD_PAUSE_MS = 20_000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<String[]> hist = new ArrayList<>();
    private long h;
    private int state;
    private LinearLayout msgs;
    private ScrollView scroll;
    private EditText input;
    private Button send;
    private TextView status;
    private View historyButton;
    private TextView pending;
    private Animation pulse;
    private boolean pendingThinking;
    private final Runnable unloadTask = this::unloadNow;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);
                loadHistory();

        if (!nativeLoaded) {
            TextView tv = (TextView) UI.footer(this, "Runtime load failed:\n" + nativeError);
            tv.setPadding(UI.space24(this), UI.space32(this), UI.space24(this), 0);
            tv.setGravity(Gravity.NO_GRAVITY);
            setContentView(tv);
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        status = new TextView(this);
        UI.bodySmall(status);
        status.setTextColor(UI.TEXT_2);
        status.setText(modelFile() == null ? "Model not installed · get it in Models" : "Unloads after 90s idle · CPU only");
        status.setPadding(0, 0, 0, UI.space16(this));
        root.addView(status);

        msgs = new LinearLayout(this);
        msgs.setOrientation(LinearLayout.VERTICAL);
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, UI.space8(this), 0, UI.space16(this));
        scroll.addView(msgs);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        input = new EditText(this);
        input.setHint("Message…");
        UI.bodyLarge(input);
        input.setTextColor(UI.TEXT_0);
        input.setHintTextColor(UI.TEXT_2);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setBackground(UI.card(this, 26));
        input.setPadding(UI.space20(this), UI.space16(this), UI.space20(this), UI.space16(this));

        send = new Button(this);
        send.setText("Send");
        UI.buttonFilled(send, this);
        send.setMinWidth(UI.dp(this, 96));
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

        renderSaved();
        historyButton = menuButton();
        setContentView(Shell.wrap(this, Shell.CHAT, "Neuro Chat", "History stays on this phone", root, false, this::finish, historyButton));
    }

    private void onSend() {
        if (state == 1 || state == 3) { if (h != 0) nativeAbort(h); return; }
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        hideKeyboard();
        hist.add(new String[]{"user", text});
        addMessage("user", text);
        persist();
        trimHistory();
        if (h == 0) loadThenGenerate(); else generate();
    }

    private void loadThenGenerate() {
        File f = modelFile();
        if (f == null) { status.setText("Model not installed · get it in Models"); return; }
        state = 1; send.setText("Load…"); status.setText("Loading model into RAM… first reply takes a moment");
        startThinking();
        new Thread(() -> {
            long nh;
            try { nh = nativeLoad(f.getPath(), N_CTX, N_THREADS); }
            catch (Throwable t) {
                ui.post(() -> { state = 0; send.setText("Send"); stopThinking(); status.setText("Load failed: " + t.getMessage()); });
                return;
            }
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) { new Thread(() -> nativeFree(nh)).start(); return; }
                h = nh; state = 2; generate();
            });
        }).start();
    }

    private void generate() {
        state = 3; send.setText("Stop"); scheduleUnload(UNLOAD_IDLE_MS);
        pending = addMessage("assistant", ""); startThinking();
        String prompt = buildPrompt();
        new Thread(() -> {
            String reply;
            try {
                java.lang.reflect.Method m = ChatActivity.class.getDeclaredMethod("onToken", String.class);
                reply = nativeGenerate(h, prompt, this, m, MAX_TOKENS);
            } catch (Throwable t) { reply = null; }
            String r = reply;
            ui.post(() -> {
                state = h != 0 ? 2 : 0; send.setText("Send"); stopThinking();
                String clean = r == null ? "" : stripThink(r);
                if (pending != null) {
                    pending.setText(clean.isEmpty() ? (r == null ? "⚠ Generate failed" : "(empty reply)") : clean);
                    pending.setTextColor(UI.TEXT_0);
                }
                hist.add(new String[]{"assistant", clean});
                persist(); pending = null; scheduleUnload(UNLOAD_IDLE_MS); scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            });
        }).start();
    }

    @SuppressWarnings("unused")
    private void onToken(String partial) {
        ui.post(() -> {
            if (pending == null) return;
            String shown = showStreaming(partial);
            if (!shown.isEmpty()) {
                if (pendingThinking) { stopThinking(); pending.setTextColor(UI.TEXT_0); }
                pending.setText(shown); scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    static String stripThink(String s) {
        if (s == null) return "";
        String r = s.replaceAll("(?is)<[^>/]*think[^>/]*>.*?<[^>]*?/[^>]*think[^>]*>", "");
        return r.replaceAll("(?is)</?[^>]*think[^>]*>", "").trim();
    }
    static String showStreaming(String partial) { return stripThink(partial); }

    private void startThinking() {
        if (pending == null || pendingThinking) return;
        pendingThinking = true; pending.setText("Thinking…"); pending.setTextColor(UI.ACCENT);
        pulse = new AlphaAnimation(1f, .35f); pulse.setDuration(900); pulse.setRepeatMode(Animation.REVERSE); pulse.setRepeatCount(Animation.INFINITE); pending.startAnimation(pulse);
    }
    private void stopThinking() { pendingThinking = false; if (pending != null && pulse != null) pending.clearAnimation(); pulse = null; }

    private void scheduleUnload(long ms) { ui.removeCallbacks(unloadTask); ui.postDelayed(unloadTask, ms); }
    private synchronized void unloadNow() {
        if (state == 3 && h != 0) { nativeAbort(h); ui.postDelayed(unloadTask, 10_000); return; }
        if (h != 0) {
            long old = h; h = 0; state = 0; ui.removeCallbacks(unloadTask); send.setText("Send"); stopThinking();
            new Thread(() -> nativeFree(old)).start(); status.setText("Model unloaded · RAM freed");
        } else state = 0;
    }

    @Override protected void onPause() {
        super.onPause(); if (h != 0) { if (state == 3) nativeAbort(h); scheduleUnload(UNLOAD_PAUSE_MS); }
    }
    @Override protected void onResume() { super.onResume(); ui.removeCallbacks(unloadTask); }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (h != 0 && (level >= TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_COMPLETE)) { if (state == 3) nativeAbort(h); unloadNow(); }
    }
    @Override protected void onDestroy() { super.onDestroy(); ui.removeCallbacks(unloadTask); if (h != 0) { long old = h; h = 0; new Thread(() -> nativeFree(old)).start(); } }

    private TextView addMessage(String role, String text) {
        boolean user = "user".equals(role);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2); clp.bottomMargin = UI.space24(this); col.setLayoutParams(clp);
        TextView label = new TextView(this); UI.labelSmall(label); label.setText(user ? "You" : "Dermes"); label.setTextColor(user ? UI.TEXT_2 : UI.ACCENT); if (user) label.setGravity(Gravity.END); col.addView(label);
        TextView body = new TextView(this); UI.bodyChat(body); body.setText(text); body.setLineSpacing(0, 1.35f);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2); blp.topMargin = UI.space6(this);
        if (user) { blp.gravity = Gravity.END; body.setMaxEms(24); body.setBackground(UI.card(this, 22, UI.ACCENT_SOFT)); body.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space12(this)); }
        else body.setPadding(UI.space4(this), 0, 0, 0);
        col.addView(body, blp); msgs.addView(col); scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN)); return body;
    }

    private void trimHistory() { int total = 0, i = hist.size(); while (i > 1 && total < 5000) total += hist.get(--i)[1].length(); if (i > 0) { for (int j=0;j<i;j++) msgs.removeViewAt(0); hist.subList(0,i).clear(); persist(); } }
    private String buildPrompt() { StringBuilder sb = new StringBuilder(); for (String[] m : hist) sb.append("<|im_start|>").append(m[0]).append('\n').append(m[1]).append('\n'); sb.append("<|im_start|>assistant\n"); return sb.toString(); }

    private void showHistory() {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, historyButton);
        if (hist.isEmpty()) menu.getMenu().add("No saved messages yet");
        int cap = Math.min(hist.size(), 12);
        for (int i = hist.size() - cap; i < hist.size(); i++) {
            String[] m = hist.get(i); String preview = m[1].replaceAll("\\s+", " "); if (preview.length() > 34) preview = preview.substring(0, 33) + "…";
            final int idx = i; menu.getMenu().add((idx + 1) + ". " + ("user".equals(m[0]) ? "You: " : "Dermes: ") + preview).setOnMenuItemClickListener(x -> { jumpTo(idx); return true; });
        }
        menu.getMenu().add("Clear this chat").setOnMenuItemClickListener(x -> { clearHistory(); return true; }); menu.show();
    }
    private void jumpTo(int idx) { scroll.post(() -> { if (idx < msgs.getChildCount()) scroll.smoothScrollTo(0, Math.max(0, msgs.getChildAt(idx).getTop() - UI.space16(this))); }); }
    private void clearHistory() { ui.removeCallbacks(unloadTask); if (h != 0) nativeAbort(h); hist.clear(); msgs.removeAllViews(); persist(); status.setText("Chat cleared"); if (h != 0) scheduleUnload(5_000); }
    private File historyFile() { File d = new File(getFilesDir(), "chat"); d.mkdirs(); return new File(d, "history.json"); }
    private void persist() {
        try {
            org.json.JSONArray a = new org.json.JSONArray();
            for (String[] m : hist) a.put(new org.json.JSONObject().put("role", m[0]).put("text", m[1]));
            try (FileOutputStream o = new FileOutputStream(historyFile())) {
                o.write(a.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }
    private void loadHistory() {
        java.io.File f = historyFile();
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            org.json.JSONArray a = new org.json.JSONArray(readAll(r));
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.getJSONObject(i);
                hist.add(new String[]{o.optString("role"), o.optString("text")});
            }
        } catch (Exception ignored) {}
    }

    private static String readAll(java.io.Reader r) throws java.io.IOException {
        StringBuilder sb = new StringBuilder(); int c;
        while ((c = r.read()) >= 0) sb.append((char)c);
        return sb.toString();
    }

    private ImageButton menuButton() {
        ImageButton b = new ImageButton(this);
        b.setImageResource(android.R.drawable.ic_menu_more);
        b.setBackgroundResource(0);
        b.setImageTintList(android.content.res.ColorStateList.valueOf(UI.TEXT_1));
        b.setPadding(UI.space12(this), UI.space8(this), 0, UI.space8(this));
        b.setContentDescription("Chat history");
        b.setOnClickListener(v -> showHistory());
        return b;
    }

    private void renderSaved() {
        msgs.removeAllViews();
        for (String[] m : hist) addMessage(m[0], m[1]);
        if (!hist.isEmpty()) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager im =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    /** One-shot off-device-thread summarization for Digest; null if model missing/busy. */
    static String summarize(Context c, String prompt) {
        if (!nativeLoaded) return "⚠ native runtime unavailable";
        File d = new File(c.getExternalFilesDir(null), "models");
        File[] fs = d.listFiles((x, n) -> n.endsWith(".gguf"));
        if (fs == null || fs.length == 0) return null;
        File f = null;
        for (File x : fs) if (x.getName().startsWith("LFM2.5-8B")) f = x;
        if (f == null) f = fs[0];
        long h;
        try { h = nativeLoad(f.getPath(), 1024, 4); } catch (Throwable t) { return "⚠ load failed: " + t; }
        try {
            String out = nativeGenerate(h, prompt, null, null, 256);
            return out == null ? null : stripThink(out);
        } catch (Throwable t) { return "⚠ generate failed: " + t; }
        finally { nativeFree(h); }
    }

    private File modelFile() {
        File d = new File(getExternalFilesDir(null), "models");
        File[] fs = d.listFiles((x, n) -> n.endsWith(".gguf"));
        if (fs == null || fs.length == 0) return null;
        for (File f : fs) if (f.getName().startsWith("LFM2.5-8B")) return f;
        return fs[0];
    }
}
