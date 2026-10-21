package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

/** Neon Runner 3D — Substance-style endless runner, pure Canvas software 3D.
 *  One plane on a receding grid, 3 lanes, camera behind the orb.
 *  Swipe left/right = lane, tap = jump, swipe up = roll (duck). No OpenGL, no assets. */
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
        // --- world: player fixed at z=0, everything else rushes toward camera ---
        static final int LANES = 3;
        static final float LANE_W = 0.9f;      // lane spacing, world units
        static final float Z_FAR = 17f;
        static final float HORIZON = 0.36f;    // fraction of height
        static final float FOCAL = 1.05f;      // focal * height

        float camH = 1.05f, camBack = 3.6f, pitch = 0.10f;
        int lane = 1;                          // 0..2, target lane
        float px;                              // actual x (lerps to lane center)
        float y, vy, roll;                     // jump height, vy, roll countdown
        boolean rolling;
        float speed = 8f, dist;
        boolean dead, running = true, started;
        int score, best;
        long lastNs;
        Choreographer cho;

        final java.util.ArrayList<float[]> obs = new java.util.ArrayList<>(); // z,type,lane,w,h
        final java.util.ArrayList<float[]> coins = new java.util.ArrayList<>(); // z,lane,phase
        final java.util.ArrayList<float[]> parts = new java.util.ArrayList<>();
        final java.util.ArrayList<float[]> stars = new java.util.ArrayList<>();
        float nextSpawnZ = Z_FAR;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();

        RunnerView(Context c) {
            super(c);
            best = prefs().getInt("runner_best", 0);
            cho = Choreographer.getInstance();
            Random r = new Random(11);
            for (int i = 0; i < 70; i++)
                stars.add(new float[]{r.nextFloat(), r.nextFloat() * 0.55f, 0.3f + r.nextFloat() * 0.7f});
        }
        private SharedPreferences prefs() {
            return getContext().getSharedPreferences("dermes", Context.MODE_PRIVATE);
        }
        private void reset() {
            obs.clear(); coins.clear(); parts.clear();
            lane = 1; px = 0; y = vy = roll = 0; speed = 8f;
            dist = 0; dead = false; rolling = false; started = true;
            score = 0; nextSpawnZ = Z_FAR;
        }
        void go(boolean on) {
            running = on && !dead;
            if (on) { lastNs = 0; if (!started) reset(); cho.postFrameCallback(this); }
            else cho.removeFrameCallback(this);
        }

        // --- projection: world (x,y,z) -> screen. camera at (0, camH, -camBack), pitch tilts down ---
        float sx(float x, float z, float w) { return w * 0.5f + FOCAL * x * hpx() / (z + camBack); }
        float sy(float y, float z, float h) {
            float dz = z + camBack;
            return h * HORIZON + FOCAL * (camH - y) * hpx() / dz + pitch * hpx() * 0.35f;
        }
        private float hpx() { return getHeight(); }
        private float s(float scale, float z) { return FOCAL * scale * hpx() / (z + camBack); }

        @Override public void doFrame(long ns) {
            if (!running) return;
            float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (ns - lastNs) / 1e9f);
            lastNs = ns;
            step(dt);
            invalidate();
            cho.postFrameCallback(this);
        }

        private void step(float dt) {
            if (dead) {
                for (float[] q : parts) { q[0] += q[3]*dt; q[1] += q[4]*dt; q[4] -= dt*3f; q[2] -= dt*1.5f; }
                parts.removeIf(q -> q[2] <= 0);
                return;
            }
            speed += dt * 0.32f;                       // ramp 8 -> ~14 by 20s
            dist += speed * dt;
            score = (int) dist;
            // lane lerp
            px += ((lane - 1) * LANE_W - px) * Math.min(1f, dt * 10f);
            // jump/roll physics
            if (y > 0 || vy > 0) {
                vy -= 16f * dt; y += vy * dt;
                if (y < 0) { y = 0; vy = 0; }
            }
            if (rolling) { roll -= dt; if (roll <= 0) rolling = false; }
            // move world
            for (float[] o : obs) o[0] -= speed * dt;
            obs.removeIf(o -> o[0] < -2f);
            for (float[] o : coins) o[0] -= speed * dt;
            coins.removeIf(o -> o[0] < -2f);
            // spawn ahead
            nextSpawnZ -= speed * dt;
            if (nextSpawnZ < Z_FAR) {
                float z = nextSpawnZ < 0 ? 2f : nextSpawnZ + 6f + (float)Math.random() * 4f;
                spawn(z);
                nextSpawnZ = z;
            }
            // coins pickup (player at z~0)
            for (java.util.Iterator<float[]> it = coins.iterator(); it.hasNext(); ) {
                float[] o = it.next();
                if (Math.abs(o[0]) < 0.55f && (int)o[1] == lane && y < 1.1f) {
                    it.remove();
                    for (int i = 0; i < 10; i++) parts.add(new float[]{
                            (o[1]-1)*LANE_W, 0.6f, 1f, (float)(Math.random()-0.5)*2f,
                            (float)Math.random()*2f, 1f});
                    score += 25; dist += 25;
                }
            }
            // collision at |z| < 0.65: low wall(jump/switch), tall bar(roll/switch), full wall(switch only)
            for (float[] o : obs) {
                if (Math.abs(o[0]) > 0.65f || (int)o[1] == 2 || (int)o[1] != lane) continue;
                float top = o[4];
                float head = rolling ? 0.85f : y;      // roll lowers head to .85
                if (head < top) { die(); return; }
            }
        }

        private void spawn(float z) {
            int r = (int)(Math.random() * 10);
            int ln = (int)(Math.random() * 3);
            if (r < 3) { obs.add(new float[]{z, 0, ln, 0.72f, 0.75f}); }               // low wall
            else if (r < 5) { obs.add(new float[]{z, 1, ln, 0.72f, 2.2f}); }           // tall bar
            else if (r < 7) {                                                          // full wall in 2 lanes
                int a = ln, b = (ln + 1 + (int)(Math.random()*2)) % 3;
                obs.add(new float[]{z, 2, a, 0.72f, 3f});
                obs.add(new float[]{z, 2, b, 0.72f, 3f});
            } else {                                                                   // coin line
                int n = 3 + (int)(Math.random() * 3);
                for (int i = 0; i < n; i++) coins.add(new float[]{z + i * 0.8f, ln, 0});
            }
        }

        private void die() {
            dead = true; running = false;
            if (score > best) { best = score; prefs().edit().putInt("runner_best", best).apply(); }
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2;
                parts.add(new float[]{px, 0.6f, 1f,
                        (float)Math.cos(a) * (0.5f + (float)Math.random()),
                        (float)Math.sin(a) + 1.5f, 1f});
            }
            invalidate();
        }

        // --- input: swipe = lane/roll, tap = jump ---
        float downX, downY; long downT;
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX(); downY = e.getY(); downT = e.getEventTime();
                    return true;
                case MotionEvent.ACTION_UP: {
                    if (dead) { reset(); go(true); return true; }
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    long dt = e.getEventTime() - downT;
                    if (Math.abs(dx) > dp(48) && Math.abs(dx) > Math.abs(dy)) {
                        lane = Math.max(0, Math.min(2, lane + (dx > 0 ? 1 : -1)));
                    } else if (dy < -dp(48) && dt < 350) {
                        rolling = true; roll = 0.45f;   // swipe up = roll
                        if (y > 0) vy = -6f;            // slam
                    } else if (dt < 260) {
                        if (y <= 0.01f && !rolling) { vy = 7.5f; y = 0.01f; }  // tap = jump
                    }
                    return true;
                }
            }
            return true;
        }

        // ---------- render ----------
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = hpx();
            // sky
            p.setShader(new LinearGradient(0, 0, 0, h * HORIZON + dp(10),
                    0xFF07090F, 0xFF101726, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
            p.setShader(null);
            for (float[] st : stars) {
                p.setColor(android.graphics.Color.argb((int)(95 * st[2]), 170, 195, 235));
                cv.drawCircle(st[0] * w, st[1] * h, 1.2f * st[2] + 0.3f, p);
            }
            // sun glow at horizon center
            p.setShader(new android.graphics.RadialGradient(w/2, h*HORIZON, w*0.45f,
                    0x2238E0D8, 0x00000000, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p); p.setShader(null);

            // ground
            p.setShader(new LinearGradient(0, h*HORIZON, 0, h,
                    0xFF0D1220, UI.BG, Shader.TileMode.CLAMP));
            cv.drawRect(0, h*HORIZON, w, h, p); p.setShader(null);

            // road edge strips (two long quads from near to far)
            drawEdge(cv, w, h, -1.62f);
            drawEdge(cv, w, h,  1.62f);
            // lane boundaries: solid glow line + moving dashes on top
            p.setStrokeWidth(dp(2.4f));
            p.setColor(android.graphics.Color.argb(170, 126, 166, 245));
            for (float x : new float[]{-LANE_W/2, LANE_W/2}) {
                cv.drawLine(sx(x, -camBack + 0.6f, w), sy(0, -camBack + 0.6f, h),
                            sx(x, Z_FAR, w), sy(0, Z_FAR, h), p);
            }
            for (float x : new float[]{-LANE_W/2, LANE_W/2})
                drawDashedLine(cv, w, h, x);
            // transverse grid lines streaming forward
            p.setStrokeWidth(dp(1.6f));
            float spacing = 1.6f, off = dist % spacing;
            for (float z = -off; z < Z_FAR; z += spacing) {
                if (z < -camBack + 0.5f) continue;
                p.setColor(android.graphics.Color.argb((int)(70 * clamp01(1 - z/Z_FAR) + 8), 95, 179, 201));
                float zl = sy(0, z, h);
                cv.drawLine(sx(-1.62f, z, w), zl, sx(1.62f, z, w), zl, p);
            }

            // obstacles + coins, far to near (painter order)
            java.util.ArrayList<float[]> ents = new java.util.ArrayList<>();
            for (float[] o : obs) ents.add(new float[]{o[0], o[1], o[2], o[3], o[4], 0});
            for (float[] o : coins) ents.add(new float[]{o[0], 3, o[1], 0.26f, 0, o[2]});
            java.util.Collections.sort(ents, new java.util.Comparator<float[]>() {
                public int compare(float[] a, float[] b) { return Float.compare(b[0], a[0]); } });
            for (float[] o : ents) {
                if (o[0] < -camBack + 0.6f || o[0] > Z_FAR) continue;
                if (o[1] == 3) drawCoin(cv, w, h, (o[2] - 1) * LANE_W, o[0]);
                else drawBox(cv, w, h, (o[2] - 1) * LANE_W, o[3], o[4], o[1], o[0]);
            }

            // player shadow + orb
            if (!dead) {
                float shs = s(0.55f - y * 0.08f, 0);
                float shx = sx(px, 0, w), shy = sy(0, 0, h);
                fill.setColor(android.graphics.Color.argb(110, 0, 0, 0));
                cv.drawOval(shx - shs, shy - shs*0.28f, shx + shs, shy + shs*0.28f, fill);
                drawOrb(cv, w, h);
            }
            // particles
            for (float[] q : parts) {
                p.setColor(android.graphics.Color.argb((int)(255 * Math.max(0, q[5])), 130, 240, 232));
                cv.drawCircle(sx(q[0], 0.2f, w), sy(q[1], 0.2f, h) - dp(28), dp(3.6f) * q[5], p);
            }

            // HUD
            p.setColor(UI.TEXT_1); p.setTextSize(sp(13));
            cv.drawText("best " + best, dp(20), dp(34), p);
            p.setColor(UI.TEXT_0); p.setTextSize(sp(24));
            p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            String sc = String.valueOf(score);
            cv.drawText(sc, w - dp(20) - p.measureText(sc), dp(40), p);
            p.setTypeface(android.graphics.Typeface.DEFAULT);
            if (dead) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(UI.TEXT_0); p.setTextSize(sp(34));
                cv.drawText("GAME OVER", w/2, h*0.34f, p);
                p.setColor(UI.TEXT_1); p.setTextSize(sp(15));
                cv.drawText(score >= best ? "new record · tap to run again" : "tap to run again",
                        w/2, h*0.34f + dp(32), p);
                p.setTextAlign(Paint.Align.LEFT);
            } else if (dist < 30) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(UI.TEXT_2); p.setTextSize(sp(14));
                cv.drawText("swipe: change lane · tap: jump · swipe up: roll",
                        w/2, h*0.66f, p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }

        private float clamp01(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

        private void drawEdge(Canvas cv, float w, float h, float x) {
            float z0 = -camBack + 0.6f, z1 = Z_FAR;
            path.reset();
            path.moveTo(sx(x - 0.05f, z0, w), sy(0, z0, h));
            path.lineTo(sx(x, z0, w), sy(0, z0, h));
            path.lineTo(sx(x, z1, w), sy(0, z1, h));
            path.lineTo(sx(x - 0.05f, z1, w), sy(0, z1, h));
            path.close();
            fill.setColor(android.graphics.Color.argb(150, 95, 179, 201));
            cv.drawPath(path, fill);
        }

        private void drawDashedLine(Canvas cv, float w, float h, float x) {
            p.setStrokeWidth(dp(1.2f));
            float spacing = 1.6f, off = dist % spacing;
            for (float z = -off; z < Z_FAR; z += spacing) {
                if (z < -camBack + 0.6f) continue;
                float z2 = z + spacing * 0.5f;
                p.setColor(android.graphics.Color.argb(60, 126, 166, 245));
                cv.drawLine(sx(x, z, w), sy(0, z, h), sx(x, z2, w), sy(0, z2, h), p);
            }
        }

        /** 3D box: lane x center wx, half-width hw, height ht, z front. type tints. */
        private void drawBox(Canvas cv, float w, float h, float wx, float hw, float ht, float type, float z) {
            int base, lite;
            if (type == 0)      { base = 0xFF3D5A80; lite = 0xFF9DB8D9; } // low wall: blue
            else if (type == 1) { base = 0xFF6B4E8F; lite = 0xFFC3A9E6; } // tall bar: violet
            else                { base = 0xFF2A3746; lite = 0xFF7A8CA0; } // full wall: graphite
            float x0 = sx(wx - hw, z, w), x1 = sx(wx + hw, z, w);
            float yb = sy(0, z, h), yt = sy(ht, z, h);
            float zb = Math.max(0.4f, 1.2f - z / Z_FAR);   // far ones fade a bit
            // front face vertical gradient = glossy glass
            p.setShader(new LinearGradient(x0, yt, x1, yb, lite, base, Shader.TileMode.CLAMP));
            p.setAlpha((int)(200 * zb));
            cv.drawRect(x0, yt, x1, yb, p);
            p.setShader(null); p.setAlpha(255);
            // top face (camera looks slightly down so it's visible): parallelogram receding
            path.reset();
            path.moveTo(x0, yt); path.lineTo(x1, yt);
            path.lineTo(sx(wx + hw, z + 0.35f, w), sy(ht, z + 0.35f, h));
            path.lineTo(sx(wx - hw, z + 0.35f, w), sy(ht, z + 0.35f, h));
            path.close();
            fill.setColor(android.graphics.Color.argb((int)(230 * zb), 20, 26, 38));
            cv.drawPath(path, fill);
            // neon top edge
            p.setColor(lite); p.setStrokeWidth(dp(2));
            cv.drawLine(sx(wx - hw, z + 0.35f, w), sy(ht, z + 0.35f, h),
                        sx(wx + hw, z + 0.35f, w), sy(ht, z + 0.35f, h), p);
        }

        private void drawCoin(Canvas cv, float w, float h, float wx, float z) {
            float cx = sx(wx, z, w), cy = sy(0.65f, z, h);
            float r = s(0.16f, z);
            p.setShader(new android.graphics.RadialGradient(cx - r*0.4f, cy - r*0.5f, r*0.2f,
                    new int[]{0xFFFFFFFF, 0xFFF0C34E, 0xFF9C6A12},
                    new float[]{0.1f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, p);
            p.setShader(null);
        }

        private void drawOrb(Canvas cv, float w, float h) {
            float cx = sx(px, 0, w), r = s(0.33f, 0) * (rolling ? 0.72f : 1f);
            float cy = sy(y + (rolling ? 0.12f : 0.33f), 0, h) - (rolling ? dp(6) : 0);
            // motion streaks
            for (int i = 4; i >= 1; i--) {
                p.setColor(android.graphics.Color.argb(20 - i * 4, 56, 224, 216));
                cv.drawCircle(cx, cy + i * r * 0.22f, r * (1f - i * 0.12f), p);
            }
            p.setShader(new android.graphics.RadialGradient(cx - r*0.35f, cy - r*0.4f, r*0.15f,
                    new int[]{0xFFFFFFFF, 0xFF7FF3EC, 0xFF14928C},
                    new float[]{0.08f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, p);
            p.setShader(null);
            // rim light
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.5f));
            p.setColor(0x66FFFFFF);
            cv.drawCircle(cx, cy, r - dp(0.8f), p);
            p.setStyle(Paint.Style.FILL);
        }

        private int dp(float v)   { return UI.dp(getContext(), v); }
        private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
    }
}
