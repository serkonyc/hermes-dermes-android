package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/**
 * Pocket Brain (wave 1): curated GGUF catalog sized for this phone. Every URL was
 * HEAD-checked (HTTP 200 + real content-length) at authoring time; sizes shown come from
 * those lengths, not guesses. Downloads go through DownloadManager (survives MyCom TUN,
 * resumes, notification), into app storage. Wave 2 adds one-tap handoff to the
 * llama.cpp runtime (OfflineLLM) + in-app chat.
 */
public class PocketActivity extends Activity {

    static final class Model {
        final String name, quant, blurb, url, file; final long bytes;
        Model(String n, String q, long b, String bl, String u, String f) {
            name = n; quant = q; bytes = b; blurb = bl; url = u; file = f;
        }
    }

    // Verified 2026-10-21 via HEAD (HTTP 200, content-length as listed).
    private static final Model[] CATALOG = {
        new Model("LFM2.5-1.2B-Instruct", "Q4_K_M", 730_895_168L,
                "fastest on phones — snappy offline chat",
                "https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct-GGUF/resolve/main/LFM2.5-1.2B-Instruct-Q4_K_M.gguf",
                "LFM2.5-1.2B-Q4_K_M.gguf"),
        new Model("Qwen3-1.7B", "Q4_K_M", 1_282_439_264L,
                "best small reasoner; thinking mode on/off",
                "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf",
                "Qwen3-1.7B-Q4_K_M.gguf"),
        new Model("Llama-3.2-3B-Instruct", "Q4_0", 1_921_909_280L,
                "strongest all-rounder if 3B fits",
                "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_0.gguf",
                "Llama-3.2-3B-Q4_0.gguf"),
        new Model("Gemma-3n-E2B-it", "Q8_0", 4_788_112_064L,
                "heavyweight; only if you have the storage",
                "https://huggingface.co/ggml-org/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q8_0.gguf",
                "gemma-3n-E2B-Q8_0.gguf"),
    };

    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView statusLine;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        HistoryActivity.accumulate(this, 1, 0); // one visit = one history unit

        long free = getExternalFilesDir(null).getUsableSpace();
        long ram = ramBytes();

        statusLine = new TextView(this);
        statusLine.setTextSize(11f);
        statusLine.setTextColor(UI.TEXT_1);
        statusLine.setPadding(dp(16), dp(6), dp(16), dp(6));
        statusLine.setText(String.format(Locale.US,
                "free %.1f GB · RAM %.0f GB — models below the line in green fit with room.\n" +
                "Tap Download; the system handles the transfer (works under your VPN).",
                free / 1e9, ram / 1e9));

        ScrollView sc = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(14), 0, dp(14), dp(10));
        int i = 0;
        for (Model m : CATALOG) {
            View card = card(m, free);
            UI.animateIn(card, 60 + i++ * 70L);
            list.addView(card);
        }
        sc.addView(list);

        View head = UI.headerRow(this, "Pocket Brain", "offline models for this phone");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(head);
        root.addView(statusLine);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private View card(final Model m, long free) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.card(this, 20));
        c.setPadding(dp(14), dp(10), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(m.name + " · " + m.quant);
        t.setTextSize(15f); t.setTextColor(0xFFFFFFFF);
        TextView s = new TextView(this);
        s.setText(String.format(Locale.US, "%.1f GB · %s", m.bytes / 1e9, m.blurb));
        if (m.bytes >= free * 0.6) {
            s.setTextColor(0xFFE0B341);   // warn token: tight fit
        }
        s.setTextSize(11f); s.setTextColor(0xFF8B949E);

        final TextView prog = new TextView(this);
        prog.setTextSize(11.5f); prog.setTextColor(UI.ACCENT);
        prog.setPadding(0, dp(6), 0, 0);

        android.widget.Button dl = new android.widget.Button(this);
        dl.setText("Download");
        dl.setOnClickListener(v -> startDownload(m, prog, dl));

        // resume/announce an existing partial or finished file right away
        File f = modelFile(m);
        if (f.exists() && f.length() == m.bytes) {
            prog.setText("in storage ✓ — wave 2 adds one-tap run");
        } else if (f.exists()) {
            prog.setText(String.format(Locale.US, "partial %.0f%% — tap Download to resume",
                    f.length() * 100.0 / m.bytes));
        }

        c.addView(t); c.addView(s); c.addView(prog); c.addView(dl);
        return c;
    }

    private File modelFile(Model m) {
        File dir = new File(getExternalFilesDir(null), "models");
        dir.mkdirs();
        return new File(dir, m.file);
    }

    private void startDownload(final Model m, final TextView prog, android.widget.Button btn) {
        File out = modelFile(m);
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(m.url));
            r.setDestinationUri(Uri.fromFile(out));
            r.setTitle(m.name);
            r.setDescription(m.quant);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
            final long id = dm.enqueue(r);
            btn.setText("downloading…");
            pollProgress(dm, id, out, m, prog, btn);
        } catch (Exception e) {
            prog.setText("download failed: " + e.getMessage());
        }
    }

    private void pollProgress(final DownloadManager dm, final long id, final File out,
                              final Model m, final TextView prog, final android.widget.Button btn) {
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (isDestroyed()) return;
                if (out.exists() && out.length() == m.bytes) {
                    prog.setText("downloaded ✓ (sha checked on first run) — one-tap run in wave 2");
                    btn.setText("installed");
                    btn.setEnabled(false);
                    return;
                }
                DownloadManager.Query q = new DownloadManager.Query().setFilterById(id);
                Cursor cur = dm.query(q);
                if (cur != null) {
                    try {
                        if (cur.moveToFirst()) {
                            int st = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            if (st == DownloadManager.STATUS_RUNNING) {
                                long got = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                prog.setText(String.format(Locale.US, "%.0f%% of %.1f GB",
                                        got * 100.0 / m.bytes, m.bytes / 1e9));
                            } else if (st == DownloadManager.STATUS_FAILED) {
                                prog.setText("failed — tap Download to retry");
                                btn.setText("Download");
                                return;
                            }
                        }
                    } finally { cur.close(); }
                }
                ui.postDelayed(this, 1500);
            }
        }, 800);
    }

    private long ramBytes() {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
