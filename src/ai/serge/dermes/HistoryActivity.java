package ai.serge.dermes;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * App activity: opens, foreground minutes and Plunger runs per day as 7-day bars
 * plus a table with swipes. Fed by ActivityLog; offline, one SharedPreferences line
 * per day.
 */
public class HistoryActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        View title = UI.headerRow(this, "Активность", "открытия · минуты · забеги · свайпы");

        BarsView opens = new BarsView(this, "открытий приложения");
        BarsView mins  = new BarsView(this, "минут в приложении");
        BarsView runs  = new BarsView(this, "забегов Плунжер");

        TextView table = new TextView(this);
        UI.bodySmall(table);
        table.setTextColor(UI.TEXT_1);
        table.setPadding(UI.space16(this), UI.space8(this), UI.space16(this), 0);

        List<ActivityLog.Day> days = ActivityLog.load(this);
        opens.setData(days, d -> d.opens);
        mins.setData(days,  d -> d.minutes);
        runs.setData(days,  d -> d.runs);

        StringBuilder t = new StringBuilder(
                String.format(Locale.US, "%-11s %5s %6s %6s %7s\n", "день", "откр", "минут", "забег", "свайп"));
        for (ActivityLog.Day d : days)
            t.append(String.format(Locale.US, "%-11s %5d %6d %6d %7d\n", d.date, d.opens, d.minutes, d.runs, d.swipes));
        if (days.isEmpty()) t.append("пусто — попользуйся приложением день-другой");
        table.setText(t);

        content.addView(title);
        content.addView(opens, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(mins,  new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(runs,  new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(table);
        content.addView(UI.footer(this, "v" + UI.version(this)));
        setContentView(Shell.wrap(this, -1, "Активность", "App usage", content, true, this::finish));
    }

    // ---------- bars ----------
    interface Get { int of(ActivityLog.Day d); }

    static final class BarsView extends View {
        private final String label; private List<ActivityLog.Day> days = new ArrayList<>(); private Get get;
        private final Paint barP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        BarsView(android.content.Context c, String label) {
            super(c); this.label = label;
            barP.setColor(UI.ACCENT);
            txt.setTextSize(9 * c.getResources().getDisplayMetrics().density);
            txt.setColor(UI.TEXT_1);
        }
        void setData(List<ActivityLog.Day> d, Get g) {
            int n = d.size();
            days = new ArrayList<>(d.subList(Math.max(0, n - 7), n));
            get = g; invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
            if (days.isEmpty() || get == null) return;
            float max = 1;
            for (ActivityLog.Day x : days) max = Math.max(max, get.of(x));
            float pad = 16 * d, top = 18 * d;
            float bw = (w - pad * 2) / days.size();
            txt.setTextSize(11 * d); txt.setColor(UI.TEXT_2);
            canvas.drawText(label.toUpperCase(Locale.US), pad, 11 * d, txt);
            for (int i = 0; i < days.size(); i++) {
                ActivityLog.Day x = days.get(i);
                int v = get.of(x);
                float bh = (h - top - 16 * d) * (v / max);
                float x0 = pad + i * bw + bw * 0.18f, x1 = pad + (i + 1) * bw - bw * 0.18f;
                canvas.drawRoundRect(x0, h - 14 * d - bh, x1, h - 14 * d, 4 * d, 4 * d, barP);
                canvas.drawText(x.date.substring(5), x0, h - 3 * d, txt);   // MM-dd
                if (v > 0) canvas.drawText(String.valueOf(v), x0, h - 16 * d - bh, txt);
            }
        }
    }
}
