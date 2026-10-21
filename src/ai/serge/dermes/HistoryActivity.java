package ai.serge.dermes;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * App activity: one bar per day for the last 7 days, fed by cumulative counters
 * (request_success_total / generation_tokens_total) snapshotted on every dashboard
 * refresh. A counter drop = server restart -> that day restarts its delta. Stored locally
 * (SharedPreferences, one line per day) so the phone works fully offline; the app is the
 * only writer.
 */
public class HistoryActivity extends Activity {

    static final String PREFS = "history";
    static final String KEY = "days";          // "yyyy-MM-dd=req,tokens\n..." newest last

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF12141A);
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("App activity · last 7 days");
        title.setTextSize(16f);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(4));

        BarsView reqBars = new BarsView(this, "requests", false);
        BarsView tokBars = new BarsView(this, "tokens", true);
        TextView note = new TextView(this);
        note.setTextSize(11f);
        note.setTextColor(0xFF6E7681);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(8), 0, dp(4));

        List<Day> days = Days.load(this);
        reqBars.setData(days);
        tokBars.setData(days);
        note.setText(days.isEmpty()
                ? "no data yet — open Pocket Brain to record a visit"
                : days.size() + " day(s) recorded on this phone · one unit per app visit");

        root.addView(title);
        root.addView(reqBars, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(tokBars, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(note);
        setContentView(root);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ---------- storage ----------
    static final class Day {
        final String date; long requests, tokens;
        Day(String date, long r, long t) { this.date = date; this.requests = r; this.tokens = t; }
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Add deltas for today (or overwrite if the same day is revisited later that day
     *  with larger totals — applyLive passes true deltas, so we simply accumulate). */
    static void accumulate(Activity a, long reqDelta, long tokDelta) {
        List<Day> days = Days.load(a);
        String td = today();
        Day last = days.isEmpty() ? null : days.get(days.size() - 1);
        if (last == null || !last.date.equals(td)) { last = new Day(td, 0, 0); days.add(last); }
        last.requests += reqDelta;
        last.tokens += tokDelta;
        while (days.size() > 14) days.remove(0);
        StringBuilder sb = new StringBuilder();
        for (Day d : days) sb.append(d.date).append('=').append(d.requests).append(',').append(d.tokens).append('\n');
        a.getSharedPreferences(PREFS, 0).edit().putString(KEY, sb.toString()).apply();
    }

    static class Days {
        static List<Day> load(Activity a) {
            List<Day> out = new ArrayList<>();
            String raw = a.getSharedPreferences(PREFS, 0).getString(KEY, "");
            for (String line : raw.split("\n")) {
                String[] kv = line.split("=");
                if (kv.length != 2) continue;
                String[] vt = kv[1].split(",");
                if (vt.length != 2) continue;
                try { out.add(new Day(kv[0], Long.parseLong(vt[0]), Long.parseLong(vt[1]))); }
                catch (NumberFormatException ignored) {}
            }
            return out;
        }
    }

    // ---------- bars ----------
    static final class BarsView extends android.view.View {
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String label;
        private final boolean human;
        private List<Day> days = new ArrayList<>();

        BarsView(android.content.Context c, String label, boolean human) {
            super(c);
            this.label = label; this.human = human;
            txt.setTextSize(9 * c.getResources().getDisplayMetrics().density);
            txt.setColor(0xFF9DA7B3);
        }
        void setData(List<Day> d) {
            int n = d.size();
            days = new ArrayList<>(d.subList(Math.max(0, n - 7), n));
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
            if (days.isEmpty()) return;
            long max = 1;
            for (Day x : days) max = Math.max(max, human ? x.tokens : x.requests);
            float pad = 16 * d, top = 18 * d;
            float bw = (w - pad * 2) / days.size();
            float title = 11 * d;
            txt.setTextSize(title); txt.setColor(0xFF6E7681);
            canvas.drawText(label.toUpperCase(Locale.US), pad, title, txt);
            for (int i = 0; i < days.size(); i++) {
                Day x = days.get(i);
                long v = human ? x.tokens : x.requests;
                float bh = (h - top - 16 * d) * (v / (float) max);
                float x0 = pad + i * bw + bw * 0.18f, x1 = pad + (i + 1) * bw - bw * 0.18f;
                bar.setColor(0xFF58A6FF);
                canvas.drawRoundRect(x0, h - 14 * d - bh, x1, h - 14 * d, 4 * d, 4 * d, bar);
                canvas.drawText(x.date.substring(5), x0, h - 3 * d, txt);   // MM-dd
                if (v > 0) {
                    String s = human ? human(v) : String.valueOf(v);
                    canvas.drawText(s, x0, h - 16 * d - bh, txt);
                }
            }
        }
        private static String human(long v) {
            if (v >= 1_000_000) return String.format(Locale.US, "%.1fM", v / 1e6);
            if (v >= 1_000) return String.format(Locale.US, "%.0fK", v / 1e3);
            return String.valueOf(v);
        }
    }
}
