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
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Digest feed: Hermes morning feed (digest.json) plus vendor RSS fetched on the phone,
 * merged into one card list. The local LFM2.5-8B writes the top summary card on-device;
 * offline shows the last cached copy (SharedPreferences, plain string). Tap card -> browser.
 */
public class DigestActivity extends Activity {

    private static final String URL =
            "https://raw.githubusercontent.com/serkonyc/hermes-dermes-digest/main/digest.json";
    private static final String[][] RSS = {
        {"openai",   "https://openai.com/blog/rss.xml"},
        {"anthropic","https://www.anthropic.com/rss.xml"},
        {"google",   "https://blog.google/technology/ai/rss/"},
        {"mistral",  "https://mistral.ai/news/rss.xml"},
        {"hn",       "https://hn.algolia.com/api/v1/search?tags=front_page&hitsPerPage=8"},
    };
    private static final String PREFS = "digest";
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private TextView header, summaryBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);
        header = new TextView(this);
        UI.labelSmall(header);
        header.setTextColor(UI.TEXT_2);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(10), 0, dp(6));
        header.setText("digest · updating…");

        summaryBox = new TextView(this);
        UI.bodyMedium(summaryBox);
        summaryBox.setTextColor(UI.TEXT_1);
        summaryBox.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space12(this));
        summaryBox.setBackground(UI.card(this, 18));
        summaryBox.setVisibility(View.GONE);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(UI.space16(this), 0, UI.space16(this), UI.space16(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(UI.space16(this), 0, UI.space16(this), 0);
        col.addView(summaryBox);
        LinearLayout listHolder = new LinearLayout(this);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        listHolder.addView(list);
        col.addView(listHolder);
        scroll.addView(col);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(header);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(UI.footer(this, "v" + UI.version(this)));

        setContentView(Shell.wrap(this, -1, "Digest", "hermes feed + vendor RSS · local summary", content, true, this::finish));

        String cachedSummary = getPrefs().getString("summary", "");
        if (!cachedSummary.isEmpty()) showSummary(cachedSummary + " (cached)");
        render(getCache(), true);
        refresh();
    }

    private void refresh() {
        pool.execute(() -> {
            String body = null;
            try { body = DermesActivity.httpGetPublic(URL); } catch (Exception ignored) {}
            final String fBody = body;
            final List<Item> extra = new ArrayList<>();
            for (String[] src : RSS) {
                try {
                    String b = DermesActivity.httpGetPublic(src[1]);
                    extra.addAll(src[0].equals("hn") ? parseHn(b) : parseRss(src[0], b));
                } catch (Exception ignored) {}
            }
            // on-device summary by the local model (offline thread; ~seconds)
            String summary = null;
            try {
                StringBuilder lines = new StringBuilder();
                if (fBody != null && fBody.startsWith("{")) {
                    JSONArray items = new JSONObject(fBody).optJSONArray("items");
                    for (int i = 0; items != null && i < items.length() && i < 10; i++)
                        lines.append("- ").append(items.getJSONObject(i).optString("title")).append('\n');
                }
                for (int i = 0; i < extra.size() && i < 15; i++)
                    lines.append("- ").append(extra.get(i).title).append('\n');
                if (lines.length() > 0)
                    summary = ChatActivity.summarize(this,
                            "Сожми до 4 пунктов по-русски: что важного сегодня в мире LLM для CTO. Без воды, пункты через дефис:\n" + lines);
            } catch (Throwable ignored) {}
            final String fSummary = summary != null && !summary.trim().isEmpty() ? summary.trim() : null;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (fBody != null && fBody.startsWith("{")) {
                    putCache(fBody);
                    render(fBody, false);
                }
                StringBuilder hdr = new StringBuilder("digest · ")
                        .append(DateFormat.getTimeInstance().format(System.currentTimeMillis()));
                hdr.append(fBody != null && fBody.startsWith("{") ? " · hermes feed" : " · offline");
                if (!extra.isEmpty()) hdr.append(" · +").append(extra.size()).append(" rss");
                header.setText(hdr);
                for (Item it : extra) list.addView(card(it.tag, it.title, it.summary, it.url));
                if (fSummary != null) {
                    getPrefs().edit().putString("summary", fSummary).apply();
                    showSummary(fSummary);
                }
            });
        });
    }

    private void showSummary(String s) {
        summaryBox.setText("Итог (локальная нейросеть)\n\n" + s);
        summaryBox.setVisibility(View.VISIBLE);
    }

    static final class Item { String tag, title, summary, url;
        Item(String t, String ti, String s, String u) { tag=t; title=ti; summary=s; url=u; } }

    private List<Item> parseRss(String tag, String xml) throws Exception {
        List<Item> out = new ArrayList<>();
        XmlPullParser p = android.util.Xml.newPullParser();
        p.setInput(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))), null);
        String title = null, link = null, desc = null, cur = null;
        boolean inItem = false;
        StringBuilder txt = new StringBuilder();
        int ev;
        while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                cur = p.getName(); txt.setLength(0);
                if ("item".equalsIgnoreCase(cur) || "entry".equalsIgnoreCase(cur)) { inItem = true; title = link = desc = null; }
            } else if (ev == XmlPullParser.TEXT) {
                txt.append(p.getText());
            } else if (ev == XmlPullParser.END_TAG && inItem) {
                String n = p.getName(), v = txt.toString().trim();
                if ("title".equalsIgnoreCase(n)) title = v;
                else if ("link".equalsIgnoreCase(n) && !v.isEmpty()) link = v;
                else if ("link".equalsIgnoreCase(n) && p.getAttributeValue(null, "href") != null) link = p.getAttributeValue(null, "href");
                else if ("description".equalsIgnoreCase(n) || "summary".equalsIgnoreCase(n)) desc = v;
                else if ("item".equalsIgnoreCase(n) || "entry".equalsIgnoreCase(n)) {
                    inItem = false;
                    if (title != null && !title.isEmpty())
                        out.add(new Item(tag, title, stripHtml(desc == null ? "" : desc), link == null ? "" : link));
                }
            }
        }
        return out;
    }

    private List<Item> parseHn(String json) throws Exception {
        List<Item> out = new ArrayList<>();
        JSONArray hits = new JSONObject(json).optJSONArray("hits");
        for (int i = 0; hits != null && i < hits.length() && i < 8; i++) {
            JSONObject h = hits.getJSONObject(i);
            String t = h.optString("title");
            if (t.isEmpty()) continue;
            String u = h.optString("url", "https://news.ycombinator.com/item?id=" + h.optString("objectID"));
            out.add(new Item("hn", t, h.optInt("points") + " pts · " + h.optInt("num_comments") + " comments", u));
        }
        return out;
    }

    private String stripHtml(String s) {
        return s.replaceAll("<[^>]*>", "").replaceAll("&[a-z#0-9]+;", " ").trim();
    }

    private void render(String json, boolean fromCache) {
        list.removeAllViews();
        if (json.isEmpty()) return;
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
            t.setTextColor(UI.TEXT_1);
            list.addView(t);
        }
    }

    private View card(String tag, String title, String summary, String url) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = UI.card(DigestActivity.this, 18);
        c.setBackground(bg);
        c.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space14(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UI.space10(this);
        c.setLayoutParams(lp);

        TextView tg = new TextView(this);
        tg.setText(tag.toUpperCase(java.util.Locale.US));
        UI.labelSmall(tg);
        tg.setTextColor(accent(tag));
        TextView ti = new TextView(this);
        ti.setText(title);
        UI.titleMedium(ti);
        ti.setPadding(0, UI.space4(this), 0, UI.space4(this));
        TextView su = new TextView(this);
        su.setText(summary);
        UI.bodySmall(su);

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
            case "llm": return UI.ACCENT;
            case "local": return UI.OK;
            case "sf": case "salesforce": return UI.VIOLET;
            default: return UI.TEAL;
        }
    }

    private android.content.SharedPreferences getPrefs() { return getSharedPreferences(PREFS, 0); }
    private String getCache() { return getPrefs().getString("json", ""); }
    private void putCache(String s) { getPrefs().edit().putString("json", s).apply(); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
