package ai.serge.dermes;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * One visual column of the dashboard, drawn in a single View (one surface, no LinearLayout
 * stacking). Coordinates are dp-scaled by the host view; everything is procedural Canvas —
 * no bitmaps, no Kotlin. Geometry: nebula-pulse -> starfield/galaxy -> sparkline -> load
 * bar -> metric cards. Height is whatever the host gives; sections are proportional.
 */
final class DashRenderer {

    // Palette
    static final int BG = 0xFF0D1117;
    private static final int ACCENT = 0xFF58A6FF;
    private static final int ACCENT_SOFT = 0xFF89C1FF;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8B949E;
    private static final int CARD = 0xFF161B22;
    private static final int CARD_EDGE = 0xFF30363D;
    private static final int GREEN = 0xFF3FB950;
    private static final int AMBER = 0xFFD29922;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private final java.util.Random starRnd = new java.util.Random(7); // used by view star cache seed

    float w, h, d; // view size px, density (1 dp = d px)

    // Section boundaries (fractions of height)
    static final float F_PULSE_END = 0.22f, F_GALAXY_END = 0.62f, F_METER_END = 0.72f;

    DashRenderer(float density) { this.d = density; }

    void size(float w, float h) { this.w = w; this.h = h; }

    private float sp(float dp) { return dp * d; }

    // Text helpers -------------------------------------------------------------
    private void txt(Canvas c, String s, float x, float y, float sp, int color) {
        text.setTextSize(sp * d); text.setColor(color);
        c.drawText(s, x, y, text);
    }

    // Stars (static field) are drawn by DashboardView into a cached bitmap.

    // ---- SECTIONS -------------------------------------------------------------

