package ai.serge.dermes;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.TrafficStats;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.StatFs;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.os.Environment;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Locale;

/**
 * Device Intel: everything a normal (non-root) app may read about the phone, refreshed
 * every 2 s: per-core CPU load+freq, thermal status, battery (level/voltage/temp/current),
 * memory (device + app heap), storage, GPU via EGL, network interfaces, runtime counters.
 * Honest labels: Android hides CPU/SoC temperature from apps without root, so the screen
 * shows the official thermal status instead and says so.
 * Kept: the llama.cpp runtime fetch button (legacy hub role).
 */
public class DeviceActivity extends Activity {

    private static final String RUNTIME_API =
            "https://api.github.com/repos/jegly/OfflineLLM/releases/latest";
    private long dmId = -1;
    private TextView info;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private long[] lastIdle, lastTotal;
    private boolean alive = true;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!alive || isDestroyed()) return;
            info.setText(collect());
            ui.postDelayed(this, 2000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);

        info = new TextView(this);
        UI.bodyMedium(info);
        info.setTextColor(UI.TEXT_1);

        Button fetch = new Button(this);
        fetch.setText("Fetch llama.cpp runtime");
        UI.buttonFilled(fetch, this);
        Button open = new Button(this);
        open.setText("Open model catalog (Hugging Face)");
        UI.buttonTonal(open, this);
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
                    runOnUiThread(() -> { fetch.setText("download " + ver + " apk"); startDownload(fUrl); });
                } catch (Exception e) {
                    runOnUiThread(() -> { fetch.setEnabled(true); fetch.setText("fetch failed: " + e.getMessage()); });
                }
            }).start();
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ScrollView sc = new ScrollView(this);
        sc.addView(info);
        content.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(fetch, UI.block(this, 12));
        content.addView(open, UI.block(this, 8));
        content.addView(UI.footer(this, "v" + UI.version(this)), UI.block(this, 16));

        setContentView(Shell.wrap(this, -1, "Device", "full device metrics · live 2s", content, true, this::finish));

        info.setText(collect());
        ui.postDelayed(tick, 2000);
    }

    @Override protected void onDestroy() {
        alive = false; ui.removeCallbacks(tick); super.onDestroy();
    }

    // ---------- metrics ----------
    private String collect() {
        StringBuilder s = new StringBuilder();
        Context c = this;

        // CPU
        int cores = Runtime.getRuntime().availableProcessors();
        s.append("CPU · ").append(android.os.Build.HARDWARE).append(" · ").append(cores).append(" ядер\n");
        long[] idle = readCpuStat(4), total = readCpuStat(0);   // /proc/stat aggregate
        if (lastIdle != null && total != null && total.length == lastTotal.length) {
            StringBuilder loads = new StringBuilder();
            long dtIdle = idle[0] - lastIdle[0], dtTot = total[0] - lastTotal[0];
            int pct = dtTot > 0 ? (int) (100 * (dtTot - dtIdle) / dtTot) : 0;
            loads.append("load: ").append(pct).append("%");
            s.append(loads).append('\n');
        }
        lastIdle = idle; lastTotal = total;
        for (int i = 0; i < cores && i < 8; i++) {
            String cur = readSysfs("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
            String max = readSysfs("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_max_freq");
            if (cur != null) s.append("  core").append(i).append(": ")
                    .append(Integer.parseInt(cur.trim()) / 1000).append(" MHz");
            if (max != null) s.append(" / max ").append(Integer.parseInt(max.trim()) / 1000).append(" MHz");
            s.append('\n');
        }

        // thermal (no CPU temp without root - say it)
        try {
            PowerManager pm = (PowerManager) c.getSystemService(POWER_SERVICE);
            String[] names = {"nominal", "fair", "mild", "severe", "critical", "emergency", "shutdown"};
            int ts = pm.getCurrentThermalStatus();
            s.append("thermal status: ").append(ts < names.length ? names[ts] : "?").append("\n");
        } catch (Exception ignored) {}
        s.append("(температуру чипа Android не даёт приложениям — только этот статус)\n");

        // battery: sticky broadcast carries everything instantly
        Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat != null) {
            int lvl = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int temp = bat.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int volt = bat.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            String[] st = {"?", "unknown", "charging", "discharging", "full", "not charging"};
            s.append("battery: ").append(100 * lvl / scale).append("% · ")
                    .append(status < st.length ? st[status] : "?")
                    .append(plugged > 0 ? " (зарядка)" : "").append('\n');
            s.append("  temp: ").append(temp / 10.f).append(" °C · voltage: ").append(volt).append(" mV\n");
            try {   // hidden but readable: charge current (µA) and battery capacity
                BatteryManager bm = (BatteryManager) c.getSystemService(BATTERY_SERVICE);
                int cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                int cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                long charge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                s.append("  current: ").append(cur / 1000.f).append(" mA · charge: ").append(charge).append(" µAh · capacity: ").append(cap).append("%\n");
            } catch (Exception ignored) {}
        }

        // memory
        android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        Runtime rt = Runtime.getRuntime();
        long appUsed = (rt.totalMemory() - rt.freeMemory()) / 1048576;
        s.append("RAM: ").append(mb(mi.totalMem / 1048576)).append(" MB total · ")
                .append(mb(mi.availMem / 1048576)).append(" MB free")
                .append(mi.lowMemory ? " · LOW!" : "").append('\n');
        s.append("native pid ").append(Process.myPid()).append(" · java heap ").append(appUsed).append(" / ").append(rt.maxMemory() / 1048576).append(" MB\n");

        // storage
        File data = Environment.getDataDirectory();
        StatFs sf = new StatFs(data.getPath());
        s.append("storage: ").append(mb(sf.getAvailableBytes() / 1048576)).append(" MB free of ")
                .append(mb(sf.getTotalBytes() / 1048576)).append(" MB\n");

        // GPU via EGL
        try {
            android.opengl.GLSurfaceView probe = null;
            javax.microedition.khronos.egl.EGL10 egl = (javax.microedition.khronos.egl.EGL10) javax.microedition.khronos.egl.EGLContext.getEGL();
            javax.microedition.khronos.egl.EGLDisplay dpy = egl.eglGetDisplay(javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY);
            int[] ver = new int[2]; egl.eglInitialize(dpy, ver);
            String ext = egl.eglQueryString(dpy, javax.microedition.khronos.egl.EGL10.EGL_EXTENSIONS);
            s.append("GPU: EGL ").append(ver[0]).append('.').append(ver[1]);
            if (ext != null) {
                int i = ext.indexOf("Adreno"); if (i < 0) i = ext.indexOf("Mali"); if (i < 0) i = ext.indexOf("VIV");
                if (i >= 0) s.append(" · ").append(ext, i, Math.min(ext.length(), i + 24));
            }
            s.append('\n');
        } catch (Throwable t) {
            s.append("GPU: query failed (").append(t.getClass().getSimpleName()).append(")\n");
        }

        // network
        try {
            NetworkInfo ni = ((android.net.ConnectivityManager) c.getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            s.append("network: ").append(ni != null && ni.isConnected() ? ni.getTypeName() : "offline").append('\n');
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                java.util.List<java.net.InetAddress> addrs = Collections.list(nif.getInetAddresses());
                for (java.net.InetAddress a : addrs) {
                    if (a instanceof java.net.Inet4Address) {
                        s.append("  ").append(nif.getName()).append(": ").append(a.getHostAddress())
                                .append(" mtu ").append(nif.getMTU()).append('\n');
                    }
                }
            }
            long rx = TrafficStats.getTotalRxBytes(), tx = TrafficStats.getTotalTxBytes();
            s.append("traffic: RX ").append(rx / 1048576).append(" MB · TX ").append(tx / 1048576).append(" MB (since boot)\n");
        } catch (Exception ignored) {}

        // system
        s.append("android ").append(Build.VERSION.RELEASE).append(" (sdk ").append(Build.VERSION.SDK_INT)
                .append(") · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        s.append("uptime: ").append(android.os.SystemClock.elapsedRealtime() / 3600000).append("h ")
                .append(android.os.SystemClock.elapsedRealtime() / 60000 % 60).append("m\n");
        return s.toString();
    }

    private long[] readCpuStat(int field) {
        try (RandomAccessFile f = new RandomAccessFile("/proc/stat", "r")) {
            String line = f.readLine();  // aggregate "cpu ..." line
            String[] p = line.split("\\s+");
            long v = 0, tot = 0;
            for (int i = 1; i < p.length; i++) { long x = Long.parseLong(p[i]); if (i == field) v = x; tot += x; }
            return new long[]{field == 0 ? tot : v};
        } catch (Exception e) { return null; }
    }

    private String readSysfs(String path) {
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(path))) {
            return r.readLine();
        } catch (Exception e) { return null; }
    }

    private void startDownload(String url) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            java.io.File dir = new java.io.File(getExternalFilesDir(null), "runtimes");
            dir.mkdirs();
            r.setDestinationUri(Uri.fromFile(new java.io.File(dir, "llama-runtime.apk")));
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            dmId = dm.enqueue(r);
        } catch (Exception e) {
            android.widget.Toast.makeText(DeviceActivity.this, "download failed: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String u) {
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) {}
    }
    private static String mb(long v) { return String.format(Locale.US, "%,d", v); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
