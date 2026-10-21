package ai.serge.dermes;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * One View painting the visual column (pulse / galaxy / load bar /
 * metric cards). Replaces the old four-View stack: fewer layers, one animation clock,
 * everything fits the screen height on purpose (layout_weight 1 in the activity).
 *
 * Performance: background starfield renders into a cached bitmap (regenerated on size
 * change); per-frame work is circles, paths and gradients only — no allocation-heavy
 * shaders except the small radial glows. Choreographer drives frames; pause() from
 * onPause stops the clock entirely.
 */
final class DashboardView extends View {

    private final DashRenderer r;
    private final List<Float> ripples = new ArrayList<>();
    private android.graphics.Bitmap starCache;

    private DashState state = new DashState();
    private double pulsePhase, rot;
    private double emitQueue, emitRate;   // request events/sec spread over refresh window
    private boolean ticking;
    private long lastNs;

    private final Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
        @Override public void doFrame(long t) {
            if (!ticking) return;
            long dt = lastNs == 0 ? 33_000_000L : t - lastNs;
            lastNs = t;
            float s = dt / 1e9f;
            int load = Math.max(0, state.running + state.waiting);
            pulsePhase += s * (0.22 + Math.min(0.7, load * 0.15));
            rot += s * 0.9;
            if (state.online && pulsePhase >= 1.0) { pulsePhase -= 1.0; ripples.add(0f); } // idle heartbeat
            emitQueue += s * emitRate;
            while (emitQueue >= 1.0) { emitQueue -= 1.0; ripples.add(0f); }
            for (int i = ripples.size() - 1; i >= 0; i--) {
                float a = ripples.get(i) + s;
                if (a > 1.7f) ripples.remove(i); else ripples.set(i, a);
            }
            invalidate();
            Choreographer.getInstance().postFrameCallbackDelayed(this, 25);
        }
    };

    DashboardView(Context c) {
        super(c);
        r = new DashRenderer(c.getResources().getDisplayMetrics().density);
    }

    void update(DashState s) {
        state = s;
        emitRate = s.requestDelta / 10.0;
    }

    void resume() {
        if (ticking) return;
        ticking = true; lastNs = 0;
        Choreographer.getInstance().postFrameCallback(frame);
    }

    void pause() {
        ticking = false;
        Choreographer.getInstance().removeFrameCallback(frame);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        r.size(w, h);
        starCache = null;
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        canvas.drawColor(DashRenderer.BG);

        // cached starfield behind everything
        if (starCache == null || starCache.getWidth() != (int) w || starCache.getHeight() != (int) h) {
            starCache = android.graphics.Bitmap.createBitmap((int) w, (int) h, android.graphics.Bitmap.Config.ARGB_8888);
            Canvas cc = new Canvas(starCache);
            java.util.Random rnd = new java.util.Random(7);
            android.graphics.Paint pp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            for (int i = 0; i < 140; i++) {
                float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h;
                float rad = 0.4f + rnd.nextFloat() * 1.1f;
                int col = new int[]{0xFFDCE7FF, 0xFFFFF0DC, 0xFFAECBFF}[rnd.nextInt(3)];
                pp.setColor(Colors.withAlpha(col, 20 + rnd.nextInt(100)));
                cc.drawCircle(x, y, rad * r.d, pp);
            }
        }
        canvas.drawBitmap(starCache, 0, 0, null);

        float yPulseEnd = h * DashRenderer.F_PULSE_END;
        float yGalaxyEnd = h * DashRenderer.F_GALAXY_END;
        float yMeterEnd = h * DashRenderer.F_METER_END;

        float[] ages = new float[ripples.size()];
        for (int i = 0; i < ages.length; i++) ages[i] = ripples.get(i);
        r.pulse(canvas, 0, yPulseEnd, pulsePhase, ages, 1f);
        r.galaxy(canvas, yPulseEnd, yGalaxyEnd, rot, state.models);
        r.meter(canvas, yGalaxyEnd, yMeterEnd, state.running, state.waiting);
        r.cards(canvas, yMeterEnd + r.d * 6, state.cards);
    }
}
