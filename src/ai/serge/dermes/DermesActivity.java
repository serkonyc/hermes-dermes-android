package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
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
 * Dermes update launcher — pure Java. Checks GitHub Releases for the latest tag; when it is
 * newer than the installed version, it shows the "Update vX" button, downloads the APK via
 * DownloadManager (system handles the proxy — important here), and hands the finished file to
 * the package installer. The dashboard is a second button.
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
    private android.widget.Button updateBtn;
    private String latestTag;          // tag shown by the update button, e.g. "3.1.0"
    private long expectedSize;         // asset size from GitHub; download-complete check

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D1117);
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("Dermes");
        title.setTextSize(28f);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setTextSize(14f);
        status.setTextColor(0xFFB0C4DE);
        status.setPadding(48, 24, 48, 24);
        status.setGravity(Gravity.CENTER);
        status.setText("checking for updates…");

        updateBtn = new android.widget.Button(this);
        updateBtn.setVisibility(View.GONE);

        android.widget.Button dash = new android.widget.Button(this);
        dash.setText("Open dashboard");
        dash.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        updateBtn.setOnClickListener(v -> startDownload());

        root.addView(title);
        root.addView(status);
        root.addView(updateBtn);
        root.addView(dash);
        setContentView(root);
        checkForUpdate();
    }

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
                JSONObject rel = new JSONObject(httpGet(
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
                    status.setText("v" + cur + " installed\n" + (fNote != null ? fNote : "no releases yet"));
                    return;
                }
                if (isNewer(fLatest, cur) && fDl != null) {
                    latestTag = fLatest;
                    status.setText("v" + cur + " installed — v" + fLatest + " available");
                    updateBtn.setText("Update to " + fLatest);
                    updateBtn.setTag(fDl);
                    updateBtn.setVisibility(View.VISIBLE);
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        status.setText(status.getText()
                                + "\n\nGrant 'Install unknown apps' for Dermes in the dialog, then tap Update again.");
                    }
                } else {
                    status.setText("v" + cur + " installed — up to date");
                }
            });
        });
    }

    private void startDownload() {
        String url = (String) updateBtn.getTag();
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
    private long pendingDownload;   // 0 = none; lets a retry cancel the previous in-flight download

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

    private static boolean isNewer(String latest, String current) {
        String[] a = latest.split("[.\\-]"), b = current.split("[.\\-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parseInt(a[i]) : 0;
            int y = i < b.length ? parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }
    private static int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private static String httpGet(String url) throws Exception {
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

    private static class View { static final int GONE = android.view.View.GONE;
                                static final int VISIBLE = android.view.View.VISIBLE; }
}
