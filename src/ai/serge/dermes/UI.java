package ai.serge.dermes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Design tokens + shared chrome (v4.1.0): every screen composes from these constants so
 * spacing/color/typography can't drift again. 8dp grid, one radius family, real vector
 * icons from res/drawable (no more hand-drawn glyphs).
 */
public final class UI {
    UI() {}

    // dp scale resolved once per call site (density differs cover vs inner display)
    public static float d(Context c) { return c.getResources().getDisplayMetrics().density; }
    public static int dp(Context c, float v) { return Math.round(v * d(c)); }

    // palette mirrors res/values/tokens.xml — keep in sync
    public static final int BG        = 0xFF12141A;
    public static final int SURFACE   = 0xFF1C2029;
    public static final int SURFACE_HI= 0xFF242A36;
    public static final int STROKE    = 0xFF2E3644;
    public static final int TEXT_0    = 0xFFF2F5FA;
    public static final int TEXT_1    = 0xFFA9B3C2;
    public static final int TEXT_2    = 0xFF69748A;
    public static final int ACCENT    = 0xFF7EA6F5;
    public static final int OK        = 0xFF6BBF7B;
    public static final int VIOLET    = 0xFFA78BDB;
    public static final int TEAL      = 0xFF5FB3C9;

    public static GradientDrawable card(Context c, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{SURFACE_HI, SURFACE});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, 1), STROKE);
        return g;
    }

    public static View headerRow(Context c, String title, String tagline) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, dp(c, 18));
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(24f);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        TextView s = new TextView(c);
        s.setText(tagline);
        s.setTextSize(12.5f);
        s.setTextColor(TEXT_2);
        s.setPadding(dp(c, 1), dp(c, 2), 0, 0);
        col.addView(t);
        col.addView(s);
        return col;
    }

    /** Standard screen root: bg token + 20dp gutters + safe area. */
    public static LinearLayout screen(android.app.Activity act) {
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        root.setPadding(dp(act, 20), dp(act, 8), dp(act, 20), dp(act, 14));
        return root;
    }

    /** Icon inside a tinted soft square — the tile signature look. */
    public static View iconChip(Context c, int iconRes, int tint, int boxDp, int iconDp) {
        android.widget.ImageView iv = new android.widget.ImageView(c);
        Drawable d = c.getDrawable(iconRes);
        d.setTint(tint);
        iv.setImageDrawable(d);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor((tint & 0x00FFFFFF) | 0x1F000000);   // 12% tint wash
        bg.setCornerRadius(dp(c, boxDp / 3f));
        iv.setBackground(bg);
        int pad = dp(c, iconDp * 0.28f);
        iv.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp));
        iv.setLayoutParams(lp);
        return iv;
    }

    /** Entrance animation: staggered rise for lists/grids. */
    public static void animateIn(View v, long delayMs) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 14));
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs).setDuration(280)
                .setInterpolator(new OvershootInterpolator(0.7f)).start();
    }
}
