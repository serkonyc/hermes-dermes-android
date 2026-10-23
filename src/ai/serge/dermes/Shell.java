package ai.serge.dermes;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared Telegram-inspired app shell. */
public final class Shell {
    Shell() {}

    public static final int HOME = 0, CHAT = 1, MODELS = 2, DETAIL = -1;
    private static final int NAV_HEIGHT_DP = 76;

    public static void prepare(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            w.setNavigationBarDividerColor(Color.TRANSPARENT);
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    public static View wrap(Activity a, int selected, String title, String subtitle,
                            View content, boolean scrollable) {
        return wrap(a, selected, title, subtitle, content, scrollable, null, null);
    }

    public static View wrap(final Activity a, int selected, String title, String subtitle,
                            View content, boolean scrollable, Runnable back) {
        return wrap(a, selected, title, subtitle, content, scrollable, back, null);
    }

    public static View wrap(final Activity a, int selected, String title, String subtitle,
                            View content, boolean scrollable, Runnable back, View menu) {
        FrameLayout frame = new FrameLayout(a);
        frame.setBackgroundColor(UI.BACKGROUND);

        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(UI.space24(a), UI.space24(a), UI.space24(a), UI.dp(a, NAV_HEIGHT_DP + 24));
        column.addView(title(a, title, subtitle, back, menu));

        if (scrollable) {
            ScrollView scroll = new ScrollView(a);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
            column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            column.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        frame.addView(column, new FrameLayout.LayoutParams(-1, -1));
        frame.addView(nav(a, selected), new FrameLayout.LayoutParams(-1, UI.dp(a, NAV_HEIGHT_DP), Gravity.BOTTOM));
        applyBottomInset(frame, column);
        return frame;
    }

    private static View title(final Activity a, String title, String subtitle, final Runnable back, View menu) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, UI.space24(a));
        if (back != null) {
            ImageView arrow = new ImageView(a);
            arrow.setImageResource(R.drawable.ic_back);
            arrow.setImageTintList(android.content.res.ColorStateList.valueOf(UI.ACCENT));
            arrow.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            arrow.setPadding(0, 0, UI.space8(a), 0);
            row.addView(arrow, new LinearLayout.LayoutParams(UI.dp(a, 32), UI.dp(a, 40)));
            arrow.setOnClickListener(v -> { Shell.press(v); back.run(); });
        }
        LinearLayout copy = new LinearLayout(a);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(back == null ? 0 : UI.space12(a), 0, 0, 0);
        TextView t = new TextView(a); UI.headlineMedium(t); t.setText(title); t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = new TextView(a); UI.labelMedium(s); s.setTextColor(UI.TEXT_2); s.setText(subtitle);
            s.setPadding(0, UI.space4(a), 0, 0);
            copy.addView(s);
        }
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        if (menu != null) row.addView(menu);
        return row;
    }

    private static View nav(Activity a, int selected) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(UI.SURFACE_CONTAINER_LOW);
        bar.setElevation(UI.dp(a, 12));
        bar.addView(item(a, selected, HOME, "Home", R.drawable.ic_home, DermesActivity.class));
        bar.addView(item(a, selected, CHAT, "Chat", R.drawable.ic_chat, ChatActivity.class));
        bar.addView(item(a, selected, MODELS, "Models", R.drawable.ic_models, PocketActivity.class));
        return bar;
    }

    private static View item(Activity a, int selected, int id, String label, int icon, Class<?> target) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        boolean active = id == selected;

        ImageView image = new ImageView(a);
        Drawable d = a.getDrawable(icon); d.setTint(active ? UI.ACCENT : UI.TEXT_2); image.setImageDrawable(d);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (active) {
            GradientDrawable pill = new GradientDrawable(); pill.setColor(UI.ACCENT_SOFT); pill.setCornerRadius(UI.dp(a, 16));
            image.setBackground(pill);
        }
        image.setPadding(UI.dp(a, 18), UI.dp(a, 5), UI.dp(a, 18), UI.dp(a, 5));
        box.addView(image, new LinearLayout.LayoutParams(UI.dp(a, 62), UI.dp(a, 32)));

        TextView text = new TextView(a);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        text.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        text.setTextColor(active ? UI.ACCENT : UI.TEXT_2);
        text.setText(label); text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.topMargin = UI.space4(a);
        box.addView(text, tlp);

        box.setOnClickListener(v -> {
            if (active) return; press(v);
            android.content.Intent i = new android.content.Intent(a, target);
            i.setFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            a.startActivity(i);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        box.setLayoutParams(lp);
        return box;
    }

    public static void press(View v) {
        v.animate().scaleX(.96f).scaleY(.96f).setDuration(70).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(130).start()).start();
    }

    private static void applyBottomInset(final FrameLayout frame, final LinearLayout column) {
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) bottom = insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom;
            else bottom = insets.getSystemWindowInsetBottom();
            column.setPadding(column.getPaddingLeft(), column.getPaddingTop(), column.getPaddingRight(),
                    UI.dp(v.getContext(), NAV_HEIGHT_DP + 24) + bottom);
            View nav = ((FrameLayout) v).getChildAt(1);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) nav.getLayoutParams();
            lp.height = UI.dp(v.getContext(), NAV_HEIGHT_DP) + bottom;
            nav.setLayoutParams(lp);
            nav.setPadding(0, 0, 0, bottom);
            return insets;
        });
    }

}
