package ai.serge.dermes;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Per-day activity log: app opens, foreground minutes, game runs and swipes.
 * SharedPreferences, one line per day: "yyyy-MM-dd=opens,minutes,runs,swipes".
 * Writer side: ActivityLog.onOpen/onClose/onRun/onSwipe (any thread).
 */
final class ActivityLog {

    static final class Day {
        final String date; int opens, minutes, runs, swipes;
        Day(String date, int o, int m, int r, int s) { this.date = date; opens=o; minutes=m; runs=r; swipes=s; }
    }

    private static final String PREFS = "activity";
    private static final String KEY = "days";
    private static long fgSince;

    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    /** Refcount foreground across all activities; first resume of a session = "open". */
    private static boolean tracking;
    static synchronized void track(android.app.Activity a) {
        if (tracking) return;
        tracking = true;
        a.getApplication().registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks() {
            private int started;
            @Override public void onActivityResumed(android.app.Activity x) {
                if (started++ == 0) onOpen(x);
            }
            @Override public void onActivityPaused(android.app.Activity x) {
                if (--started <= 0) { started = 0; onClose(x); }
            }
            @Override public void onActivityCreated(android.app.Activity x, android.os.Bundle s) {}
            @Override public void onActivityStarted(android.app.Activity x) {}
            @Override public void onActivityStopped(android.app.Activity x) {}
            @Override public void onActivitySaveInstanceState(android.app.Activity x, android.os.Bundle s) {}
            @Override public void onActivityDestroyed(android.app.Activity x) {}
        });
    }


    static synchronized List<Day> load(Context c) {
        List<Day> out = new ArrayList<>();
        for (String line : c.getSharedPreferences(PREFS, 0).getString(KEY, "").split("\n")) {
            String[] kv = line.split("=");
            if (kv.length != 2) continue;
            String[] v = kv[1].split(",");
            if (v.length != 4) continue;
            try { out.add(new Day(kv[0], Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                    Integer.parseInt(v[2]), Integer.parseInt(v[3]))); }
            catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private static synchronized void save(Context c, List<Day> days) {
        StringBuilder sb = new StringBuilder();
        for (Day d : days) sb.append(d.date).append('=').append(d.opens).append(',')
                .append(d.minutes).append(',').append(d.runs).append(',').append(d.swipes).append('\n');
        c.getSharedPreferences(PREFS, 0).edit().putString(KEY, sb.toString()).apply();
    }

    private static Day todayRow(Context c, List<Day> days) {
        String td = today();
        for (Day d : days) if (d.date.equals(td)) return d;
        Day d = new Day(td, 0, 0, 0, 0);
        days.add(d);
        while (days.size() > 14) days.remove(0);
        return d;
    }

    static synchronized void onOpen(Context c) { List<Day> ds = load(c); todayRow(c, ds).opens++; fgSince = System.currentTimeMillis(); save(c, ds); }
    static synchronized void onClose(Context c) {
        if (fgSince == 0) return;
        List<Day> ds = load(c);
        todayRow(c, ds).minutes += (int) Math.max(0, (System.currentTimeMillis() - fgSince) / 60_000);
        fgSince = 0; save(c, ds);
    }
    static synchronized void onRun(Context c)   { List<Day> ds = load(c); todayRow(c, ds).runs++;   save(c, ds); }
    static synchronized void onSwipe(Context c, int n) { List<Day> ds = load(c); todayRow(c, ds).swipes += n; save(c, ds); }
}
