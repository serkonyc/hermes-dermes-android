package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Digest: Hermes feed + vendor RSS as a categorized news reader.
 * Sections: "Все", then tag groups (vendor/category), each collapsible. Search box filters
 * cards live; tag chips work as multi-filters. Local LFM2.5-8B writes the top summary;
 * stripThink-style preambles ("Вот краткое…") are cut before display. Offline = cached copy.
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

    private final List<Item> all = new ArrayList<>();
    private final Map<String, Boolean> collapsed = new LinkedHashMap<>();
    private final java.util.Set<String> activeTags = new java.util.LinkedHashSet<>();
    private String query = "";
    private boolean showAll = true;

    private LinearLayout list, chipsRow;
    private TextView header, summaryBox, countLabel;
    private EditText search;

    static final class Item { String tag, cat, title, summary, url; boolean expanded;
        Item(String t, String c, String ti, String s, String u) { tag=t; cat=c; title=ti; summary=s; url=u; } }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);

        header = new TextView(this);
        UI.labelSmall(header);
        header.setTextColor(UI.TEXT_2);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(6), 0, dp(6));
        header.setText("digest · updating…");

        summaryBox = new TextView(this);
        UI.bodyMedium(summaryBox);
        summaryBox.setTextColor(UI.TEXT_1);
        summaryBox.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space12(this));
        summaryBox.setBackground(UI.card(this, 18));
        summaryBox.setVisibility(View.GONE);

        search = new EditText(this);
        search.setHint("поиск по заголовкам и описаниям…");
        UI.bodyMedium(search);
        search.setHintTextColor(UI.TEXT_2);
        search.setSingleLine(true);
        search.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space12(this));
        search.setBackground(UI.card(this, 16));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable e) { query = e.toString().trim().toLowerCase(Locale.US); rebuild(); }
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
        });

        chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.addView(chipsRow);
        LinearLayout.LayoutParams chlp = new LinearLayout.LayoutParams(-1, -2);
        chlp.topMargin = UI.space10(this);
        chipsScroll.setLayoutParams(chlp);

        countLabel = new TextView(this);
        UI.labelSmall(countLabel);
        countLabel.setTextColor(UI.TEXT_2);
        countLabel.setPadding(UI.space4(this), UI.space10(this), 0, UI.space4(this));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(UI.space16(this), 0, UI.space16(this), UI.space16(this));
        col.addView(summaryBox);
        col.addView(search);
        col.addView(chipsScroll);
        col.addView(countLabel);
        col.addView(list);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(col);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(header);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(UI.footer(this, "v" + UI.version(this)));
        setContentView(Shell.wrap(this, -1, "Digest", "новости по категориям · поиск · фильтры", content, false, this::finish));

        String cachedSummary = getPrefs().getString("summary", "");
        if (!cachedSummary.isEmpty()) showSummary(cachedSummary + " (cached)");

        // cached items first (persisted merged list), then live refresh
        try {
            String cachedItems = getPrefs().getString("items", "");
            if (!cachedItems.isEmpty()) { all.clear(); all.addAll(fromJson(cachedItems)); }
        } catch (Exception ignored) {}
        rebuild();
        refresh();
    }

    // ---------- data ----------
    private void refresh() {
        pool.execute(() -> {
            String body = null;
            try { body = DermesActivity.httpGetPublic(URL); } catch (Exception ignored) {}
            final String fBody = body;
            final List<Item> merged = new ArrayList<>();
            List<Item> hermes = new ArrayList<>();
            if (fBody != null && fBody.startsWith("{")) { try { hermes.addAll(parseHermes(fBody)); } catch (Exception ignored) {} }
            merged.addAll(hermes);
            for (String[] src : RSS) {
                try {
                    String b = DermesActivity.httpGetPublic(src[1]);
                    merged.addAll(src[0].equals("hn") ? parseHn(b) : parseRss(src[0], b));
                } catch (Exception ignored) {}
            }
            String summary = null;
            try {
                StringBuilder lines = new StringBuilder();
                for (int i = 0; i < merged.size() && i < 20; i++)
                    lines.append("- ").append(merged.get(i).title).append('\n');
                if (lines.length() > 0)
                    summary = ChatActivity.summarize(this,
                            "Только список, без вступлений и заключений. Сожми до 4 пунктов по-русски: что важного сегодня в мире LLM для CTO. Формат: дефис, текст. Первый символ ответа — дефис:\\n" + lines);
            } catch (Throwable ignored) {}
            final String fSummary = cleanSummary(summary);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (!merged.isEmpty()) {
                    all.clear(); all.addAll(merged);
                    try { getPrefs().edit().putString("items", toJson(merged)).apply(); } catch (Exception ignored) {}
                    header.setText("digest · " + DateFormat.getTimeInstance().format(System.currentTimeMillis())
                            + " · " + merged.size() + " новостей");
                } else {
                    header.setText("digest · offline · " + all.size() + " новостей из кэша");
                }
                if (fSummary != null) {
                    getPrefs().edit().putString("summary", fSummary).apply();
                    showSummary(fSummary);
                }
                rebuild();
            });
        });
    }

    private List<Item> parseHermes(String json) throws Exception {
        List<Item> out = new ArrayList<>();
        JSONArray items = new JSONObject(json).optJSONArray("items");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            out.add(new Item(it.optString("tag", "hermes"), catOf(it.optString("tag", "hermes")),
                    it.optString("title"), it.optString("summary"), it.optString("url")));
        }
        return out;
    }

    /** Model sometimes prefixes "Вот ваши пункты:" - cut everything before the first dash line. */
    static String cleanSummary(String raw) {
        if (raw == null) return null;
        String s = ChatActivity.stripThink(raw).trim();
        if (s.isEmpty()) return null;
        String[] lines = s.split("\n");
        int first = -1;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.startsWith("- ") || l.startsWith("– ")) { first = i; break; }
        }
        if (first < 0) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = first; i < lines.length; i++) sb.append(lines[i]).append('\n');
        return sb.toString().trim();
    }

    private void showSummary(String s) {
        summaryBox.setText("Итог (локальная нейросеть)\n\n" + s);
        summaryBox.setVisibility(View.VISIBLE);
    }

    // ---------- filtering & rendering ----------
    private String catOf(String tag) {
        String t = tag.toLowerCase(Locale.US);
        switch (t) {
            case "openai": case "anthropic": case "google": case "mistral": return "вендоры";
            case "hn": return "HN";
            case "llm": return "модели";
            case "local": return "локальные LLM";
            case "sf": case "salesforce": return "Salesforce";
            default: return "другое";
        }
    }

    private void rebuild() {
        list.removeAllViews();
        chipsRow.removeAllViews();

        // tag universe in stable order
        java.util.LinkedHashMap<String, int[]> counts = new java.util.LinkedHashMap<>(); // cat -> [total, shown]
        for (Item it : all) {
            int[] c = counts.get(it.cat);
            if (c == null) counts.put(it.cat, c = new int[2]);
            c[0]++;
        }
        // chips: "Все" + categories
        TextView allChip = chip("Все", activeTags.isEmpty() && query.isEmpty());
        allChip.setOnClickListener(v -> { activeTags.clear(); query = ""; search.setText(""); showAll = true; rebuild(); });
        chipsRow.addView(allChip);
        for (String cat : counts.keySet()) {
            final boolean on = activeTags.contains(cat);
            TextView cc = chip(cat + " " + counts.get(cat)[0], on);
            final String fcat = cat;
            cc.setOnClickListener(v -> {
                if (activeTags.contains(fcat)) activeTags.remove(fcat); else activeTags.add(fcat);
                showAll = activeTags.isEmpty() && query.isEmpty();
                rebuild();
            });
            chipsRow.addView(cc);
        }

        int shown = 0;
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            String cat = e.getKey();
            List<Item> items = new ArrayList<>();
            for (Item it : all) {
                if (!it.cat.equals(cat)) continue;
                if (!activeTags.isEmpty() && !activeTags.contains(cat)) continue;
                if (!query.isEmpty() && !((it.title + " " + it.summary).toLowerCase(Locale.US).contains(query))) continue;
                items.add(it);
            }
            if (items.isEmpty()) continue;
            shown += items.size();
            boolean col = Boolean.TRUE.equals(collapsed.get(cat)) && query.isEmpty() && activeTags.isEmpty();
            list.addView(sectionHead(cat, items.size(), col));
            if (!col) for (Item it : items) list.addView(card(it));
        }
        countLabel.setText(shown + " из " + all.size() + (query.isEmpty() ? "" : " · поиск: «" + query + "»"));
        if (shown == 0) {
            TextView t = new TextView(this);
            t.setText(all.isEmpty() ? "пусто — потяни свежее" : "ничего не найдено");
            UI.bodyMedium(t);
            t.setTextColor(UI.TEXT_2);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, UI.space32(this), 0, 0);
            list.addView(t);
        }
    }

    private TextView chip(String text, boolean on) {
        TextView c = new TextView(this);
        c.setText(text);
        UI.labelSmall(c);
        c.setTextColor(on ? 0xFF10141A : UI.TEXT_1);
        c.setBackgroundColor(on ? UI.ACCENT : (UI.BACKGROUND));
        c.setPadding(UI.space12(this), UI.space6(this), UI.space12(this), UI.space6(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = UI.space8(this);
        c.setLayoutParams(lp);
        return c;
    }

    private View sectionHead(String cat, int n, boolean isCollapsed) {
        TextView h = new TextView(this);
        h.setText((isCollapsed ? "▶ " : "▼ ") + cat.toUpperCase(Locale.US) + " · " + n);
        UI.labelMedium(h);
        h.setTextColor(UI.TEXT_1);
        h.setLetterSpacing(0.06f);
        h.setPadding(UI.space4(this), UI.space16(this), 0, UI.space8(this));
        h.setOnClickListener(v -> {
            collapsed.put(cat, !isCollapsed);
            rebuild();
        });
        return h;
    }

    private View card(final Item it) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.card(this, 18));
        c.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space14(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UI.space10(this);
        c.setLayoutParams(lp);

        TextView tg = new TextView(this);
        tg.setText(it.tag.toUpperCase(Locale.US));
        UI.labelSmall(tg);
        tg.setTextColor(accent(it.tag));
        TextView ti = new TextView(this);
        ti.setText(it.title);
        UI.titleMedium(ti);
        ti.setPadding(0, UI.space4(this), 0, UI.space4(this));
        final TextView su = new TextView(this);
        su.setText(it.expanded ? it.summary : brief(it.summary));
        UI.bodySmall(su);
        c.addView(tg); c.addView(ti); c.addView(su);
        if (it.summary.length() > 160) {
            su.append(" …ещё");
            su.setOnClickListener(v -> { it.expanded = !it.expanded;
                su.setText(it.expanded ? it.summary : brief(it.summary) + " …ещё"); });
        }
        if (it.url != null && it.url.startsWith("http")) {
            c.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(it.url))); }
                catch (Exception ignored) {}
            });
        }
        return c;
    }

    private String brief(String s) { return s.length() <= 160 ? s : s.substring(0, 160) + "…"; }

    private int accent(String tag) {
        switch (tag == null ? "" : tag.toLowerCase(Locale.US)) {
            case "llm": return UI.ACCENT;
            case "local": return UI.OK;
            case "sf": case "salesforce": return UI.VIOLET;
            default: return UI.TEAL;
        }
    }

    // ---------- persistence ----------
    private String toJson(List<Item> items) throws Exception {
        JSONArray a = new JSONArray();
        for (Item it : items) {
            JSONObject o = new JSONObject();
            o.put("tag", it.tag); o.put("cat", it.cat); o.put("title", it.title);
            o.put("summary", it.summary); o.put("url", it.url);
            a.put(o);
        }
        return a.toString();
    }
    private List<Item> fromJson(String s) throws Exception {
        List<Item> out = new ArrayList<>();
        JSONArray a = new JSONArray(s);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            out.add(new Item(o.optString("tag"), o.optString("cat", "другое"),
                    o.optString("title"), o.optString("summary"), o.optString("url")));
        }
        return out;
    }

    // ---------- parsers ----------
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
                        out.add(new Item(tag, catOf(tag), title, stripHtml(desc == null ? "" : desc), link == null ? "" : link));
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
            out.add(new Item("hn", catOf("hn"), t, h.optInt("points") + " pts · " + h.optInt("num_comments") + " comments", u));
        }
        return out;
    }

    private String stripHtml(String s) {
        return s.replaceAll("<[^>]*>", "").replaceAll("&[a-z#0-9]+;", " ").trim();
    }

    private android.content.SharedPreferences getPrefs() { return getSharedPreferences(PREFS, 0); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
