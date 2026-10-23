package ai.serge.dermes;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Hub v4.16 — focused entry: greeting, model-state hero with one CTA, quick list.
 * Update machinery lives in SettingsActivity; only the shared HTTP helper stays here.
 */
public class DermesActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.prepare(this);
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        View chip = UI.iconChip(this, R.drawable.ic_spark, UI.ON_ACCENT, 44, 22);
        chip.setBackground(UI.grad(this, 14));
        root.addView(chip);

        TextView greet = new TextView(this);
        UI.displayMedium(greet);
        greet.setText(greeting());
        greet.setPadding(0, UI.space24(this), 0, 0);
        root.addView(greet);
        UI.animateIn(greet, 40);

        root.addView(hero(), UI.block(this, 24));

        TextView sec = new TextView(this);
        UI.labelMedium(sec);
        sec.setTextColor(UI.TEXT_1);
        sec.setText("БЫСТРЫЙ ДОСТУП");
        sec.setLetterSpacing(0.06f);
        sec.setPadding(0, UI.space32(this), 0, UI.space8(this));
        root.addView(sec);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(UI.card(this, 24));
        list.setPadding(UI.space16(this), 0, UI.space16(this), 0);
        list.addView(UI.row(this, R.drawable.ic_feed, UI.ACCENT, "Дайджест", "Hermes morning feed",
                () -> startActivity(new android.content.Intent(this, DigestActivity.class))));
        list.addView(divider());
        list.addView(UI.row(this, R.drawable.ic_capsule, UI.VIOLET, "Капсулы", "from the future",
                () -> startActivity(new android.content.Intent(this, CapsuleActivity.class))));
        list.addView(divider());
        list.addView(UI.row(this, R.drawable.ic_runner, UI.TEAL, "Плунжер Ран", "tap. jump. repeat",
                () -> startActivity(new android.content.Intent(this, RunnerActivity.class))));
        root.addView(list);
        UI.animateIn(list, 200);

        root.addView(UI.footer(this, "v" + UI.version(this)), UI.block(this, 24));
        setContentView(Shell.wrap(this, Shell.HOME, "", "", root, true));
    }

    /** Model-state card + the single primary action of the whole app. */
    private View hero() {
        boolean ready = modelFile() != null;

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackground(UI.cardElevated(this, 28));
        hero.setElevation(UI.dp(this, 6));
        hero.setPadding(UI.space20(this), UI.space20(this), UI.space20(this), UI.space20(this));

        TextView state = new TextView(this);
        UI.labelLarge(state);
        state.setTextColor(ready ? UI.OK : UI.WARN);
        state.setText((ready ? "●  " : "○  ") + (ready ? "Модель готова · всё локально" : "Модель не загружена"));
        hero.addView(state);

        TextView desc = new TextView(this);
        UI.bodyMedium(desc);
        desc.setTextColor(UI.TEXT_1);
        desc.setLineSpacing(0, 1.3f);
        desc.setText(ready
                ? "Ни одного сетевого запроса. Разговоры остаются на телефоне."
                : "Один раз скачай ~5 ГБ — дальше чат полностью офлайн, даже в метро.");
        desc.setPadding(0, UI.space12(this), 0, 0);
        hero.addView(desc);

        android.widget.Button cta = new android.widget.Button(this);
        UI.buttonFilled(cta, this);
        cta.setText(ready ? "Продолжить чат" : "Скачать модель");
        cta.setPadding(0, UI.space12(this), 0, UI.space12(this));
        cta.setOnClickListener(v -> {
            Shell.press(v);
            startActivity(new android.content.Intent(this,
                    ready ? ChatActivity.class : PocketActivity.class));
        });
        hero.addView(cta, UI.block(this, 20));
        UI.animateIn(hero, 120);
        return hero;
    }

    private java.io.File modelFile() {
        java.io.File d = new java.io.File(getExternalFilesDir(null), "models");
        java.io.File[] fs = d.listFiles((x, n) -> n.endsWith(".gguf"));
        if (fs == null || fs.length == 0) return null;
        for (java.io.File f : fs) if (f.getName().startsWith("LFM2.5-8B")) return f;
        return fs[0];
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x0FFFFFFF);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, UI.dp(this, 1)));
        return v;
    }

    private String greeting() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String g = h < 6 ? "Доброй ночи" : h < 12 ? "Доброе утро" : h < 18 ? "Добрый день" : "Добрый вечер";
        return g + ", Сергей";
    }

    /** Shared public GET (Digest + Device read through this). */
    public static String httpGetPublic(String url) throws Exception {
        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("User-Agent", "dermes-updater");
        try {
            if (conn.getResponseCode() != 200) throw new Exception("http " + conn.getResponseCode());
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            r.close();
            return sb.toString();
        } finally { conn.disconnect(); }
    }
}
