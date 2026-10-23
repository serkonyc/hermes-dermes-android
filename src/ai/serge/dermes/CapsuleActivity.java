package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
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
        Shell.prepare(this);

        labelIn = field("label (visible while sealed)", false);
        bodyIn = field("message to your future self…", true);

        final TextView when = new TextView(this);
        UI.bodyMedium(when);
        when.setTextColor(UI.TEXT_1);
        when.setText(fmt(unlockAt));
        Button plus1h = chip("+1h", () -> { unlockAt += 3600_000L; when.setText(fmt(unlockAt)); });
        Button plus1d = chip("+1d", () -> { unlockAt += 86_400_000L; when.setText(fmt(unlockAt)); });
        Button reset  = chip("now",  () -> { unlockAt = System.currentTimeMillis(); when.setText(fmt(unlockAt)); });

        Button seal = new Button(this);
        seal.setText("Seal capsule");
        UI.buttonFilled(seal, this);
        seal.setOnClickListener(v -> seal(when));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, UI.space8(this), 0, UI.space8(this));
        ScrollView sc = new ScrollView(this);
        sc.addView(list);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(labelIn);
        content.addView(bodyIn);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(0, UI.space6(this), 0, 0);
        chips.addView(new TextView(this) {{ setText("unlocks: "); setTextColor(UI.TEXT_2); }});
        chips.addView(when);
        content.addView(chips);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, 0, 0, UI.space6(this));
        row.addView(plus1h); row.addView(plus1d); row.addView(reset);
        content.addView(row);
        content.addView(seal);
        content.addView(UI.footer(this, "v" + UI.version(this)), UI.block(this, 16));

        setContentView(Shell.wrap(this, -1, "Time Capsules", "Sealed notes that unlock later", content, true, this::finish));

        render();
    }

    private EditText field(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        UI.bodyMedium(e);
        e.setHintTextColor(UI.TEXT_2);
        e.setMaxLines(multiline ? 3 : 1);
        if (!multiline) e.setInputType(InputType.TYPE_CLASS_TEXT);
        else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setPadding(UI.space18(this), UI.space12(this), UI.space18(this), UI.space12(this));
        e.setBackground(UI.card(CapsuleActivity.this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UI.space6(this), 0, 0);
        e.setLayoutParams(lp);
        return e;
    }

    private Button chip(String t, Runnable r) {
        Button b = new Button(this);
        b.setText(t); b.setMinWidth(0); b.setMinHeight(0);
        UI.buttonTonal(b, this); // buttonTonal owns padding+type; chip adds nothing
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
                UI.bodyMedium(t);
                t.setBackground(UI.card(CapsuleActivity.this, 18));
                t.setPadding(UI.space16(this), UI.space12(this), UI.space16(this), UI.space14(this));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = UI.space8(this);
                t.setLayoutParams(lp);
                if (!open) {
                    t.setText("🔒 " + o.getString("label") + "\nsealed · " + countdown(at));
                    t.setTextColor(UI.TEXT_2);
                } else {
                    String body;
                    try { body = dec(o.getString("cipher"), at); }
                    catch (Exception e) { body = "(corrupted)"; }
                    t.setText("✉ " + o.getString("label") + " · opened\n\n" + body);
                    t.setTextColor(UI.TEXT_0);
                    final int idx = i;
                    t.setOnLongClickListener(v -> { delete(idx); return true; });
                }
                UI.animateIn(t, i * 50L);
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

}