    /** Nebula pulse: layered haze, dark core, expanding ripple rings. */
    void pulse(Canvas c, float y0, float y1, double phase, float[] rippleAges, float breathe) {
        float hh = y1 - y0;
        float cx = w / 2f, cy = y0 + hh / 2f;
        float base = hh * 0.22f;
        float br = 1f + 0.05f * (float) Math.sin(phase * Math.PI * 2);
        // haze layers (soft, radial gradient for smooth falloff)
        for (float[] layer : new float[][]{{4.4f, 0x0F}, {3.1f, 0x1C}, {2.1f, 0x3D}, {1.5f, 0x6E}}) {
            float rr = base * layer[0] * br;
            glow.setShader(new RadialGradientHolder(cx, cy, rr, (int) layer[1]).shader);
            c.drawCircle(cx, cy, rr, glow);
        }
        glow.setShader(null);
        // dark core disc, then bright inner rim
        c.drawCircle(cx, cy, base * 1.12f * br, darkCore());
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sp(1.6f)); p.setColor(0x4D89C1FF);
        c.drawCircle(cx, cy, base * 1.12f * br, p);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(cx - base, cy - base, cx + base, cy + base,
                new int[]{0x3358A6FF, 0x9958A6FF}, null, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, base * 0.92f * br, p);
        p.setShader(null);
        // ripples: thick near, thin far, additive
        float maxR = Math.min(w * 0.48f, hh * 0.96f);
        p.setStyle(Paint.Style.STROKE);
        for (float age : rippleAges) {
            if (age <= 0) continue;
            float t = Math.min(1f, age / 1.7f);
            float rr = base * 1.12f + (maxR - base * 1.12f) * t;
            int a = (int) (210 * (1 - t) * (1 - t));
            if (a <= 4) continue;
            p.setStrokeWidth(sp(3.2f) * (1 - 0.75f * t) + sp(0.6f));
            p.setColor(Colors.withAlpha(ACCENT, a));
            c.drawCircle(cx, cy, rr, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private Paint darkCore() { p.setColor(BG); return p; }

    /** Starfield + orbit + model stars. */
    void galaxy(Canvas c, float y0, float y1, double rot, java.util.List<DashState.Model> models) {
        // starfield lives in the view's cached bitmap; here only orbits and stars
        float hh = y1 - y0;
        float cx = w / 2f, cy = y0 + hh / 2f;
        // central glow
        for (float[] l : new float[][]{{90, 0x14}, {55, 0x33}, {30, 0x66}}) {
            glow.setShader(new RadialGradientHolder(cx, cy, sp(l[0]), (int) l[1]).shader);
            c.drawCircle(cx, cy, sp(l[0]), glow);
        }
        glow.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sp(1.1f)); p.setColor(0x3D808080);
        float orbit0 = sp(42), orbit1 = sp(96);
        c.drawOval(cx - orbit0 * 1.55f, cy - orbit0 * 0.6f, cx + orbit0 * 1.55f, cy + orbit0 * 0.6f, p);
        c.drawOval(cx - orbit1 * 1.55f, cy - orbit1 * 0.6f, cx + orbit1 * 1.55f, cy + orbit1 * 0.6f, p);
        p.setStyle(Paint.Style.FILL);
        int i = 0;
        for (DashState.Model m : models) {
            float orbit = i == 0 ? orbit0 : orbit1 + sp(i * 22);
            double a = rot / (1.0 + orbit / sp(150)) + i * 2.4;
            float sx = cx + (float) Math.cos(a) * orbit * 1.55f;
            float sy = cy + (float) Math.sin(a) * orbit * 0.6f;
            float rad = sp(7f + 16f * Math.min(1f, (float) (m.gb / 80.0)));
            // glow halo, tinted by state
            int col = "ready".equals(m.state) ? ACCENT : AMBER;
            for (float[] l : new float[][]{{2.6f, 0x26}, {1.7f, 0x4D}, {1.25f, 0x80}}) {
                glow.setShader(new RadialGradientHolder(sx, sy, rad * l[0], (int) l[1], col).shader);
                c.drawCircle(sx, sy, rad * l[0], glow);
            }
            glow.setShader(null);
            p.setColor(Colors.mix(col, 0xFFFFFFFF, 0.35f));
            c.drawCircle(sx, sy, rad, p);
            // specular
            p.setColor(Colors.withAlpha(0xFFFFFFFF, 190));
            c.drawCircle(sx - rad * 0.3f, sy - rad * 0.38f, rad * 0.32f, p);
            txt(c, m.id + "  " + String.format(java.util.Locale.US, "%.0f GB", m.gb),
                    Math.min(sx + rad + sp(6), w - sp(140)), sy + sp(4), 11, 0xFFC9D4E4);
            i++;
        }
    }

    /** Load bar: slim rounded track with glow cap + label. */
    void meter(Canvas c, float y0, float y1, int running, int waiting) {
        float padL = sp(20), padR = sp(20);
        float pw = w - padL - padR;
        float ph = sp(14);
        float py = (y0 + y1) / 2 - ph / 2 + sp(6);
        txt(c, "LOAD", padL, y0 + sp(14), 10, TEXT_DIM);
        String lab = "running " + running + " · waiting " + waiting;
        txt(c, lab, w - padR, y0 + sp(14), 10, TEXT_DIM);
        r.set(padL, py, padL + pw, py + ph);
        p.setColor(0xFF21262D); c.drawRoundRect(r, ph, ph, p);
        float[] seg = {running, waiting};
        int[] col = {GREEN, AMBER};
        float total = Math.max(1f, running + waiting);
        float x = padL;
        for (int i = 0; i < 2; i++) {
            if (seg[i] <= 0) continue;
            float sw = pw * seg[i] / total;
            r.set(x, py, x + sw, py + ph);
            p.setShader(new LinearGradient(x, py, x + sw, py + ph,
                    new int[]{Colors.lighten(col[i], 0.25f), col[i]}, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, ph, ph, p);
            p.setShader(null);
            x += sw;
        }
        // divider tick between segments
        if (running > 0 && waiting > 0) {
            float dx = padL + pw * running / total;
            p.setColor(BG); p.setStrokeWidth(sp(1.6f));
            c.drawLine(dx, py + sp(2), dx, py + ph - sp(2), p);
        }
    }

    /** Metric cards: 2-column glass grid, accent ticks, big numbers. */
    void cards(Canvas c, float y0, java.util.List<DashState.Card> items) {
        float pad = sp(20), gap = sp(10);
        float cw = (w - pad * 2 - gap) / 2;
        int rows = (items.size() + 1) / 2;
        // fit all rows into remaining height (cover screen is short; bottom row was clipping)
        float avail = h - y0 - pad;
        float ch = Math.min(sp(62), (avail - gap * (rows - 1)) / rows);
        if (ch < sp(34)) return;
        for (int i = 0; i < items.size(); i++) {
            DashState.Card cd = items.get(i);
            float x = pad + (i % 2) * (cw + gap);
            float y = y0 + (i / 2) * (ch + gap);
            r.set(x, y, x + cw, y + ch);
            // card body with top-edge accent
            p.setColor(CARD); c.drawRoundRect(r, sp(12), sp(12), p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sp(1)); p.setColor(CARD_EDGE);
            c.drawRoundRect(r, sp(12), sp(12), p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(cd.accent);
            r.set(x + sp(12), y + sp(13), x + sp(15), y + sp(21));
            c.drawRoundRect(r, sp(1.5f), sp(1.5f), p);
            txt(c, cd.label.toUpperCase(java.util.Locale.US), x + sp(21), y + sp(21), 9.5f, TEXT_DIM);
            // shrink-to-fit: "75 644 MB" must never clip on narrow panels (review v3.1.1)
            float vs = 17f;
            text.setTextSize(sp(vs));
            while (text.measureText(cd.value) > cw - sp(24) && vs > 11f) {
                vs -= 1f; text.setTextSize(sp(vs));
            }
            text.setColor(TEXT_MAIN);
            c.drawText(cd.value, x + sp(12), y + ch - sp(13), text);
        }
    }

    // utils --------------------------------------------------------------------
    private Paint withA(int color, int a) { p.setColor(Colors.withAlpha(color, a)); return p; }

    private static final class RadialGradientHolder {
        final Shader shader;
        RadialGradientHolder(float cx, float cy, float r, int alpha) { this(cx, cy, r, alpha, 0xFF58A6FF); }
        RadialGradientHolder(float cx, float cy, float r, int alpha, int rgb) {
            int core = Colors.withAlpha(rgb, alpha & 0xFF);
            this.shader = new RadialGradient(cx, cy, Math.max(1f, r),
                    new int[]{core, Colors.withAlpha(rgb, 0)}, null, Shader.TileMode.CLAMP);
        }
    }
}
