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
 * Dermes hub v4.1.0 — composed from the UI design system (token palette, 8dp grid,
 * vector icon chips from res/drawable, staggered entrance, press-scale feedback).
 * Hero = Pocket Brain, five feature tiles, live update line. Update machinery unchanged:
 * GitHub Releases latest vs PackageInfo.versionName, DownloadManager (survives MyCom TUN),
 * FileProvider installer handoff.
 */
public class DermesActivity extends Activity {

    private static final String REPO = "serkonyc/hermes-dermes-android";
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private TextView status;
    private String latestTag, downloadUrl;
    private long expectedSize, pendingDownload;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(build());
        checkForUpdate();
    }

    private static final class Tile {
        final String label, sub; final int icon, accent; final Class<?> target;
        Tile(String l, String s, int i, int a, Class<?> t) { label = l; sub = s; icon = i; accent = a; target = t; }
    }

    private View build() {
        LinearLayout root = UI.screen(this);
        root.addView(UI.headerRow(this, "Dermes", "local AI · your phone, no cloud"));

        View hero = hero();
        UI.animateIn(hero, 40);
        root.addView(hero, new LinearLayout.LayoutParams(-1, UI.dp(this, 128)));

        Tile[] tiles = {
            new Tile("Digest", "Hermes morning feed", R.mipmap.t_feed_icon, UI.ACCENT, DigestActivity.class),
            new Tile("Capsules", "from the future", R.mipmap.t_capsule_icon, UI.VIOLET, CapsuleActivity.class),
            new Tile("Mac Pulse", "live mlx-serve", R.mipmap.t_pulse_icon, UI.TEAL, DashboardActivity.class),
            new Tile("History", "7-day usage", R.mipmap.t_bars_icon, UI.OK, HistoryActivity.class),
            new Tile("Update", "check now", R.mipmap.t_update_icon, UI.TEXT_2, null),
        };
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        for (int t = 0; t < tiles.length; t += 2) {
            View a = tile(tiles[t]);
            View b = t + 1 < tiles.length ? tile(tiles[t + 1]) : new View(this);
            LinearLayout r = row(a, b);
            UI.animateIn(r, 120 + t * 80L);
            grid.addView(r);
        }
        root.addView(grid);

        status = new TextView(this);
        status.setTextSize(11.5f);
        status.setTextColor(UI.TEXT_2);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, UI.dp(this, 12), 0, 0);
        status.setText("checking for updates…");
        root.addView(status);
        return root;
    }

    private View hero() {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.HORIZONTAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setBackground(UI.card(this, 22));
        t.setPadding(UI.dp(this, 18), 0, UI.dp(this, 18), 0);
        t.setClickable(true);

                android.widget.ImageView heroIcon = new android.widget.ImageView(this);
        heroIcon.setImageResource(R.mipmap.t_brain_icon);
        heroIcon.setBackground(UI.card(this, 12));
        t.addView(heroIcon, new LinearLayout.LayoutParams(UI.dp(this, 54), UI.dp(this, 54)));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(UI.dp(this, 16), 0, 0, 0);
        txt.addView(bold("Pocket Brain", 20));
        TextView s = sub("download & run local LLM models");
        s.setTextSize(13f);
        s.setPadding(0, UI.dp(this, 3), 0, 0);
        txt.addView(s);
        TextView meta = sub("0.7–4.8 GB · offline in the metro");
        meta.setTextColor(UI.TEXT_2);
        meta.setPadding(0, UI.dp(this, 3), 0, 0);
        txt.addView(meta);
        t.addView(txt, new LinearLayout.LayoutParams(0, -2, 1f));

        t.setOnClickListener(v -> {
            press(v);
            startActivity(new Intent(this, PocketActivity.class));
        });
        return t;
    }

    private void press(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70)
         .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(130).start())
         .start();
    }

    private TextView bold(String s, float sp) {
        TextView l = new TextView(this);
        l.setText(s); l.setTextSize(sp); l.setTextColor(UI.TEXT_0);
        l.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
        return l;
    }
    private TextView sub(String s) {
        TextView l = new TextView(this);
        l.setText(s); l.setTextSize(11.5f); l.setTextColor(UI.TEXT_1);
        return l;
    }

    private LinearLayout row(View a, View b2) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        r.setPadding(0, UI.dp(this, 6), 0, 0);
        LinearLayout.LayoutParams lap = new LinearLayout.LayoutParams(0, -1, 1f);
        lap.rightMargin = UI.dp(this, 6);
        LinearLayout.LayoutParams rap = new LinearLayout.LayoutParams(0, -1, 1f);
        rap.leftMargin = UI.dp(this, 6);
        r.addView(a, lap);
        r.addView(b2, rap);
        return r;
    }

    private View tile(final Tile spec) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.BOTTOM);
        t.setBackground(UI.card(this, 22));
        t.setPadding(UI.dp(this, 16), 0, UI.dp(this, 16), UI.dp(this, 14));
        t.setClickable(true);

        android.widget.ImageView chip = new android.widget.ImageView(this);
        chip.setImageResource(spec.icon);
        chip.setElevation(UI.dp(this, 2));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(UI.dp(this, 42), UI.dp(this, 42));
        clp.bottomMargin = UI.dp(this, 10);
        t.addView(chip, clp);
        t.addView(bold(spec.label, 15));
        t.addView(sub(spec.sub));
        t.setOnClickListener(v -> {
            press(v);
            if (spec.target != null) startActivity(new Intent(this, spec.target));
            else onUpdateTile();
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
                    status.setText("v" + cur + " installed · " + (fNote != null ? fNote : "no releases yet"));
                    return;
                }
                if (isNewer(fLatest, cur) && fDl != null) {
                    latestTag = fLatest;
                    downloadUrl = fDl;
                    status.setTextColor(UI.OK);
                    status.setText("v" + fLatest + " available · tap Update");
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        status.setText(status.getText()
                                + " · grant 'install unknown apps' first");
                    }
                } else {
                    latestTag = null;
                    downloadUrl = null;
                    status.setTextColor(UI.TEXT_2);
                    status.setText("v" + cur + " · up to date");
                }
            });
        });
    }

    private void onUpdateTile() {
        if (latestTag == null || downloadUrl == null) { checkForUpdate(); return; }
        if (!getPackageManager().canRequestPackageInstalls()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                status.setText("Open Settings → Apps → Dermes → Install unknown apps, then retry.");
            }
            return;
        }
        if (pendingDownload > 0) { try { dm().remove(pendingDownload); } catch (Exception ignored) {} }
        java.io.File dir = new java.io.File(getExternalFilesDir(null), "updates");
        dir.mkdirs();
        java.io.File out = new java.io.File(dir, "dermes-" + latestTag + ".apk");
        if (out.exists()) out.delete();
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(downloadUrl));
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
