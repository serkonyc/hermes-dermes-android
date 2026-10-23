package ai.serge.dermes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Design system (v4.13.2): Material Design 3 tokens + Telegram-style dark aesthetic.
 *
 * Core principles applied:
 * - 8dp spacing scale (4dp for icons/fine details)
 * - Tonal surfaces (lighter surface = higher elevation, no strokes)
 * - Material type scale: display/headline/title/body/label
 * - Telegram clean dark: flat surfaces, generous padding, no visual noise
 * - 20dp screen margins, 16dp card padding, 8/12dp gaps between elements
 */
public final class UI {
    UI() {}

    // ========== Density ==========
    public static float d(Context c) { return c.getResources().getDisplayMetrics().density; }
    public static int dp(Context c, float v) { return Math.round(v * d(c)); }

    // ========== "Quiet Neural Flagship" tokens (docs/DESIGN_TOKENS.md) ==========
    // Obsidian canvas; surfaces step up one warm tonal level each.
    public static final int BACKGROUND       = 0xFF05070C;  // page background, never pure black
    public static final int SURFACE          = 0xFF05070C;
    public static final int SURFACE_CONTAINER_LOWEST = 0xFF05070C;
    public static final int SURFACE_CONTAINER_LOW    = 0xFF0B0F17;  // cards, sheets, nav
    public static final int SURFACE_CONTAINER        = 0xFF121826;  // elevated cards, composer
    public static final int SURFACE_CONTAINER_HIGH   = 0xFF1A2234;  // modals
    public static final int SURFACE_CONTAINER_HIGHEST= 0xFF232C40;  // tooltips

    // Legacy aliases for existing code
    public static final int BG       = BACKGROUND;
    public static final int SURFACE_HI = SURFACE_CONTAINER;

    // Text colors (warm white, never pure white)
    public static final int TEXT_0 = 0xFFEAE7F2;
    public static final int TEXT_1 = 0xFF9AA0B4;
    public static final int TEXT_2 = 0xFF5A6072;
    public static final int STROKE = 0x00000000;  // deprecated, tonal surfaces only

    // Accent & status: cyan = intelligence, violet = assistant identity,
    // gradient accent->assistant only on: active nav pill, CTA, send button.
    public static final int ACCENT = 0xFF2DE2E6;
    public static final int ACCENT_DEEP = 0xFF12A9C4;
    public static final int ON_ACCENT = 0xFF04121A;   // text/icon on bright fills
    public static final int ACCENT_SOFT = 0xFF123043;  // user-message surface
    public static final int OK     = 0xFF3ED89A;
    public static final int VIOLET = 0xFF7A5CFF;
    public static final int VIOLET_DEEP = 0xFF4A2FD6;
    public static final int WARN   = 0xFFF5B94D;
    public static final int TEAL   = 0xFF12A9C4;

    // ========== Spacing tokens (8dp grid) ==========
    // MD3 spacing: space4, space8, space12, space16, space20, space24, space32
    public static int space4(Context c)  { return dp(c, 4); }
    public static int space6(Context c)  { return dp(c, 6); }
    public static int space8(Context c)  { return dp(c, 8); }
    public static int space10(Context c) { return dp(c, 10); }
    public static int space12(Context c) { return dp(c, 12); }
    public static int space14(Context c) { return dp(c, 14); }
    public static int space16(Context c) { return dp(c, 16); }
    public static int space18(Context c) { return dp(c, 18); }
    public static int space20(Context c) { return dp(c, 20); }
    public static int space24(Context c) { return dp(c, 24); }
    public static int space32(Context c) { return dp(c, 32); }

    // ========== Type scale (MD3 display/headline/title/body/label) ==========
    // Size and weight for each role; line-height auto-calc by TextView

    // Display - hero text, large numerals
    public static void displayLarge(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }
    public static void displayMedium(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }

    // Headline - section headers, prominent titles
    public static void headlineMedium(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }
    public static void headlineSmall(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }

    // Title - medium-emphasis, shorter text
    public static void titleMedium(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }
    public static void titleSmall(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }

    // Body - paragraphs, longer text
    public static void bodyLarge(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }
    /** Chat body: slightly larger than bodyLarge for long-form reading. */
    public static void bodyChat(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }
    public static void bodyMedium(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(TEXT_0);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }
    public static void bodySmall(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTextColor(TEXT_1);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }

