package ai.serge.dermes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;

/**
 * Digest feed: styled cards from hermes-dermes-digest/digest.json (published by the
 * Hermes cron that builds Serge's morning digest). Tap card -> browser. Offline shows
 * the last cached copy (kept in SharedPreferences, plain string).
 */
public class DigestActivity extends Activity {

    private static final String URL =
            "https://raw.githubusercontent.com/serkonyc/hermes-dermes-digest/main/digest.json";
    private static final String PREFS = "digest";
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private TextView header;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        header = new TextView(this);
        header.setTextSize(11f);
        header.setTextColor(0xFF6E7681);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(10), 0, dp(6));
        header.setText("digest · updating…");

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), 0, dp(16), dp(16));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UI.BG);
        root.setFitsSystemWindows(true);
        root.addView(header);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        render(getCache(), true);
        refresh();
    }

    private void refresh() {
        pool.execute(() -> {
            String body = null;
            try { body = DermesActivity.httpGetPublic(URL); } catch (Exception ignored) {}
            final String fBody = body;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (fBody != null && fBody.startsWith("{")) {
                    putCache(fBody);
                    render(fBody, false);
                    header.setText("digest · " + DateFormat.getTimeInstance()
                            .format(System.currentTimeMillis()) + " · pulled from GitHub");
                } else {
                    header.setText("digest · offline · cached copy");
                }
            });
        });
    }

    private void render(String json, boolean fromCache) {
        list.removeAllViews();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray items = root.optJSONArray("items");
            if (items == null) return;
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                list.addView(card(it.optString("tag"), it.optString("title"),
                        it.optString("summary"), it.optString("url")));
            }
        } catch (Exception e) {
            TextView t = new TextView(this);
            t.setText("digest unreadable: " + e);
            t.setTextColor(0xFFD29922);
            list.addView(t);
        }
    }

    private View card(String tag, String title, String summary, String url) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = UI.card(DigestActivity.this, 18);
        c.setBackground(bg);
        c.setPadding(dp(16), dp(12), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);

        TextView tg = new TextView(this);
        tg.setText(tag.toUpperCase(java.util.Locale.US));
        tg.setTextSize(9f);
        tg.setTextColor(accent(tag));
        TextView ti = new TextView(this);
        ti.setText(title);
        ti.setTextSize(15f);
        ti.setTextColor(0xFFFFFFFF);
        ti.setPadding(0, dp(2), 0, dp(4));
        TextView su = new TextView(this);
        su.setText(summary);
        su.setTextSize(12f);
        su.setTextColor(0xFF9DA7B3);

        c.addView(tg); c.addView(ti); c.addView(su);
        if (url != null && url.startsWith("http")) {
            c.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception ignored) {}
            });
        }
        return c;
    }

    private int accent(String tag) {
        switch (tag == null ? "" : tag.toLowerCase(java.util.Locale.US)) {
            case "llm": return 0xFF58A6FF;
            case "local": return 0xFF3FB950;
            case "sf": case "salesforce": return 0xFFBC8CFF;
            default: return 0xFFD29922;
        }
    }

    private String getCache() { return getSharedPreferences(PREFS, 0).getString("json", ""); }
    private void putCache(String s) { getSharedPreferences(PREFS, 0).edit().putString("json", s).apply(); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
