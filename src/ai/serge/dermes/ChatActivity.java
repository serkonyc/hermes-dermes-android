package ai.serge.dermes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;

/**
 * Asynchronous chat with Hermes over the shared GitHub repo (hermes-dermes-digest).
 * Protocol: phone POSTs an issue "Dermes: <prompt>" via GitHub REST (no token needed to
 * create in a public repo? no — it does: user provides once, stored locally). Hermes runs
 * a cron watcher that answers as an issue comment, then the phone polls comments.
 *
 * The GitHub token is typed once into the app, kept in SharedPreferences (phone-only),
 * sent only to api.github.com over TLS. No token -> read-only mode: you still see past
 * threads but cannot send.
 */
public class ChatActivity extends Activity {

    private static final String REPO = "serkonyc/hermes-dermes-digest";
    private static final String PREFS = "chat";
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private LinearLayout msgs;
    private EditText input;
    private Button send;
    private TextView hint;
    private String openIssue = null;   // URL fragment number of the thread we're in
    private long seenCommentId = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        msgs = new LinearLayout(this);
        msgs.setOrientation(LinearLayout.VERTICAL);
        msgs.setPadding(dp(14), dp(8), dp(14), dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(msgs);

        input = new EditText(this);
        input.setHint("message to Hermes…");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF6E7681);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMaxLines(3);
        input.setBackgroundColor(0xFF161B22);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setTextSize(14f);

        send = new Button(this);
        send.setText("Send");
        send.setOnClickListener(v -> send());

        hint = new TextView(this);
        hint.setTextSize(11f);
        hint.setTextColor(0xFF6E7681);
        hint.setGravity(Gravity.CENTER);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), 0, dp(14), dp(8));
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(send, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D1117);
        root.setFitsSystemWindows(true);
        root.addView(hint);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(row);
        setContentView(root);

        if (token() == null || token().isEmpty()) {
            showTokenDialog();
        } else {
            hint.setText("chat with Hermes via GitHub · thread #" + (openIssue == null ? "new" : openIssue));
        }
    }

    // ---------- token ----------
    private String token() { return getSharedPreferences(PREFS, 0).getString("gh", null); }
    private void saveToken(String t) { getSharedPreferences(PREFS, 0).edit().putString("gh", t.trim()).apply(); }

    private void showTokenDialog() {
        hint.setText("GitHub token (repo scope) enables sending; Hermes answers as comments.\nPaste token below and Send once to store it:");
        send.setText("Store");
        send.setOnClickListener(v -> {
            String t = input.getText().toString().trim();
            if (t.startsWith("ghp_") || t.startsWith("github_pat_")) {
                saveToken(t);
                input.setText("");
                send.setText("Send");
                send.setOnClickListener(x -> send());
                hint.setText("token stored on this phone only · chat ready");
            } else {
                hint.setText("looks not like a GitHub token (ghp_… / github_pat_…)");
            }
        });
    }

    // ---------- protocol ----------
    private void send() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        final String tk = token();
        if (tk == null || tk.isEmpty()) { showTokenDialog(); return; }
        input.setText("");
        addBubble("me", text);
        pool.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (openIssue == null) {
                    body.put("title", "Dermes: " + (text.length() > 60 ? text.substring(0, 60) : text));
                    body.put("body", text);
                    String resp = DermesActivity.httpPost(
                            "https://api.github.com/repos/" + REPO + "/issues", tk, body.toString());
                    JSONObject iss = new JSONObject(resp);
                    openIssue = iss.getString("number");
                    seenCommentId = 0;   // fresh thread: only future answers count
                } else {
                    body.put("body", text);
                    DermesActivity.httpPost(
                            "https://api.github.com/repos/" + REPO + "/issues/" + openIssue + "/comments",
                            tk, body.toString());
                }
                runOnUiThread(() -> ui.postDelayed(this::pollAnswer, 8000));  // Hermes cron ~5 min; poll politely
            } catch (Exception e) {
                runOnUiThread(() -> addBubble("err", "send failed: " + e.getMessage()));
            }
        });
    }

    /** A reply is from Hermes if the login is the bot itself or carries the marker.
     *  (Watcher runs under the user's account, so login alone cannot identify it.) */
    private static boolean isHermes(String login, String body) {
        return "hermes-bot".equals(login) || body.contains("[hermes]");
    }

    private void pollAnswer() {
        if (isDestroyed() || openIssue == null) return;
        pool.execute(() -> {
            try {
                String arr = DermesActivity.httpGetPublic(
                        "https://api.github.com/repos/" + REPO + "/issues/" + openIssue + "/comments");
                JSONArray cs = new JSONArray(arr);
                for (int i = 0; i < cs.length(); i++) {
                    JSONObject c = cs.getJSONObject(i);
                    long id = c.getLong("id");
                    if (id <= seenCommentId) continue;
                    seenCommentId = id;
                    String login = c.optJSONObject("user").optString("login");
                    final String body = c.optString("body");
                    if (isHermes(login, body)) {
                        runOnUiThread(() -> addBubble("hermes",
                                body.replace("[hermes]", "").trim()));
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void addBubble(String who, String text) {
        TextView t = new TextView(this);
        t.setText(("hermes".equals(who) ? "Hermes · " : "me · ")
                + DateFormat.getTimeInstance().format(System.currentTimeMillis()) + "\n" + text);
        t.setTextSize(13f);
        int bg = "me".equals(who) ? 0xFF1F6FEB : "err".equals(who) ? 0xFF6E2B2B : 0xFF21262D;
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(bg); g.setCornerRadius(dp(12));
        t.setBackground(g);
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(dp(12), dp(8), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        t.setLayoutParams(lp);
        msgs.addView(t);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
