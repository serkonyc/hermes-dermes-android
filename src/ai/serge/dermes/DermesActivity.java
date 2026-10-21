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
        root.setBackgroundColor(0xFF0D1117);
        root.setFitsSystemWindows(true);
        root.setPadding(dp(14), dp(6), dp(14), dp(10));

        TextView title = new TextView(this);
        title.setText("Dermes");
        title.setTextSize(24f);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);

        TextView ver = new TextView(this);
        ver.setText("v" + installedVersion());
        ver.setTextSize(11f);
        ver.setTextColor(0xFF6E7681);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, 0, 0, dp(6));

        status = new TextView(this);
        status.setTextSize(11f);
        status.setTextColor(0xFFB0C4DE);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(6), 0, 0);
        status.setText("checking for updates…");

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        grid.addView(row(tile("Dashboard", "live Mac pulse", 0xFF1F6FEB, DashboardActivity.class),
                tile("Digest", "Hermes feed", 0xFF238636, DigestActivity.class)));
        grid.addView(row(tile("Pocket Brain", "local LLMs", 0xFF1B7FA8, PocketActivity.class),
                tile("History", "usage on this phone", 0xFF8957E5, HistoryActivity.class)));
        grid.addView(row(tile("Update", "check & install", 0xFF6E40C9, null),
                tile("About", "v" + installedVersion(), 0xFF30363D, null)));

        root.addView(title);
        root.addView(ver);
        root.addView(grid);
        root.addView(status);
        return root;
    }

    private LinearLayout row(View a, View b2) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        r.setPadding(0, dp(5), 0, dp(5));
        LinearLayout.LayoutParams lap = new LinearLayout.LayoutParams(0, -1, 1f);
        lap.rightMargin = dp(5);
        LinearLayout.LayoutParams rap = new LinearLayout.LayoutParams(0, -1, 1f);
        rap.leftMargin = dp(5);
        r.addView(a, lap);
        r.addView(b2, rap);
        return r;
    }

    private View tile(final String label, String sub, int accent, final Class<?> target) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF161B22);
        g.setCornerRadius(dp(16));
        g.setStroke(dp(1), accent);
        t.setBackground(g);
        t.setClickable(true);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16f);
        l.setTextColor(0xFFFFFFFF);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(10f);
        s.setTextColor(0xFF8B949E);
        t.addView(l);
        t.addView(s);
        t.setOnClickListener(v -> {
            v.setAlpha(0.55f);
            v.animate().alpha(1f).setDuration(140).start();
            if (target != null) startActivity(new Intent(this, target));
            else if (label.equals("Update")) onUpdateTile();
            else status.setText("Dermes · Mac LLM monitor · updates via GitHub Releases");
        });
        return t;
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
