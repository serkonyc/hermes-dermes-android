package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
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

    // Verified 2026-10-22 via HEAD (HTTP 200 + content-length).
    // Single model chosen by benchmarks (MMLU/GPQA/IFEval winner vs 4B dense; 1.5B active
    // params, 2.4x faster on phones). The whole screen revolves around this one file.
    private static final Model MODEL =
        new Model("LFM2.5-8B-A1B", "Q4_K_M", 5_155_564_768L,
                "the on-device brain — downloads once, then chats offline",
                "https://huggingface.co/LiquidAI/LFM2.5-8B-A1B-GGUF/resolve/main/LFM2.5-8B-A1B-Q4_K_M.gguf",
                "LFM2.5-8B-A1B-Q4_K_M.gguf");

    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView prog;
    private android.widget.Button actionBtn, dlBtn;
    private boolean autoStarted;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        HistoryActivity.accumulate(this, 1, 0); // one visit = one history unit

        prog = new TextView(this);
        prog.setTextSize(11.5f); prog.setTextColor(UI.ACCENT);
        prog.setPadding(dp(16), dp(6), dp(16), dp(6));

        actionBtn = new android.widget.Button(this);
        dlBtn = new android.widget.Button(this);
        dlBtn.setText("Re-download");
        dlBtn.setVisibility(View.GONE);
        dlBtn.setOnClickListener(v -> startDownload(dlBtn));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), 0, dp(16), dp(12));
        root.addView(UI.headerRow(this, "Neuro Chat", "one model · on-device · nothing leaves the phone"));
        root.addView(prog);
        root.addView(actionBtn);
        root.addView(dlBtn);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshState();
        // auto-start the one-time download on first visit (only if nothing runs yet)
        if (!autoStarted) {
            autoStarted = true;
            File f = modelFile();
            if (!f.exists() && !downloadActive() &&
                    getExternalFilesDir(null).getUsableSpace() > MODEL.bytes + 2_000_000_000L) {
                startDownload(actionBtn);
            }
        }
    }

    /** Single source of truth for the screen: not-ready -> Download; ready -> Chat. */
    private void refreshState() {
        File f = modelFile();
        if (f.exists() && f.length() == MODEL.bytes) {
            actionBtn.setText("Chat");
            actionBtn.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
            prog.setText("model in storage ✓ — offline chat, unloads from RAM when idle");
            dlBtn.setVisibility(View.VISIBLE);
        } else {
            actionBtn.setText("Download model");
            actionBtn.setOnClickListener(v -> startDownload(actionBtn));
            if (f.exists()) {
                prog.setText(String.format(Locale.US, "paused at %.0f%% — tap to resume",
                        f.length() * 100.0 / MODEL.bytes));
            } else {
                prog.setText(String.format(Locale.US,
                        "one-time download, %.1f GB. Works under your VPN, resumes if interrupted; "
                        + "afterwards the chat is fully offline.", MODEL.bytes / 1e9));
            }
            dlBtn.setVisibility(View.GONE);
        }
    }

    private boolean downloadActive() {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query q = new DownloadManager.Query()
                .setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING);
        Cursor cur = dm.query(q);
        try { return cur != null && cur.moveToFirst()
                && cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)).contains("LFM2.5"); }
        finally { if (cur != null) cur.close(); }
    }

    private File modelFile() {
        File dir = new File(getExternalFilesDir(null), "models");
        dir.mkdirs();
        return new File(dir, MODEL.file);
    }

    private void startDownload(android.widget.Button btn) {
        File out = modelFile();
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(MODEL.url));
            r.setDestinationUri(Uri.fromFile(out));
            r.setTitle(MODEL.name);
            r.setDescription(MODEL.quant);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
            final long id = dm.enqueue(r);
            btn.setText("downloading…");
            btn.setEnabled(false);
            pollProgress(dm, id, out, btn);
        } catch (Exception e) {
            prog.setText("download failed: " + e.getMessage());
        }
    }

    private void pollProgress(final DownloadManager dm, final long id, final File out,
                              final android.widget.Button btn) {
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (isDestroyed()) return;
                if (out.exists() && out.length() == MODEL.bytes) {
                    btn.setEnabled(true);
                    refreshState();
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
                                        got * 100.0 / MODEL.bytes, MODEL.bytes / 1e9));
                            } else if (st == DownloadManager.STATUS_FAILED) {
                                prog.setText("failed — tap to retry");
                                btn.setText("Download model");
                                btn.setEnabled(true);
                                return;
                            }
                        }
                    } finally { cur.close(); }
                }
                ui.postDelayed(this, 1500);
            }
        }, 800);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}