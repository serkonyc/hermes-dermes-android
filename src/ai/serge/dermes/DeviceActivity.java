package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Device Intel + on-device LLM experiment hub.
 *
 * Reality (measured, not guessed): a 1B-param model at q4_k_m is ~600 MB of weights,
 * which fits a 12 GB phone easily. But running it needs llama.cpp native binaries
 * (arm64-v8a .so). This app is pure Java without NDK, so the honest path is:
 *  1. report device capability (RAM, ABI, free storage);
 *  2. download a community llama.cpp Android runtime APK (ChatterUI, MIT, bundles
 *     llama.cpp .so + a model picker) and offer to install it side-by-side;
 *  3. Dermes stays the monitor: Mac serves big models, the phone runs its own 1B brain
 *     through the bundled runtime.
 * No fake "in-app inference" claims.
 */
public class DeviceActivity extends Activity {

    // OfflineLLM: llama.cpp Android frontend (arm64, Android 13+), apk on GitHub releases.
    private static final String RUNTIME_API =
            "https://api.github.com/repos/jegly/OfflineLLM/releases/latest";
    private long dmId = -1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        Runtime rt = Runtime.getRuntime();
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long ram = mi.totalMem;
        File data = Environment.getDataDirectory();
        long free = data.getUsableSpace();
        String abi = android.os.Build.SUPPORTED_ABIS.length > 0 ? android.os.Build.SUPPORTED_ABIS[0] : "?";

        StringBuilder sb = new StringBuilder();
        sb.append("SoC family: ").append(android.os.Build.HARDWARE).append('\n');
        sb.append("ABI: ").append(abi).append("  (arm64 = llama.cpp capable)\n");
        sb.append("RAM budget for apps: ").append(mb(ram)).append(" MB\n");
        sb.append("free storage: ").append(mb(free / 1024 / 1024)).append(" MB\n\n");
        sb.append("Local 1B model (q4_k_m ≈ 600 MB weights): ");
        boolean fits = ram > 4L * 1024 * 1024 * 1024 && free > 1_500L * 1024 * 1024
                && abi.contains("arm64");
        sb.append(fits ? "FITS on this device.\n" : "does not fit; see numbers above.\n");
        sb.append("\nDermes itself cannot execute GGUF (no native llama.cpp in a pure-Java"
                + " build). The supported route: grab the llama.cpp runtime below and run a"
                + " 1B model next to this app. Mac stays the heavy lifting, phone gets a"
                + " private pocket brain.");

        TextView info = new TextView(this);
        info.setText(sb);
        info.setTextSize(13f);
        info.setTextColor(0xFFC9D1D9);
        info.setPadding(dp(18), dp(14), dp(18), dp(8));

        Button fetch = new Button(this);
        fetch.setText("Fetch llama.cpp runtime");
        Button open = new Button(this);
        open.setText("Open model catalog (Hugging Face)");
        open.setOnClickListener(v -> openUrl("https://huggingface.co/models?pipeline_tag=text-generation&sort=downloads"));

        fetch.setOnClickListener(v -> {
            fetch.setEnabled(false);
            fetch.setText("querying GitHub…");
            new Thread(() -> {
                try {
                    String resp = DermesActivity.httpGetPublic(RUNTIME_API);
                    org.json.JSONObject j = new org.json.JSONObject(resp);
                    String url = null;
                    org.json.JSONArray as = j.getJSONArray("assets");
                    for (int i = 0; i < as.length(); i++) {
                        String n = as.getJSONObject(i).optString("name", "");
                        if (n.endsWith(".apk")) { url = as.getJSONObject(i).getString("browser_download_url"); break; }
                    }
                    if (url == null) throw new Exception("no apk asset in release");
                    String ver = j.optString("tag_name");
                    final String fUrl = url;
                    runOnUiThread(() -> {
                        fetch.setText("download " + ver + " apk");
                        startDownload(fUrl);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        fetch.setEnabled(true);
                        fetch.setText("fetch failed: " + e.getMessage());
                    });
                }
            }).start();
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D1117);
        root.setFitsSystemWindows(true);
        ScrollView sc = new ScrollView(this);
        sc.addView(info);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(fetch);
        root.addView(open);
        TextView title = new TextView(this);
        title.setText("Device intel");
        title.setTextSize(16f);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(4));
        root.addView(title, 0);
        setContentView(root);

        registerReceiver(onDone, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private final BroadcastReceiver onDone = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != dmId) return;
            android.widget.Toast.makeText(DeviceActivity.this,
                    "runtime apk downloaded — open Downloads and install",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    };

    private void startDownload(String url) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "llama-runtime-" + new SimpleDateFormat("HHmm", Locale.US)
                            .format(new Date()) + ".apk");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            dmId = dm.enqueue(r);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "download failed: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) {}
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(onDone); } catch (Exception ignored) {}
    }
    private static String mb(long v) { return String.format(Locale.US, "%,d", v); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
