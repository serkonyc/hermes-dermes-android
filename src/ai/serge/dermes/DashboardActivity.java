package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hermes LLM Monitor — pure Java (no Kotlin, no stdlib): this device kills Kotlin
 * activities before onCreate (proven by v1.2.x diagnostics), so the app avoids Kotlin entirely.
 * Demo-first: works offline from canned data; live mlx-serve metrics overlay when reachable.
 */
public class DashboardActivity extends Activity {

    private static final String ENDPOINT_LOOPBACK = "http://127.0.0.1:18422";
    private static final String ENDPOINT_LAN = "http://192.168.1.96:18422";
    private static final long REFRESH_SECONDS = 10L;
    private static final int MIN_INNER_WIDTH_DP = 560;

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView titleView, statusView;
    private Button refreshButton;
    private DashboardView dash;
    private TpsChartView tpsChart;

    private String lastLiveText = null;
    private String liveBase = null;
    private Double prevGenTokens = null;
    private Double prevRequests = null;
    private Long prevTimeMs = null;
    private int demoIndex = 0;
    private Double lastGenTokens;   // for history deltas
    private Double lastGenSnapshot;

    private final Runnable refreshTick = new Runnable() {
        @Override public void run() {
            doRefresh();
            mainHandler.postDelayed(this, REFRESH_SECONDS * 1000L);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Header: title left, status below-left, Refresh right — one compact row instead
        // of three stacked full-width rows (art review: too much chrome, too little data).
        titleView = new TextView(this);
        titleView.setTextSize(18f);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setText("Dermes");

        // One short status line instead of the old debug-log wall (art review v3.1.1:
        // raw monospace text = "looks like debug output, not UI").
        statusView = new TextView(this);
        statusView.setTextSize(11f);
        statusView.setTextColor(0xFF8B949E);
        statusView.setSingleLine(true);

        refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setOnClickListener(v -> doRefresh());

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(10), dp(12), dp(4));
        LinearLayout.LayoutParams txtcol = new LinearLayout.LayoutParams(0, -2, 1f);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.addView(titleView);
        inner.addView(statusView);
        head.addView(inner, txtcol);
        head.addView(refreshButton, new LinearLayout.LayoutParams(-2, -2));

        dash = new DashboardView(this);
        tpsChart = new TpsChartView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF181A1F);
        root.setFitsSystemWindows(true);   // keep clear of notch/status bar (review note)
        root.addView(head);
        root.addView(tpsChart, new LinearLayout.LayoutParams(-1, dp(130)));
        root.addView(dash, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        dash.resume();
        doRefresh();
        mainHandler.removeCallbacks(refreshTick);
        mainHandler.postDelayed(refreshTick, REFRESH_SECONDS * 1000L);
    }

