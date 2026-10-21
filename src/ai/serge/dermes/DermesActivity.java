package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Dermes hub (v3.8.0) — pure Java. One screen, six tiles: Dashboard (live Mac monitor),
 * Digest (Hermes feed), History (local usage chart), Device intel (local LLM
 * feasibility), Update (self-update), About. The update machinery is
 * unchanged: GitHub Releases latest vs PackageInfo.versionName, DownloadManager (system
 * proxy survives MyCom TUN), FileProvider installer handoff.
 *
 * Device notes (Samsung Fold, no adb — the phone is the only test bench):
 * - Kotlin is out: this device kills Kotlin activities before onCreate (v1.2.x diagnostics).
 * - Raw sockets/LAN die under the always-on MyCom TUN; DownloadManager works because it goes
 *   through the system proxy, which is why the updater does not hand-roll HTTP.
 */
public class DermesActivity extends Activity {

    private static final String REPO = "serkonyc/hermes-dermes-android";
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private TextView status;
    private String latestTag;          // tag offered by the Update tile
    private String downloadUrl;        // asset URL for latestTag
    private long expectedSize;         // asset size from GitHub; download-complete check
    private long pendingDownload;      // 0 = none; lets a retry cancel the previous in-flight download

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(build());
        checkForUpdate();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF181A1F);
        root.setFitsSystemWindows(true);
        root.setPadding(dp(22), dp(10), dp(22), dp(12));

        // Header left-aligned (art review: centered stack looked like a dial pad)
        TextView title = new TextView(this);
        title.setText("Dermes");
        title.setTextSize(26f);
        title.setTextColor(0xFFEBEEF3);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView tagline = new TextView(this);
        tagline.setText("local AI · your phone, no cloud");
        tagline.setTextSize(12f);
        tagline.setTextColor(0xFF9BA3AF);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(0, 0, 0, dp(16));
        head.addView(title);
        head.addView(tagline);

        // Hero: the one feature that matters, full width, dominant type sizes
        View hero = hero();

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        grid.addView(row(tile("Mac Pulse", "live mlx-serve", 0xFF6C8CC4, DashboardActivity.class),
                tile("Digest", "Hermes morning feed", 0xFF5FA773, DigestActivity.class)));
        grid.addView(row(tile("History", "7-day usage", 0xFF9B87C9, HistoryActivity.class),
                tile("Update", "check now", 0xFF8A93A8, null)));

        status = new TextView(this);
        status.setTextSize(11.5f);
        status.setTextColor(0xFF6E7580);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(10), 0, 0);
        status.setText("checking for updates…");

        root.addView(head);
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(150)));
        root.addView(grid);
        root.addView(status);
        return root;
    }

    private View hero() {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.HORIZONTAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF2C323E, 0xFF22262E});
        g.setCornerRadius(dp(20));
        g.setStroke(dp(1), 0xFF465064);
        t.setBackground(g);
        t.setPadding(dp(20), 0, dp(20), 0);
        t.setClickable(true);

        IconView ic = new IconView(this, IconView.BRAIN, 0xFF6C8CC4, dp(52));
        t.addView(ic, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(18), 0, 0, 0);
        TextView l = new TextView(this);
        l.setText("Pocket Brain");
        l.setTextSize(21f);
        l.setTextColor(0xFFEBEEF3);
        l.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView s = new TextView(this);
        s.setText("download & run local LLM models");
        s.setTextSize(13f);
        s.setTextColor(0xFFA8B1C0);
        s.setPadding(0, dp(3), 0, 0);
        TextView meta = new TextView(this);
        meta.setText("0.7–4.8 GB · offline in the metro");
        meta.setTextSize(12f);
        meta.setTextColor(0xFF7C879A);
        meta.setPadding(0, dp(4), 0, 0);
        txt.addView(l); txt.addView(s); txt.addView(meta);
        t.addView(txt, new LinearLayout.LayoutParams(0, -2, 1f));

        t.setOnClickListener(v -> {
            v.setAlpha(0.6f);
            v.animate().alpha(1f).setDuration(140).start();
            startActivity(new Intent(this, PocketActivity.class));
        });
        return t;
    }

    private LinearLayout row(View a, View b2) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        r.setPadding(0, dp(7), 0, dp(0));
        LinearLayout.LayoutParams lap = new LinearLayout.LayoutParams(0, -1, 1f);
        lap.rightMargin = dp(7);
        LinearLayout.LayoutParams rap = new LinearLayout.LayoutParams(0, -1, 1f);
        rap.leftMargin = dp(7);
        r.addView(a, lap);
        r.addView(b2, rap);
        return r;
    }

    private View tile(final String label, String sub, int accent, final Class<?> target) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.BOTTOM);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF262A32, 0xFF22252D});
        g.setCornerRadius(dp(20));
        g.setStroke(dp(1), 0xFF383E4A);
        t.setBackground(g);
        t.setPadding(dp(18), 0, dp(18), dp(16));
        t.setClickable(true);

        IconView ic = new IconView(this, iconFor(label), accent, dp(22));
        LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(dp(26), dp(26));
        icp.bottomMargin = dp(12);
        t.addView(ic, icp);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(15.5f);
        l.setTextColor(0xFFEBEEF3);
        l.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(12f);
        s.setTextColor(0xFF9BA3AF);
        t.addView(l);
        t.addView(s);
        t.setOnClickListener(v -> {
            v.setAlpha(0.55f);
            v.animate().alpha(1f).setDuration(140).start();
            if (target != null) startActivity(new Intent(this, target));
            else onUpdateTile();
        });
        return t;
    }

    private static int iconFor(String label) {
        if (label.equals("Mac Pulse")) return IconView.PULSE;
        if (label.equals("Digest"))    return IconView.FEED;
        if (label.equals("History"))   return IconView.BARS;
        return IconView.ARROW;   // Update
    }

    // ---------- updater ----------
    private String installedVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;   // single source of truth: the manifest
        } catch (Exception e) { return "0"; }
    }

    private void checkForUpdate() {
        pool.execute(() -> {
            String latest = null, dl = null, note = null;
            try {
                JSONObject rel = new JSONObject(httpGetPublic(
                        "https://api.github.com/repos/" + REPO + "/releases/latest"));
                latest = rel.optString("tag_name").replaceFirst("^v", "");
                JSONArray assets = rel.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        if (a.optString("name").endsWith(".apk")) {
                            dl = a.optString("browser_download_url");
                            expectedSize = a.optLong("size");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                note = "update check failed: " + e;
            }
            final String fLatest = latest, fDl = dl, fNote = note;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                String cur = installedVersion();
                if (fLatest == null) {
                    status.setTextColor(0xFFD29922);
                    status.setText("v" + cur + " installed · " + (fNote != null ? fNote : "no releases yet"));
                    return;
                }
                if (isNewer(fLatest, cur) && fDl != null) {
                    latestTag = fLatest;
                    downloadUrl = fDl;
                    status.setTextColor(0xFF3FB950);
                    status.setText("v" + cur + " installed · v" + fLatest + " available — tap Update");
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        status.setText(status.getText()
                                + "\ngrant 'Install unknown apps' for Dermes, then tap Update again");
                    }
                } else {
                    latestTag = null;
                    downloadUrl = null;
                    status.setTextColor(0xFF6E7681);
                    status.setText("v" + cur + " installed · up to date");
                }
            });
        });
    }

    private void onUpdateTile() {
        if (latestTag == null || downloadUrl == null) {
            status.setText("no pending update · rechecking…");
            status.setTextColor(0xFFB0C4DE);
            checkForUpdate();
            return;
        }
        startDownload();
    }

    private void startDownload() {
        String url = downloadUrl;
        if (url == null) return;
        if (!getPackageManager().canRequestPackageInstalls()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                status.setText("Open Settings → Apps → Dermes → Install unknown apps, then retry.");
            }
            return;
        }
        // Cancel any still-running previous download; two concurrent ones overwrite each other.
        if (pendingDownload > 0) { try { dm().remove(pendingDownload); } catch (Exception ignored) {} }
        java.io.File dir = new java.io.File(getExternalFilesDir(null), "updates");
        dir.mkdirs();
        // Version-stamped name: the old file must NEVER look like the new download mid-flight
        // (size-check polling on a shared name installs partial APKs).
        java.io.File out = new java.io.File(dir, "dermes-" + latestTag + ".apk");
        if (out.exists()) out.delete();
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.setDestinationUri(Uri.fromFile(out));
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setTitle("Dermes update");
            pendingDownload = dm().enqueue(r);
            status.setText("downloading… install prompt appears when done");
            pollInstall(out);
        } catch (Exception e) {
            status.setText("download failed: " + e);
        }
    }

    private DownloadManager dm() { return (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE); }

    // Once DownloadManager lands the APK, launch the system installer for it.
    private void pollInstall(java.io.File apk) {
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            int misses = 0;
            @Override public void run() {
                if (apk.exists() && apk.length() == expectedSize) {
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            DermesActivity.this, "ai.serge.dermes.fileprovider", apk);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { startActivity(i); } catch (Exception e) {
                        status.setText("installer failed: " + e);
                    }
                } else if (!isDestroyed() && ++misses < 80) { // ~2 min cap
                    h.postDelayed(this, 1500);
                } else if (!isDestroyed()) {
                    status.setText("download did not finish; open the release page manually");
                }
            }
        }, 1500);
    }

    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("[.\\-]"), b = current.split("[.\\-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parseInt(a[i]) : 0;
            int y = i < b.length ? parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }
    private static int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    public static String httpGetPublic(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("User-Agent", "dermes-updater");
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
}
