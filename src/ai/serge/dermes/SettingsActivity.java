package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Settings v4.16: app/update status (updater moved here from the hub), storage, device,
 * history. Install flow unchanged: DownloadManager (survives TUN) -> PackageInstaller
 * session, no APK left on disk.
 */
public class SettingsActivity extends Activity {

    private static final String REPO = "serkonyc/hermes-dermes-android";
    private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private TextView status;
    private android.widget.Button action;
    private String latestTag, downloadUrl;
    private long expectedSize;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);
        sweepOldApks();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // version + update card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UI.cardElevated(this, 28));
        card.setElevation(UI.dp(this, 6));
        card.setPadding(UI.space20(this), UI.space20(this), UI.space20(this), UI.space20(this));

        TextView ver = new TextView(this);
        UI.titleMedium(ver);
        ver.setText("Dermes " + UI.version(this));
        card.addView(ver);

        status = new TextView(this);
        UI.bodySmall(status);
        status.setTextColor(UI.TEXT_2);
        status.setText("проверяем обновления…");
        status.setPadding(0, UI.space6(this), 0, 0);
        card.addView(status);

        action = new android.widget.Button(this);
        UI.buttonTonal(action, this);
        action.setText("Проверить обновления");
        action.setOnClickListener(v -> onUpdate());
        card.addView(action, UI.block(this, 16));
        root.addView(card);

        // grouped rows
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(UI.card(this, 24));
        list.setPadding(UI.space16(this), 0, UI.space16(this), 0);
        list.addView(UI.row(this, R.drawable.ic_models, UI.ACCENT, "Модель и хранилище",
                "LFM2.5-8B · ~5 ГБ · выживает обновления",
                () -> startActivity(new Intent(this, PocketActivity.class))));
        list.addView(divider());
        list.addView(UI.row(this, R.drawable.ic_pulse, UI.TEAL, "Состояние устройства",
                "RAM, температура, runtime",
                () -> startActivity(new Intent(this, DeviceActivity.class))));
        list.addView(divider());
        list.addView(UI.row(this, R.drawable.ic_bars, UI.OK, "История использования",
                "7-дневная статистика",
                () -> startActivity(new Intent(this, HistoryActivity.class))));
        root.addView(list, UI.block(this, 24));

        root.addView(UI.footer(this, "локальный режим · данные не покидают телефон"), UI.block(this, 24));
        setContentView(Shell.wrap(this, Shell.SETTINGS, "Настройки", null, root, true));
        checkForUpdate();
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x0FFFFFFF);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, UI.dp(this, 1)));
        return v;
    }

    // ---------- updater (moved verbatim from the hub; behavior unchanged) ----------

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
                JSONObject rel = new JSONObject(DermesActivity.httpGetPublic(
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
                    status.setText("v" + cur + " установлено · " + (fNote != null ? fNote : "релизов пока нет"));
                    return;
                }
                if (isNewer(fLatest, cur) && fDl != null) {
                    latestTag = fLatest;
                    downloadUrl = fDl;
                    status.setTextColor(UI.OK);
                    status.setText("v" + fLatest + " доступна для установки");
                    action.setText("Обновить до v" + fLatest);
                    UI.buttonFilled(action, this);
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        status.setText(status.getText()
                                + " · разреши установку из неизвестных источников");
                    }
                } else {
                    latestTag = null;
                    downloadUrl = null;
                    status.setTextColor(UI.TEXT_2);
                    status.setText("v" + cur + " · обновлений нет");
                }
            });
        });
    }

    private boolean staging = false;

    private void onUpdate() {
        if (staging) return;                       // second tap would race the installer
        if (latestTag == null || downloadUrl == null) { checkForUpdate(); return; }
        if (!getPackageManager().canRequestPackageInstalls()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                status.setText("Настройки → Приложения → Dermes → Установка неизвестных приложений, затем повтори.");
            }
            return;
        }
        staging = true;
        action.setEnabled(false);
        action.setText("скачиваем…");
        java.io.File dir = new java.io.File(getExternalFilesDir(null), "updates");
        dir.mkdirs();
        java.io.File out = new java.io.File(dir, "dermes-" + latestTag + ".apk");
        if (out.exists()) out.delete();
        try {
            // DownloadManager survives the phone's TUN proxy where raw sockets don't;
            // install itself goes through PackageInstaller (ACTION_VIEW handoff gets
            // rejected on One UI 15 as "can't install app").
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(downloadUrl));
            r.setDestinationUri(Uri.fromFile(out));
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setTitle("Dermes update");
            dm().enqueue(r);
            status.setText("скачиваем… системное окно откроется по завершении");
            pollInstall(out);
        } catch (Exception e) {
            staging = false; action.setEnabled(true); action.setText("Обновить до v" + latestTag);
            status.setText("download failed: " + e);
        }
    }

    private DownloadManager dm() { return (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE); }

    /** Delete APK leftovers (incl. files from the old ACTION-view flow). */
    private void sweepOldApks() {
        java.io.File[] fs = new java.io.File(getExternalFilesDir(null), "updates").listFiles();
        if (fs != null) for (java.io.File f : fs) if (f.getName().endsWith(".apk")) f.delete();
    }

    private void pollInstall(java.io.File apk) {
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            int misses = 0; long last = -1;
            @Override public void run() {
                if (isDestroyed()) return;
                long len = apk.exists() ? apk.length() : -1;
                // size must equal the release asset AND be stable for one tick: a torn
                // APK handed to PackageInstaller fails as INSTALL_PARSE_FAILED_NOT_APK
                if (len == expectedSize && len == last && zipSane(apk)) stageAndInstall(apk);
                else if (++misses < 80) h.postDelayed(this, 1500); // ~2 min cap
                else { staging = false; action.setEnabled(true);
                       status.setText("скачивание не завершилось; повтори обновление"); }
                last = len;
            }
        }, 1500);
    }

    /** Cheap zip integrity: local header up front, EOCD record at the tail. */
    private boolean zipSane(java.io.File f) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            if (raf.length() < 30) return false;
            byte[] h = new byte[2]; raf.readFully(h);
            if (h[0] != 'P' || h[1] != 'K') return false;
            byte[] t = new byte[(int) Math.min(64, raf.length())];
            raf.seek(raf.length() - t.length); raf.readFully(t);
            for (int i = 0; i + 3 < t.length; i++)
                if (t[i] == 'P' && t[i+1] == 'K' && t[i+2] == 5 && t[i+3] == 6) return true;
            return false;
        } catch (Exception e) { return false; }
    }

    /** Stream the APK into an installer session, drop the file, show the system sheet. */
    private void stageAndInstall(java.io.File apk) {
        status.setText("подготавливаем установку…");
        new Thread(() -> {
            int sessionId = -1;
            try {
                android.content.pm.PackageInstaller pi = getPackageManager().getPackageInstaller();
                android.content.pm.PackageInstaller.SessionParams sp =
                    new android.content.pm.PackageInstaller.SessionParams(
                        android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sessionId = pi.createSession(sp);
                Intent result = new Intent(this, InstallResult.class);
                int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        | android.app.PendingIntent.FLAG_MUTABLE;
                final android.app.PendingIntent piIntent = android.app.PendingIntent.getBroadcast(
                        this, sessionId, result, flags);
                try (android.content.pm.PackageInstaller.Session sess = pi.openSession(sessionId);
                     java.io.InputStream in = new java.io.FileInputStream(apk)) {
                    java.io.OutputStream out = sess.openWrite("base.apk", 0, apk.length());
                    byte[] buf = new byte[128 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    sess.fsync(out);
                    out.close();
                    sess.commit(piIntent.getIntentSender()); // opens the system install sheet
                }
                apk.delete(); // bytes live in the session now - no apk junk in storage
                uiPost("подтверди обновление в системном окне");
                runOnUiThread(() -> { staging = false; action.setEnabled(true); });
            } catch (Exception e) {
                if (sessionId >= 0) try { getPackageManager().getPackageInstaller().abandonSession(sessionId); } catch (Exception ignored) {}
                apk.delete();
                uiPost("stage failed: " + e);
                runOnUiThread(() -> { staging = false; action.setEnabled(true); });
            }
        }).start();
    }

    private void uiPost(String s) { runOnUiThread(() -> { if (status != null) status.setText(s); }); }

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
}
