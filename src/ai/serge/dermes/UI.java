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

    // ========== MD3 Color tokens (tonal dark surfaces) ==========
    // Background = the lowest layer; surface containers step up in lightness for elevation
    public static final int BACKGROUND       = 0xFF12141A;  // page background
    public static final int SURFACE          = 0xFF12141A;  // MD3 "surface" = same as bg
    public static final int SURFACE_CONTAINER_LOWEST = 0xFF12141A;
    public static final int SURFACE_CONTAINER_LOW    = 0xFF1C2029;  // cards, sheets
    public static final int SURFACE_CONTAINER        = 0xFF242A36;  // elevated cards
    public static final int SURFACE_CONTAINER_HIGH   = 0xFF2C323F;  // modals
    public static final int SURFACE_CONTAINER_HIGHEST= 0xFF353B49;  // tooltips
    
    // Legacy aliases for existing code
    public static final int BG       = BACKGROUND;
    public static final int SURFACE_HI = SURFACE_CONTAINER;
    
    // Text colors (high-emphasis for body, medium for secondary info)
    public static final int TEXT_0 = 0xFFF2F5FA;  // high-emphasis (body text)
    public static final int TEXT_1 = 0xFFA9B3C2;  // medium-emphasis (captions, labels)
    public static final int TEXT_2 = 0xFF69748A;  // disabled, hints
    public static final int STROKE = 0x00000000;  // deprecated, tonal surfaces only
    
    // Accent & status
    public static final int ACCENT = 0xFF7EA6F5;  // primary action, interactive
    public static final int OK     = 0xFF6BBF7B;
    public static final int VIOLET = 0xFFA78BDB;
    public static final int TEAL   = 0xFF5FB3C9;

    // ========== Spacing tokens (8dp grid) ==========
    // MD3 spacing: space4, space8, space12, space16, space20, space24, space32
    public static int space4(Context c)  { return dp(c, 4); }
    public static int space8(Context c)  { return dp(c, 8); }
    public static int space12(Context c) { return dp(c, 12); }
    public static int space16(Context c) { return dp(c, 16); }
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
    
    /** Elevated card (Surface Container for more prominence). */
    public static GradientDrawable cardElevated(Context c, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE_CONTAINER);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Filled button (primary action, filled tonal container style). */
    public static void buttonFilled(android.widget.Button b, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(ACCENT);
        g.setCornerRadius(dp(c, 20));  // pill
        b.setBackground(g);
        b.setTextColor(0xFF0B1220);
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
        root.setPadding(space20(act), dp(act, 16), space20(act), space24(act));
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
