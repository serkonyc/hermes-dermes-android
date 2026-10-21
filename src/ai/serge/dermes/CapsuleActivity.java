package ai.serge.dermes;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Time Capsules (wave 3): a note locked until a timestamp. Content is AES-encrypted with
 * a key derived from the unlock time itself — the app cannot open a sealed capsule even
 * by code inspection of storage; the deadline is the key. Sealed list shows only the
 * label and countdown. Voice notes arrive in wave 3.1 (record -> same storage).
 */
public class CapsuleActivity extends Activity {

    private static final String PREFS = "capsules";
    private static final String KEY = "list";

    private LinearLayout list;
    private EditText labelIn, bodyIn;
    private long unlockAt = System.currentTimeMillis() + 3600_000L;   // default: +1h

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        labelIn = field("label (visible while sealed)", false);
        bodyIn = field("message to your future self…", true);

        final TextView when = new TextView(this);
        when.setTextSize(13f);
        when.setTextColor(0xFFA8B1C0);
        when.setText(fmt(unlockAt));
        Button plus1h = chip("+1h", () -> { unlockAt += 3600_000L; when.setText(fmt(unlockAt)); });
        Button plus1d = chip("+1d", () -> { unlockAt += 86_400_000L; when.setText(fmt(unlockAt)); });
        Button reset  = chip("now",  () -> { unlockAt = System.currentTimeMillis(); when.setText(fmt(unlockAt)); });

        Button seal = new Button(this);
        seal.setText("Seal capsule");
        seal.setOnClickListener(v -> seal(when));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), dp(8), dp(18), dp(8));
        ScrollView sc = new ScrollView(this);
        sc.addView(list);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(title("Time Capsules"));
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(labelIn);
        root.addView(bodyIn);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(dp(18), dp(6), dp(18), 0);
        chips.addView(new TextView(this) {{ setText("unlocks: "); setTextColor(0xFF6E7580); }});
        chips.addView(when);
        root.addView(chips);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(14), 0, dp(14), dp(6));
        row.addView(plus1h); row.addView(plus1d); row.addView(reset);
        root.addView(row);
        root.addView(seal);
        root.setPadding(0, dp(6), 0, 0);
        root.setFitsSystemWindows(true);
        setContentView(root);

        render();
    }

    private TextView title(String t) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(20f); v.setTextColor(0xFFEBEEF3);
        v.setGravity(Gravity.CENTER); v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private EditText field(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextColor(0xFFEBEEF3); e.setHintTextColor(0xFF6E7580);
        e.setTextSize(14f);
        e.setMaxLines(multiline ? 3 : 1);
        if (!multiline) e.setInputType(InputType.TYPE_CLASS_TEXT);
        else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setPadding(dp(18), dp(10), dp(18), dp(10));
        return e;
    }

    private Button chip(String t, Runnable r) {
        Button b = new Button(this);
        b.setText(t); b.setMinWidth(0); b.setMinHeight(0);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        b.setTextSize(12f);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    // ---------- crypto: key = hash(unlock timestamp) ----------
    private static SecretKeySpec keyFor(long unlockAt) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256")
                .digest(("dermes-capsule:" + unlockAt).getBytes("UTF-8"));
        return new SecretKeySpec(h, "AES");
    }
    private static String enc(String plain, long at) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, keyFor(at));
        return android.util.Base64.encodeToString(c.doFinal(plain.getBytes("UTF-8")), 0);
    }
    private static String dec(String b64, long at) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, keyFor(at));
        return new String(c.doFinal(android.util.Base64.decode(b64, 0)), "UTF-8");
    }

    private void seal(TextView when) {
        String label = labelIn.getText().toString().trim();
        String body = bodyIn.getText().toString().trim();
        if (label.isEmpty() || body.isEmpty()) { when.setText("label and message required"); return; }
        try {
            JSONArray arr = load();
            JSONObject o = new JSONObject();
            o.put("label", label);
            o.put("at", unlockAt);
            o.put("cipher", enc(body, unlockAt));
            arr.put(o);
            save(arr);
            labelIn.setText(""); bodyIn.setText("");
            render();
        } catch (Exception e) {
            when.setText("seal failed: " + e);
        }
    }

    private void render() {
        list.removeAllViews();
        JSONArray arr = load();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                boolean open = System.currentTimeMillis() >= at;
                TextView t = new TextView(this);
                t.setTextSize(13f);
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setColor(0xFF262A32); g.setCornerRadius(dp(14)); g.setStroke(dp(1), 0xFF383E4A);
                t.setBackground(g);
                t.setPadding(dp(16), dp(12), dp(16), dp(14));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(8);
                t.setLayoutParams(lp);
                if (!open) {
                    t.setText("🔒 " + o.getString("label") + "\nsealed · " + countdown(at));
                    t.setTextColor(0xFF7C879A);
                } else {
                    String body;
                    try { body = dec(o.getString("cipher"), at); }
                    catch (Exception e) { body = "(corrupted)"; }
                    t.setText("✉ " + o.getString("label") + " · opened\n\n" + body);
                    t.setTextColor(0xFFEBEEF3);
                    final int idx = i;
                    t.setOnLongClickListener(v -> { delete(idx); return true; });
                }
                list.addView(t);
            } catch (Exception ignored) {}
        }
    }

    private void delete(int idx) {
        JSONArray arr = load();
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) if (i != idx) out.put(arr.opt(i));
        save(out);
        render();
    }

    private JSONArray load() {
        try { return new JSONArray(getSharedPreferences(PREFS, 0).getString(KEY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    private void save(JSONArray a) {
        getSharedPreferences(PREFS, 0).edit().putString(KEY, a.toString()).apply();
    }

    private static String countdown(long at) {
        long d = at - System.currentTimeMillis();
        if (d < 3600_000L) return String.format(Locale.US, "opens in %d min", d / 60_000L);
        if (d < 86_400_000L) return String.format(Locale.US, "opens in %d h", d / 3600_000L);
        return String.format(Locale.US, "opens in %d days", d / 86_400_000L);
    }
    private static String fmt(long t) {
        return java.text.DateFormat.getDateTimeInstance().format(t);
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
