package ai.serge.dermes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Tiny vector glyphs drawn with strokes — consistent weight, no icon deps. */
final class IconView extends View {
    static final int BRAIN = 0, PULSE = 1, FEED = 2, BARS = 3, ARROW = 4;
    private final int kind, color, size;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, int kind, int color, int sizePx) {
        super(c);
        this.kind = kind; this.color = color; this.size = sizePx;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, sizePx / 12f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = size / 2f;
        p.setColor(color);
        switch (kind) {
            case BRAIN: {   // neural nodes: one center, three linked
                float[][] n = {{cx, cy - r * .85f}, {cx + r * .8f, cy + r * .55f},
                               {cx - r * .8f, cy + r * .55f}};
                p.setColor((color & 0x00FFFFFF) | 0x55000000);   // links half-transparent
                for (float[] q : n) cv.drawLine(cx, cy, q[0], q[1], p);
                p.setColor(color);
                cv.drawCircle(cx, cy, r * .22f, p);
                for (float[] q : n) cv.drawCircle(q[0], q[1], r * .22f, p);
                break;
            }
            case PULSE: {   // ECG line
                float[] x = {cx - r, cx - r * .45f, cx - r * .15f, cx + r * .15f, cx + r * .45f, cx + r};
                float[] y = {cy, cy, cy - r * .7f, cy + r * .7f, cy, cy};
                for (int i = 0; i < x.length - 1; i++) cv.drawLine(x[i], y[i], x[i + 1], y[i + 1], p);
                break;
            }
            case FEED: {    // three text lines, first short = headline
                float w0 = r * 1.4f, w1 = r * 1.7f;
                for (int i = 0; i < 3; i++) {
                    float yy = cy + (i - 1) * r * .55f;
                    cv.drawLine(cx - (i == 0 ? w0 : w1) / 1.2f, yy, cx + w1 / 1.2f, yy, p);
                }
                break;
            }
            case BARS: {    // ascending bars
                for (int i = 0; i < 3; i++) {
                    float bx = cx + (i - 1) * r * .6f;
                    float bh = r * (.35f + i * .35f);
                    cv.drawLine(bx, cy + r * .8f, bx, cy + r * .8f - bh * 2, p);
                }
                break;
            }
            case ARROW: {   // download: shaft + head + tray
                cv.drawLine(cx, cy - r * .85f, cx, cy + r * .25f, p);
                cv.drawLine(cx - r * .5f, cy - r * .25f, cx, cy + r * .25f, p);
                cv.drawLine(cx + r * .5f, cy - r * .25f, cx, cy + r * .25f, p);
                cv.drawLine(cx - r * .8f, cy + r * .85f, cx + r * .8f, cy + r * .85f, p);
                break;
            }
        }
    }
}
