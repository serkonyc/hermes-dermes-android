package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

/** Neon Runner — endless arcade. Tap to jump, double jump available.
 *  Pure Canvas + Choreographer clock; pauses with the activity. */
public class RunnerActivity extends Activity {

    private RunnerView view;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        view = new RunnerView(this);
        setContentView(view);
    }
    @Override protected void onResume() { super.onResume(); view.go(true); }
    @Override protected void onPause()  { super.onPause();  view.go(false); }

    private static class RunnerView extends View implements Choreographer.FrameCallback {
        // world
        float orbY, orbVy, speed = 0.42f, t;   // normalized units of width/height
        boolean running = true, dead, canDouble;
        final java.util.ArrayList<float[]> obs = new java.util.ArrayList<>(); // x,w,h
        final java.util.ArrayList<float[]> stars = new java.util.ArrayList<>();
        final java.util.ArrayList<float[]> parts = new java.util.ArrayList<>();
        int score; int best;
        long lastNanos;
        Choreographer cho;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);

        RunnerView(Context c) {
            super(c);
            best = prefs().getInt("runner_best", 0);
            cho = Choreographer.getInstance();
            java.util.Random r = new java.util.Random(7);
            for (int i = 0; i < 60; i++)
                stars.add(new float[]{r.nextFloat(), r.nextFloat() * 0.75f, 0.3f + r.nextFloat() * 0.7f});
        }
        private SharedPreferences prefs() {
            return getContext().getSharedPreferences("dermes", Context.MODE_PRIVATE);
        }

        void reset() {
            obs.clear(); parts.clear(); orbY = 0; orbVy = 0; speed = 0.42f;
            score = 0; t = 0; dead = false; canDouble = true; running = true;
        }

        void go(boolean on) {
            running = on && !dead;
            if (on) { lastNanos = 0; cho.postFrameCallback(this); }
            else cho.removeFrameCallback(this);
        }

        @Override public void doFrame(long ns) {
            if (!running) return;
            float dt = lastNanos == 0 ? 0.016f : Math.min(0.05f, (ns - lastNanos) / 1e9f);
            lastNanos = ns;
            step(dt);
            invalidate();
            cho.postFrameCallback(this);
        }

        private void step(float dt) {
            t += dt;
            if (dead) { // particles keep flying
                for (float[] q : parts) { q[0] += q[2]*dt; q[1] += q[3]*dt; q[3] += 2.2f*dt; q[4] -= dt*1.4f; }
                parts.removeIf(q -> q[4] <= 0);
                return;
            }
            speed += dt * 0.0045f;                       // ramp
            score += (int)(speed * dt * 60);
            // physics (normalized height units, gravity down-negative)
            orbVy -= 3.6f * dt;
            orbY += orbVy * dt;
            if (orbY < 0) { orbY = 0; orbVy = 0; canDouble = true; }
            // spawn obstacles: gap scales with speed so it stays fair
            float last = obs.isEmpty() ? -9 : obs.get(obs.size()-1)[0];
            if (obs.isEmpty() || last < 1f - (0.55f + 0.35f*speed)) {
                float h = 0.08f + (float)Math.random() * (0.07f + speed*0.08f);
                obs.add(new float[]{1.05f, 0.035f, h});
            }
            for (float[] o : obs) o[0] -= speed * dt;
            obs.removeIf(o -> o[0] < -0.1f);
            // collision: orb fixed at x=0.22, radius .030 (width units), height above ground
            float ox = 0.22f, orad = 0.030f;
            float groundH = 0.16f;                        // ground line from bottom (height units)
            for (float[] o : obs) {
                float oy = o[2];                          // obstacle height
                if (ox + orad > o[0] && ox - orad < o[0] + o[1] && orbY < oy) die();
            }
        }

        private void die() {
            dead = true; running = false;
            if (score > best) { best = score; prefs().edit().putInt("runner_best", best).apply(); }
            // burst
            for (int i = 0; i < 26; i++) {
                double a = Math.random() * Math.PI * 2; float v = (float)(0.3 + Math.random()*0.8);
                parts.add(new float[]{0.22f, orbY, (float)Math.cos(a)*v, (float)Math.sin(a)*v, 1f});
            }
            invalidate();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_DOWN) return true;
            if (dead) { reset(); go(true); return true; }
            if (orbY <= 0.001f) { orbVy = 1.15f; canDouble = false; }
            else if (canDouble) { orbVy = 0.95f; canDouble = false; }
            return true;
        }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            // sky
            p.setShader(new LinearGradient(0, 0, 0, h,
                    0xFF0B0E14, UI.BG, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
            p.setShader(null);
            // parallax stars
            for (float[] s : stars) {
                float x = ((s[0] - t * 0.008f * s[2]) % 1f + 1f) % 1f;
                p.setColor(Color.argb((int)(90 * s[2]), 160, 190, 230));
                cv.drawCircle(x * w, s[1] * h, 1.1f * s[2] + 0.4f, p);
            }
            float groundY = h * 0.84f;
            // neon ground with moving ticks
            p.setStrokeWidth(dp(2));
            p.setColor(UI.TEAL); glow.setMaskFilter(null);
            cv.drawLine(0, groundY, w, groundY, p);
            p.setColor(Color.argb(40, 56, 224, 216));
            p.setStrokeWidth(dp(7));
            cv.drawLine(0, groundY, w, groundY, p);
            p.setColor(Color.argb(120, 56, 224, 216));
            float tick = 0.06f, off = (t * speed * 2f) % tick;
            for (float x = -off; x < 1f; x += tick) {
                float px = x * w;
                cv.drawLine(px, groundY + dp(4), px - dp(10), groundY + dp(14), p);
            }
            // obstacles: glass bars
            for (float[] o : obs) {
                float x = o[0] * w, bw = o[1] * w, bh = o[2] * h * 1.6f;
                p.setShader(new LinearGradient(x, groundY - bh, x, groundY,
                        0xFF3B82F6, 0xFF1D3A75, Shader.TileMode.CLAMP));
                cv.drawRoundRect(x, groundY - bh, x + bw, groundY, dp(5), dp(5), p);
                p.setShader(null);
                p.setColor(Color.argb(140, 150, 200, 255));
                cv.drawLine(x + dp(2), groundY - bh + dp(2), x + bw - dp(2), groundY - bh + dp(2), p);
            }
            // orb trail
            if (!dead) {
                float ox = 0.22f * w, oy = groundY - dp(10) - orbY * h * 1.6f - dp(14);
                for (int i = 5; i >= 1; i--) {
                    p.setColor(Color.argb(26 - i*4, 56, 224, 216));
                    cv.drawCircle(ox - i * dp(9), oy + dp(2), dp(14) - i*dp(1.6f), p);
                }
                // orb: glossy sphere
                glow.setColor(0xFF38E0D8);
                p.setShader(new android.graphics.RadialGradient(
                        ox - dp(4), oy - dp(5), dp(2),
                        new int[]{0xFFFFFFFF, 0xFF7FF3EC, 0xFF14928C},
                        new float[]{0.1f, 0.55f, 1f}, Shader.TileMode.CLAMP));
                cv.drawCircle(ox, oy, dp(14), p);
                p.setShader(null);
            }
            // particles
            for (float[] q : parts) {
                p.setColor(Color.argb((int)(255 * Math.max(0, q[4])), 120, 240, 235));
                cv.drawCircle(q[0] * w, groundY - dp(10) - q[1] * h * 1.6f, dp(3.4f) * q[4], p);
            }
            // HUD
            p.setColor(UI.TEXT_1); p.setTextSize(sp(13));
            cv.drawText("best " + best, dp(20), dp(34), p);
            p.setColor(UI.TEXT_0); p.setTextSize(sp(22));
            p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            cv.drawText(String.valueOf(score), w - dp(20) - p.measureText(String.valueOf(score)), dp(38), p);
            if (dead) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(UI.TEXT_0); p.setTextSize(sp(30));
                cv.drawText("GAME OVER", w/2, h*0.40f, p);
                p.setColor(UI.TEXT_1); p.setTextSize(sp(15));
                p.setTypeface(android.graphics.Typeface.DEFAULT);
                cv.drawText(score >= best ? "new record · tap to run again" : "tap to run again",
                        w/2, h*0.40f + dp(30), p);
                p.setTextAlign(Paint.Align.LEFT);
            } else if (score < 40) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(UI.TEXT_2); p.setTextSize(sp(14));
                cv.drawText("tap to jump · tap again mid-air", w/2, h*0.30f, p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }
        private int dp(float v)  { return UI.dp(getContext(), v); }
        private float sp(float v){ return v * getResources().getDisplayMetrics().scaledDensity; }
    }
}