    // Label - buttons, chips, captions
    public static void labelLarge(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }
    public static void labelMedium(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }
    public static void labelSmall(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL);
    }

    // ========== Components ==========

    /**
     * Tonal surface card (MD3 elevation through color, not shadows).
     * Surface Container Low = default elevated card (chat bubbles, tiles).
     */
    public static GradientDrawable card(Context c, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE_CONTAINER_LOW);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Card with explicit fill (user bubbles etc). */
    public static GradientDrawable card(Context c, int radiusDp, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Elevated card (Surface Container for more prominence). */
    public static GradientDrawable cardElevated(Context c, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE_CONTAINER);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Signature 135deg cyan->violet gradient pill (CTA, send, active nav icon). */
    public static GradientDrawable grad(Context c, int radiusDp) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{ACCENT, VIOLET});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Filled button (primary action, flagship gradient pill). */
    public static void buttonFilled(android.widget.Button b, Context c) {
        GradientDrawable g = grad(c, 20);
        b.setBackground(g);
        b.setTextColor(ON_ACCENT);
        b.setAllCaps(false);
        labelLarge(b);
        b.setPadding(space16(c), space12(c), space16(c), space12(c));
        b.setElevation(dp(c, 2));
    }

    /** Tonal button (secondary action, Surface Container style). */
    public static void buttonTonal(android.widget.Button b, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE_CONTAINER);
        g.setCornerRadius(dp(c, 20));
        b.setBackground(g);
        b.setTextColor(ACCENT);
        b.setAllCaps(false);
        labelLarge(b);
        b.setPadding(space16(c), space12(c), space16(c), space12(c));
    }

    /** Outlined button (minimal emphasis, Surface with tinted outline). */
    public static void buttonOutlined(android.widget.Button b, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setCornerRadius(dp(c, 20));
        g.setStroke(dp(c, 1), (ACCENT & 0x00FFFFFF) | 0x4D000000);
        b.setBackground(g);
        b.setTextColor(ACCENT);
        b.setAllCaps(false);
        labelLarge(b);
        b.setPadding(space16(c), space12(c), space16(c), space12(c));
    }

    /**
     * Screen header: display/headline title + caption tagline.
     * Standard top padding: 20dp sides, 16dp top, 24dp bottom.
     */
    public static View headerRow(Context c, String title, String tagline) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, space24(c));

        TextView t = new TextView(c);
        t.setText(title);
        headlineMedium(t);

        if (tagline != null && !tagline.isEmpty()) {
            TextView s = new TextView(c);
            s.setText(tagline);
            bodySmall(s);
            s.setTextColor(TEXT_2);
            s.setPadding(0, space4(c), 0, 0);
            col.addView(t);
            col.addView(s);
        } else {
            col.addView(t);
        }
        return col;
    }

    /**
     * Standard screen root: page bg, safe area insets, 20dp margins, 16dp top gap below status bar.
     * All activities wrap content inside this.
     */
    public static LinearLayout screen(android.app.Activity act) {
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setFitsSystemWindows(true);
        // 20dp side margins, 16dp top, 24dp bottom
        root.setPadding(space24(act), space24(act), space24(act), space24(act));
        return root;
    }

    /** Centered footer: small label, bottom padding for nav bar clearance. */
    public static View footer(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        labelSmall(t);
        t.setTextColor(TEXT_2);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, space16(c), 0, dp(c, 8));
        return t;
    }

    /** Full-width block with top margin (standard vertical gap between sections). */
    public static LinearLayout.LayoutParams block(Context c, float topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    /**
     * Icon inside tinted surface chip (tile/grid signature look).
     * Surface Container + 15% tint wash, rounded 12dp, icon centered with padding.
     */
    public static View iconChip(Context c, int iconRes, int tint, int boxDp, int iconDp) {
        android.widget.ImageView iv = new android.widget.ImageView(c);
        Drawable d = c.getDrawable(iconRes);
        d.setTint(tint);
        iv.setImageDrawable(d);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor((tint & 0x00FFFFFF) | 0x26000000);  // 15% tint
        bg.setCornerRadius(dp(c, 12));
        iv.setBackground(bg);

        int pad = (int) (dp(c, iconDp) * 0.28f);
        iv.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp));
        iv.setLayoutParams(lp);
        return iv;
    }

    /** Standard list row: icon chip + title/sub + chevron; used by Tools and Settings. */
    public static View row(Context c, int iconRes, int tint, String title, String sub, Runnable onClick) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        r.setPadding(0, space12(c), 0, space12(c));
        r.addView(iconChip(c, iconRes, tint, 44, 24));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(space16(c), 0, space16(c), 0);
        TextView h = new TextView(c); titleMedium(h); h.setText(title);
        col.addView(h);
        if (sub != null && !sub.isEmpty()) {
            TextView s = new TextView(c); bodySmall(s); s.setText(sub); s.setTextColor(TEXT_1);
            s.setSingleLine(true); s.setEllipsize(android.text.TextUtils.TruncateAt.END);
            s.setPadding(0, space4(c), 0, 0);
            col.addView(s);
        }
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        android.widget.ImageView chevron = new android.widget.ImageView(c);
        chevron.setImageResource(R.drawable.ic_chevron);
        chevron.setImageTintList(android.content.res.ColorStateList.valueOf(TEXT_2));
        r.addView(chevron, new LinearLayout.LayoutParams(dp(c, 20), dp(c, 20)));
        if (onClick != null) {
            r.setClickable(true);
            r.setOnClickListener(v -> { Shell.press(v); onClick.run(); });
        }
        return r;
    }

    /** Staggered entrance (rise + fade, ease-out curve). */
    public static void animateIn(View v, long delayMs) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 16));
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();
    }

    /** Get app version (for footer display). */
    public static String version(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }
}