    @Override protected void onPause() {
        mainHandler.removeCallbacks(refreshTick);
        dash.pause();
        super.onPause();
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void doRefresh() {
        refreshButton.setEnabled(false);
        pool.execute(() -> {
            LiveSnapshot snap = fetchLive();
            final String statusLine = snap != null ? snap.statusLine : demoStatusLine();
            if (snap != null) lastLiveText = snap.text;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                statusView.setText(statusLine);
                refreshButton.setEnabled(true);
                if (snap != null) applyLive(snap); else advanceDemo();
            });
        });
    }

    private static final class LiveSnapshot {
        final String text, base, statusLine;
        final DashState state;            // visual state (may be null if metrics unavailable)
        final Double genTokens, requests; // raw cumulative for sparkline/ripple deltas
        final int running, waiting;
        final List<StarModel> models;
        LiveSnapshot(String text, String base, String statusLine, DashState state,
                     Double genTokens, Double requests,
                     int running, int waiting, List<StarModel> models) {
            this.text = text; this.base = base; this.statusLine = statusLine; this.state = state;
            this.genTokens = genTokens; this.requests = requests;
            this.running = running; this.waiting = waiting; this.models = models;
        }
    }

    private static Double s_gen(Map<String, String> m) { return dbl(m.get("vllm:generation_tokens_total")); }
    private static Double s_req(Map<String, String> m) { return dbl(m.get("vllm:request_success_total")); }

    /** Cards + load from the metrics map. Deltas (tok/s, cache rate) computed vs previous
     *  refresh on the UI thread in applyLive; absolute values are fine from one snapshot. */
    private DashState buildState(Map<String, String> m, List<StarModel> models, long latencyMs) {
        DashState s = new DashState();
        s.online = true;
        s.running = intr(m.get("vllm:num_requests_running"));
        s.waiting = intr(m.get("vllm:num_requests_waiting"));
        s.models = DashState.modelsFrom(models);
        int acc = 0xFF58A6FF, green = 0xFF3FB950, amber = 0xFFD29922, purple = 0xFFBC8CFF;
        s.cards.add(new DashState.Card("requests", num(m.get("vllm:request_success_total")), acc));
        s.cards.add(new DashState.Card("tokens out", fmt(dbl(m.get("vllm:generation_tokens_total")).longValue()), purple));
        String gpu = m.get("mlx_serve:gpu_utilization_pct");
        s.cards.add(new DashState.Card("gpu", gpu == null ? "—" : num(gpu) + "%",
                gpu != null && dbl(gpu) > 80 ? amber : green));
        String mem = m.get("mlx_serve:memory_mb");
        s.cards.add(new DashState.Card("memory", mem == null ? "—" : fmt(dbl(mem).longValue()) + " MB", green));
        String hits = m.get("vllm:prefix_cache_hits_total"), q = m.get("vllm:prefix_cache_queries_total");
        String cache = "—";
        if (dbl(hits) != null && dbl(q) != null && dbl(q) > 0) {
            cache = String.format(Locale.US, "%.1f%%", 100 * dbl(hits) / dbl(q));
        }
        s.cards.add(new DashState.Card("cache hit", cache, purple));
        String up = m.get("mlx_serve:process_start_time_seconds");
        s.cards.add(new DashState.Card("uptime", up == null ? "—" : fmtUptime(dbl(up)), acc));
        s.cards.add(new DashState.Card("latency", latencyMs + " ms", latencyMs < 50 ? green : amber));
        return s;
    }

    private static String fmtUptime(double startEpochSec) {
        long secs = System.currentTimeMillis() / 1000 - (long) startEpochSec;
        long hh = secs / 3600, mm = (secs % 3600) / 60;
        return hh > 0 ? hh + "h " + mm + "m" : mm + "m";
    }

    private LiveSnapshot fetchLive() {
        long t0 = System.currentTimeMillis();
        String body = null, base = ENDPOINT_LOOPBACK;
        for (String ep : new String[]{ENDPOINT_LOOPBACK, ENDPOINT_LAN}) {
            try { body = httpGet(ep + "/v1/models"); base = ep; break; } catch (Exception ignored) {}
        }
        if (body == null) return null;
        try {
            long latency = System.currentTimeMillis() - t0;
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ONLINE  (").append(latency).append(" ms)\n");
            sb.append("Endpoint: ").append(base).append("\n\n");
            JSONArray arr = new JSONObject(body).optJSONArray("data");
            if (arr == null) arr = new JSONArray();
            List<StarModel> models = new ArrayList<>();
            sb.append("Models: ").append(arr.length()).append("\n");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject m = arr.getJSONObject(i);
                String id = m.optString("id");
                String state = m.optString("state", "ready");
                models.add(new StarModel(id, state, m.optDouble("bytes_resident", 0.0)));
                sb.append("  - ").append(id).append("  [").append(state)
                  .append("]  ctx ").append(m.optInt("context_length")).append("\n");
            }
            sb.append("\n");
            try {
                Map<String, String> metrics = parseMetrics(httpGet(base + "/metrics"));
                DashState ds = buildState(metrics, models, latency);
                sb.append("Live metrics:\n");
                for (DashState.Card cd : ds.cards) sb.append("  ").append(cd.label).append(": ").append(cd.value).append("\n");
                sb.append("  running now: ").append(ds.running).append("  waiting: ").append(ds.waiting).append("\n");
                return new LiveSnapshot(sb.toString(), base,
                        "ONLINE · " + latency + " ms · " + base.replaceFirst("https?://", "")
                                + " · " + DateFormat.getTimeInstance().format(System.currentTimeMillis()),
                        ds, s_gen(metrics), s_req(metrics),
                        ds.running, ds.waiting, models);
            } catch (Exception e) {
                sb.append("(metrics endpoint unavailable)\n");
                return new LiveSnapshot(sb.toString(), base,
                        "ONLINE · " + latency + " ms · metrics unavailable",
                        null, null, null, 0, 0, models);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void applyLive(LiveSnapshot s) {
        if (!s.base.equals(liveBase)) { liveBase = s.base; prevGenTokens = null; prevRequests = null; }
        double tps = 0;
        if (s.genTokens != null) {
            long now = System.currentTimeMillis();
            if (prevGenTokens != null && prevTimeMs != null && now > prevTimeMs) {
                tps = (s.genTokens - prevGenTokens) * 1000.0 / (now - prevTimeMs);
            }
            prevGenTokens = s.genTokens;
            prevTimeMs = now;
        }
        int reqDelta = 0;
        long tokDelta = 0;
        if (s.requests != null && prevRequests != null) reqDelta = (int) Math.max(0, s.requests - prevRequests);
        if (s.requests != null) prevRequests = s.requests;
        if (s.genTokens != null && lastGenTokens != null) tokDelta = (long) Math.max(0, s.genTokens - lastGenTokens);
        if (s.genTokens != null) {
            if (lastGenSnapshot != null && lastGenSnapshot != s.genTokens && (reqDelta > 0 || tokDelta > 0) )
                HistoryActivity.accumulate(this, reqDelta, tokDelta);
            lastGenSnapshot = s.genTokens;
        }
        lastGenTokens = s.genTokens;

        DashState st = s.state;
        if (st == null) { st = new DashState(); st.models = DashState.modelsFrom(s.models);
            st.running = s.running; st.waiting = s.waiting; }
        st.tps = Math.max(0, tps);
        st.requestDelta = reqDelta;
        if (tps > 0) tpsChart.addSample(tps);
        dash.update(st);
    }

    private void advanceDemo() {
        liveBase = null; prevGenTokens = null; prevRequests = null;
        int idx = demoIndex % Demo.RUNNING.length;
        DashState st = new DashState();
        st.online = false;
        st.running = Demo.RUNNING[idx]; st.waiting = Demo.WAITING[idx];
        st.tps = Demo.TPS[idx]; st.requestDelta = Demo.RIPPLES[idx];
        tpsChart.addSample(Demo.TPS[idx]);
        st.models = DashState.modelsFrom(Demo.STARS);
        int acc = 0xFF58A6FF, green = 0xFF3FB950, amber = 0xFFD29922, purple = 0xFFBC8CFF;
        st.cards.add(new DashState.Card("requests", "499", acc));
        st.cards.add(new DashState.Card("tokens out", "122 643", purple));
        st.cards.add(new DashState.Card("gpu", "88%", amber));
        st.cards.add(new DashState.Card("memory", "75 644 MB", green));
        st.cards.add(new DashState.Card("cache hit", "58.8%", purple));
        st.cards.add(new DashState.Card("uptime", "4h 52m", acc));
        st.cards.add(new DashState.Card("latency", "tunnel", green));
        dash.update(st);
        demoIndex++;
    }

    private String demoStatusLine() {
        StringBuilder sb = new StringBuilder("OFFLINE DEMO · tap Refresh to retry");
        sb.append(" · ").append(DateFormat.getTimeInstance().format(System.currentTimeMillis()));
        return sb.toString();
    }

    private static Map<String, String> parseMetrics(String body) {
        Map<String, String> out = new HashMap<>();
        for (String line : body.split("\n")) {
            if (line.startsWith("#") || !line.contains(" ")) continue;
            String key = line.substring(0, line.indexOf(' '));
            if (!out.containsKey(key)) out.put(key, line.substring(line.indexOf(' ') + 1).trim());
        }
        return out;
    }

    private static Double dbl(String s) { try { return s == null ? null : Double.parseDouble(s); } catch (Exception e) { return null; } }
    private static int intr(String s) { Double d = dbl(s); return d == null ? 0 : d.intValue(); }
    private static String num(String s) { Double d = dbl(s); return d == null ? s : fmt(d.longValue()); }
    private static String fmt(long n) { return String.format(Locale.US, "%,d", n).replace(',', ' '); }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(1200);
        conn.setReadTimeout(1500);
        try {
            if (conn.getResponseCode() != 200) throw new Exception("http " + conn.getResponseCode());
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            r.close();
            return sb.toString();
        } finally { conn.disconnect(); }
    }

    // ---------- data models ----------

    /** Raw model entry from /v1/models; converted to DashState.Model for drawing. */
    static final class StarModel {
        final String id, state; final double bytesResident;
        StarModel(String id, String state, double bytesResident) {
            this.id = id; this.state = state; this.bytesResident = bytesResident;
        }
    }

    static final class Demo {
        static final double[] TPS = {21.4, 24.8, 23.1, 31.6, 28.9, 26.4, 33.2, 29.7, 27.5, 35.1};
        static final int[] RUNNING = {1, 2, 1, 3, 2, 1, 2, 1, 3, 1};
        static final int[] WAITING = {0, 1, 0, 2, 0, 0, 1, 0, 1, 0};
        static final int[] RIPPLES = {1, 3, 2, 5, 4, 1, 2, 1, 4, 2};
        static final List<StarModel> STARS = new ArrayList<>();
        static { STARS.add(new StarModel("Qwen3.8-Flash", "ready", 75_644_000_000d)); }
    }

}
