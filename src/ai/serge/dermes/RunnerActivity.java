package ai.serge.dermes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

/** Neon Runner v3 — Substance-like endless runner, software 3D on Canvas.
 *  Chase camera, 3 lanes, a real runner character (bob/lean/squash/stride),
 *  3 obstacle families, coins, pylons, speed lines, screen shake, +25 popups, big HUD.
 *  Swipe = lane, tap = jump, swipe up = roll. */
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
        // ---- camera / projection (pure software 3D) ----
        float camH = 2.3f, camBack = 6.5f, pitch = 0.10f, focal = 0.95f;
        float camShakeX, camShakeY;                 // decaying trauma

        // ---- world ----
        static final float LANE_W = 0.9f, Z_FAR = 22f, ROAD_HALF = 2.4f;
        int lane = 1; float px;                     // lane target + lerp'd x
        float y, vy; boolean airborne;
        float speed = 9f, dist, runT;               // runT = animation clock
        boolean dead, running, started;
        int score, best, coins;

        final java.util.ArrayList<float[]> obs = new java.util.ArrayList<>();   // z,type,lane,hw,h
        final java.util.ArrayList<float[]> coinL = new java.util.ArrayList<>(); // z,lane
        final java.util.ArrayList<float[]> pop = new java.util.ArrayList<>();   // +25 popups: x,ytop,z,life
        final java.util.ArrayList<float[]> parts = new java.util.ArrayList<>(); // death burst
        float nextSpawnZ = 10f;
        long lastNs; Choreographer cho;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();

        RunnerView(Context c) {
            super(c);
            best = prefs().getInt("runner_best", 0);
            cho = Choreographer.getInstance();
        }
        private SharedPreferences prefs() {
            return getContext().getSharedPreferences("dermes", Context.MODE_PRIVATE);
        }
        private void reset() {
            obs.clear(); coinL.clear(); pop.clear(); parts.clear();
            lane = 1; px = 0; y = vy = 0; speed = 9f;
            dist = 0; runT = 0; score = 0; coins = 0;
            dead = false; airborne = false; started = true;
            nextSpawnZ = 10f; camShakeX = camShakeY = 0;
        }
        void go(boolean on) {
            running = on && !dead;
            if (on) { lastNs = 0; if (!started) reset(); cho.postFrameCallback(this); }
            else cho.removeFrameCallback(this);
        }

        // world -> screen
        float sx(float x, float z)  { return getWidth() * 0.5f + focal * x * H() / (z + camBack); }
        float sy(float yv, float z) {
            float dz = z + camBack;
            return H() * (camH - yv) * focal / dz + H() * pitch * 0.35f + H() * 0.5f;
        }
        float sscale(float z)       { return focal * H() / (z + camBack); }
        private float H() { return getHeight(); }

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
                for (float[] q : parts) { q[1] += q[3]*dt; q[2] += q[4]*dt; q[4] -= dt*4f; q[5] -= dt*1.6f; }
                parts.removeIf(q -> q[5] <= 0);
                return;
            }
            runT += dt * (6f + speed * 0.35f);
            speed += dt * 0.28f;
            dist += speed * dt;
            score = (int) dist + coins * 25;
            camShakeX *= (float)Math.pow(0.001f, dt); camShakeY *= (float)Math.pow(0.001f, dt);
            px += ((lane - 1) * LANE_W - px) * Math.min(1f, dt * 9f);   // lane lerp
            if (y > 0 || vy > 0) {
                airborne = true; vy -= 17f * dt; y += vy * dt;
                if (y < 0) { y = 0; vy = 0; airborne = false; camShakeY = Math.min(9f, speed*0.5f); }
            }
            for (float[] o : obs) o[0] -= speed * dt;
            obs.removeIf(o -> o[0] < -camBack + 0.3f);
            for (float[] o : coinL) o[0] -= speed * dt;
            coinL.removeIf(o -> o[0] < -camBack + 0.3f);
            for (float[] o : pop) o[3] -= dt;
            pop.removeIf(o -> o[3] <= 0);
            nextSpawnZ -= speed * dt;
            if (nextSpawnZ < Z_FAR * 0.6f) {
                float z = Math.max(6f, nextSpawnZ) + 5.5f + (float)Math.random() * 4f;
                spawn(z); nextSpawnZ = z;
            }
            for (java.util.Iterator<float[]> it = coinL.iterator(); it.hasNext(); ) {
                float[] o = it.next();
                if (Math.abs(o[0]) < 0.7f && (int)o[1] == lane && y < 1.3f) {
                    it.remove(); coins++;
                    pop.add(new float[]{(o[1]-1)*LANE_W, 1.1f, 0.4f, 0.7f});
                }
            }
            for (float[] o : obs) {
                if (o[0] > 0.9f || o[0] < -0.7f || (int)o[2] != lane) continue;
                float top = o[4], head = y;
                if (head < top) { die(); return; }
            }
        }

        private void spawn(float z) {
            int r = (int)(Math.random() * 10);
            int ln = (int)(Math.random() * 3);
            if (r < 4) obs.add(new float[]{z, 0, ln, 0.42f, 0.8f});          // low: jump
            else if (r < 6) {                                                // full: lane change
                int a = ln, b = (ln + 1 + (int)(Math.random()*2)) % 3;
                obs.add(new float[]{z, 2, a, 0.42f, 3f});
                obs.add(new float[]{z, 2, b, 0.42f, 3f});
            } else {
                int n = 3 + (int)(Math.random() * 4);
                for (int i = 0; i < n; i++) coinL.add(new float[]{z + i * 0.9f, ln});
            }
        }

        private void die() {
            dead = true; running = false;
            camShakeX = camShakeY = 16f;
            if (score > best) { best = score; prefs().edit().putInt("runner_best", best).apply(); }
            for (int i = 0; i < 42; i++) {
                double a = Math.random() * Math.PI * 2;
                parts.add(new float[]{px, 0.8f, 0, (float)Math.cos(a)*(1f+(float)Math.random()),
                        (float)Math.sin(a) + 2f, 1f});
            }
            invalidate();
        }

        // ---- input ----
        float downX, downY; long downT;
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX(); downY = e.getY(); downT = e.getEventTime(); return true;
                case MotionEvent.ACTION_UP: {
                    if (dead) { reset(); go(true); return true; }
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    long dt = e.getEventTime() - downT;
                    if (Math.abs(dx) > dp(40) && Math.abs(dx) > Math.abs(dy))
                        lane = Math.max(0, Math.min(2, lane + (dx > 0 ? 1 : -1)));
                    else if (dy < -dp(40)) { if (y <= 0.01f) { vy = 8f; y = 0.01f; } }  // swipe up = jump
                    return true;
                }
            }
            return true;
        }

        // ================= RENDER =================
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = H();
            float shx = camShakeX * (float)(Math.random()*2-1), shy = camShakeY * (float)(Math.random()*2-1);
            cv.save(); cv.translate(shx, shy);
            // sky + ground gradients
            fillP.setShader(new android.graphics.LinearGradient(0, 0, 0, h*0.52f,
                    0xFF06080E, 0xFF131B2C, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h*0.52f, fillP);
            fillP.setShader(new android.graphics.LinearGradient(0, h*0.52f, 0, h,
                    0xFF0E1422, 0xFF07090E, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRect(0, h*0.52f, w, h, fillP); fillP.setShader(null);
            fillP.setShader(new android.graphics.RadialGradient(w/2, h*0.5f, w*0.55f,
                    0x3326D8E0, 0x00000000, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, fillP); fillP.setShader(null);

            // road slab + neon edges + lane lines
            quad(cv, -ROAD_HALF, -camBack+0.5f, ROAD_HALF, -camBack+0.5f, ROAD_HALF, Z_FAR, -ROAD_HALF, Z_FAR, 0xFF1A2233);
            p.setStrokeWidth(dp(3)); p.setColor(0xFF5FB3C9);
            for (float x : new float[]{-ROAD_HALF, ROAD_HALF})
                cv.drawLine(sx(x, -camBack+0.5f), sy(0, -camBack+0.5f), sx(x, Z_FAR), sy(0, Z_FAR), p);
            p.setStrokeWidth(dp(2)); p.setColor(0x997EA6F5);
            for (float x : new float[]{-LANE_W/2, LANE_W/2})
                cv.drawLine(sx(x, -camBack+0.5f), sy(0, -camBack+0.5f), sx(x, Z_FAR), sy(0, Z_FAR), p);
            // transverse stream lines
            p.setStrokeWidth(dp(1.5f));
            float sp0 = 1.6f, off = dist % sp0;
            for (float z = -off; z < Z_FAR; z += sp0) {
                if (z < -camBack + 0.4f) continue;
                int a = (int)(95 * (1 - z/Z_FAR)) + 10;
                p.setColor((a << 24) | 0x3A5570);
                cv.drawLine(sx(-ROAD_HALF, z), sy(0, z), sx(ROAD_HALF, z), sy(0, z), p);
            }
            // side pylons (speed rails)
            for (float z = -(dist % 4f); z < Z_FAR; z += 4f) {
                if (z < -camBack + 0.5f) continue;
                box3d(cv, -ROAD_HALF - 0.35f, 0, 0.9f, z, 0.12f, 0xFF233247, 0xFF3D5A80);
                box3d(cv,  ROAD_HALF + 0.35f, 0, 0.9f, z, 0.12f, 0xFF233247, 0xFF3D5A80);
            }
            // radial speed lines when fast
            if (speed > 10.5f) {
                int n = (int)((speed - 10.5f) * 1.6f);
                p.setStrokeWidth(dp(2)); p.setColor(0x1EFFFFFF);
                for (int i = 0; i < n; i++) {
                    float a = (runT * 3f + i * 1.7f) % 6.283f;
                    float r0 = w * 0.36f + ((i * 91f) % (w * 0.28f));
                    cv.drawLine(w/2 + (float)Math.cos(a) * r0, h*0.48f + (float)Math.sin(a) * r0 * 0.7f,
                                w/2 + (float)Math.cos(a) * (r0 + w*0.11f), h*0.48f + (float)Math.sin(a) * (r0 + w*0.11f) * 0.7f, p);
                }
            }
            // entities far->near (painter order)
            java.util.ArrayList<float[]> ents = new java.util.ArrayList<>();
            for (float[] o : obs) ents.add(new float[]{o[0], o[1], o[2], o[3], o[4], 0});
            for (float[] o : coinL) ents.add(new float[]{o[0], 3, o[1], 0.18f, 0, 0});
            java.util.Collections.sort(ents, new java.util.Comparator<float[]>() {
                public int compare(float[] a, float[] b) { return Float.compare(b[0], a[0]); } });
            for (float[] o : ents) {
                if (o[0] < -camBack + 0.4f || o[0] > Z_FAR) continue;
                float wx = (o[2] - 1) * LANE_W;
                if (o[1] == 3) coin(cv, wx, o[0]);
                else box3d(cv, wx, 0, o[4], o[0], o[3],
                        o[1]==0 ? 0xFF3D5A80 : 0xFF323F52,
                        o[1]==0 ? 0xFF9DB8D9 : 0xFF8FA2B8);
            }
            if (!dead) { shadow(cv); runner(cv); }
            for (float[] q : parts) {
                p.setColor(android.graphics.Color.argb((int)(255*q[5]), 130, 240, 232));
                cv.drawCircle(sx(q[0], 0.3f), sy(q[1], 0.3f) - dp(20), dp(4f)*q[5], p);
            }
            for (float[] o : pop) {   // +25 popups float up
                p.setAlpha((int)(255 * Math.min(1, o[3] * 2f)));
                p.setColor(0xFFF0C34E); p.setTextSize(sp(17)); p.setTextAlign(Paint.Align.CENTER);
                cv.drawText("+25", sx(o[0], o[2]), sy(o[1] + (0.7f - o[3]) * 1.4f, o[2]), p);
                p.setTextAlign(Paint.Align.LEFT); p.setAlpha(255);
            }
            cv.restore();
            hud(cv, w, h);
        }

        private void hud(Canvas cv, float w, float h) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(sp(44));
            p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            String sc = String.valueOf(score);
            p.setColor(0x40000000); cv.drawText(sc, w/2 + dp(2), dp(58) + dp(2), p);  // drop shadow
            p.setColor(UI.TEXT_0);   cv.drawText(sc, w/2, dp(58), p);
            p.setTextSize(sp(13)); p.setColor(UI.TEXT_2);
            cv.drawText("BEST " + best, w/2, dp(82), p);
            // coins top-right with gem
            p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(sp(19)); p.setColor(0xFFF0C34E);
            String cs = "× " + coins;
            cv.drawText(cs, w - dp(20), dp(46), p);
            float gx = w - dp(20) - p.measureText(cs) - dp(14);
            p.setColor(0xFFFFE9A8); cv.drawCircle(gx, dp(40), dp(7), p);
            p.setColor(0xFF9C6A12); cv.drawCircle(gx, dp(40), dp(4.5f), p);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(android.graphics.Typeface.DEFAULT);
            if (dead) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(sp(40)); p.setColor(0xFFFFFFFF);
                p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                cv.drawText("GAME OVER", w/2, h*0.36f, p);
                p.setTypeface(android.graphics.Typeface.DEFAULT);
                p.setTextSize(sp(20)); p.setColor(0xFFF0C34E);
                cv.drawText("score " + score, w/2, h*0.36f + dp(40), p);
                p.setTextSize(sp(15)); p.setColor(UI.TEXT_1);
                cv.drawText(score >= best && score > 0 ? "new record · tap to run again" : "tap to run again",
                        w/2, h*0.36f + dp(70), p);
                p.setTextAlign(Paint.Align.LEFT);
            } else if (dist < 40) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(UI.TEXT_2); p.setTextSize(sp(14));
                cv.drawText("swipe left/right: lane · swipe up: jump", w/2, h*0.62f, p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }

        // ---- primitives ----
        private void quad(Canvas cv, float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4, int col) {
            path.reset();
            path.moveTo(sx(x1, z1), sy(0, z1)); path.lineTo(sx(x2, z2), sy(0, z2));
            path.lineTo(sx(x3, z3), sy(0, z3)); path.lineTo(sx(x4, z4), sy(0, z4));
            path.close(); fillP.setColor(col); cv.drawPath(path, fillP);
        }

        /** axis box: lane x center, y 0..ht, depth 0.4 from z, half-width hw. front+top+side faces. */
        private void box3d(Canvas cv, float wx, float y0, float ht, float z, float hw, int base, int lite) {
            float z2 = z + 0.4f;
            float fx0 = sx(wx-hw, z), fx1 = sx(wx+hw, z), fyb = sy(y0, z), fyt = sy(ht, z);
            fillP.setColor(base); cv.drawRect(fx0, fyt, fx1, fyb, fillP);
            path.reset();
            path.moveTo(fx0, fyt); path.lineTo(fx1, fyt);
            path.lineTo(sx(wx+hw, z2), sy(ht, z2)); path.lineTo(sx(wx-hw, z2), sy(ht, z2));
            path.close(); fillP.setColor(lite); cv.drawPath(path, fillP);
            path.reset();
            path.moveTo(fx1, fyb); path.lineTo(fx1, fyt);
            path.lineTo(sx(wx+hw, z2), sy(ht, z2)); path.lineTo(sx(wx+hw, z2), sy(y0, z2));
            path.close(); fillP.setColor(mix(base, lite)); cv.drawPath(path, fillP);
            p.setStrokeWidth(dp(2)); p.setColor(lite);
            cv.drawLine(sx(wx-hw, z2), sy(ht, z2), sx(wx+hw, z2), sy(ht, z2), p);
        }
        private int mix(int a, int b) {
            return 0xFF000000 | (((a>>16&255)+(b>>16&255))/2<<16) | (((a>>8&255)+(b>>8&255))/2<<8) | ((a&255)+(b&255))/2;
        }

        private void coin(Canvas cv, float wx, float z) {
            float cx = sx(wx, z), cy = sy(0.7f, z), r = 0.18f * sscale(z);
            fillP.setShader(new android.graphics.RadialGradient(cx - r*0.35f, cy - r*0.4f, r*0.15f,
                    new int[]{0xFFFFFFFF, 0xFFF0C34E, 0xFF9C6A12},
                    new float[]{0.1f, 0.6f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, fillP); fillP.setShader(null);
        }

        private void shadow(Canvas cv) {
            float cx = sx(px, 0), gy = sy(0, 0);
            float r = sscale(0) * Math.max(0.15f, 0.42f - y * 0.05f);
            fillP.setColor(android.graphics.Color.argb((int)Math.max(0, 130 - y*40), 0, 0, 0));
            cv.drawOval(cx - r, gy - r*0.26f, cx + r, gy + r*0.26f, fillP);
        }

        /** The runner: a real little person seen from behind — head, glossy jacket
         *  torso, swinging arms, two-segment legs with a running cycle, knee shins,
         *  bob + jump tuck + lean into lane change. */
        private void runner(Canvas cv) {
            float s = sscale(0);
            float cx = sx(px, 0);
            float lean = ((lane - 1) * LANE_W - px) * 24f;                 // deg into the turn
            float bob = airborne ? 0 : (float)Math.abs(Math.sin(runT)) * 0.05f;
            float Hh = 1.5f * s;                                           // full body height
            float baseY = sy(y, 0) - bob * s;
            cv.save(); cv.translate(cx, baseY); cv.rotate(lean);
            // rim glow under the figure
            fillP.setShader(new android.graphics.RadialGradient(0, -Hh*0.45f, Hh*0.7f,
                    0x3326D8E0, 0x00000000, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRoundRect(-Hh*0.5f, -Hh, Hh*0.5f, dp(4), Hh*0.3f, Hh*0.3f, fillP);
            fillP.setShader(null);
            float stride = airborne ? 0.9f : (float)Math.sin(runT * 2f);   // leg phase
            float hipY = -Hh*0.42f, shoY = -Hh*0.78f, headY = -Hh*0.9f;
            // --- legs (behind body first, then front) ---
            drawLeg(cv,  s, hipY, -stride, airborne);
            drawLeg(cv, -s, hipY,  stride, airborne);
            // --- torso: glossy jacket, wider at shoulders ---
            path.reset();
            path.moveTo(-Hh*0.16f, hipY); path.lineTo(-Hh*0.24f, shoY);
            path.quadTo(0, shoY - Hh*0.06f, Hh*0.24f, shoY);
            path.lineTo(Hh*0.16f, hipY); path.close();
            fillP.setShader(new android.graphics.LinearGradient(0, shoY, 0, hipY,
                    0xFF7FF3EC, 0xFF0E6E68, android.graphics.Shader.TileMode.CLAMP));
            cv.drawPath(path, fillP);
            fillP.setShader(null);
            fillP.setColor(0x4DFFFFFF);   // gloss on left shoulder blade
            cv.drawRoundRect(-Hh*0.2f, shoY + Hh*0.02f, -Hh*0.06f, shoY + Hh*0.3f, Hh*0.05f, Hh*0.05f, fillP);
            // --- arms anti-phase to legs, elbow bend ---
            drawArm(cv,  s, shoY, stride, airborne);
            drawArm(cv, -s, shoY, -stride, airborne);
            // --- head + hood ---
            fillP.setColor(0xFF0B5750);
            cv.drawCircle(0, headY, Hh*0.135f, fillP);
            fillP.setColor(0xFFE8F4F1);
            cv.drawCircle(0, headY - Hh*0.015f, Hh*0.105f, fillP);
            fillP.setColor(0x66FFFFFF);
            cv.drawCircle(-Hh*0.04f, headY - Hh*0.05f, Hh*0.035f, fillP);   // head gloss
            cv.restore();
            p.setStrokeCap(Paint.Cap.BUTT);
            // dust puffs while grounded
            if (!airborne) {
                int a = (int)(60 * Math.abs(Math.cos(runT)));
                fillP.setColor(android.graphics.Color.argb(a, 160, 180, 200));
                float gy = sy(0, 0);
                cv.drawCircle(cx - Hh*0.4f, gy - dp(3), dp(4.5f), fillP);
                cv.drawCircle(cx + Hh*0.42f, gy - dp(2), dp(3.5f), fillP);
            }
        }
        /** one leg: thigh from hip swinging with phase, shin folded when back. */
        private void drawLeg(Canvas cv, float sgn, float hipY, float ph, boolean air) {
            float L = 0.42f * sscale(0);
            float kneeX = sgn * L * 0.25f + ph * L * 0.5f, kneeY = hipY + L * 0.55f;
            float footX = kneeX + (air ? L*0.15f : ph * L * 0.35f - (ph < 0 ? L*0.25f : 0));
            float footY = air ? kneeY + L*0.3f : (ph > 0 ? 0f : -L*0.25f*(float)(-ph));
            p.setStrokeWidth(dp(6.5f)); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(0xFF173B52);
            cv.drawLine(sgn * dp(4), hipY, kneeX, kneeY, p);
            cv.drawLine(kneeX, kneeY, footX, footY, p);
            p.setColor(0xFF0E2A3C);   // shoe
            p.setStrokeWidth(dp(5));
            cv.drawLine(footX - dp(1), footY, footX + dp(4), footY, p);
        }
        /** one arm: upper + fore, swing opposite the same-side leg. */
        private void drawArm(Canvas cv, float sgn, float shoY, float ph, boolean air) {
            float L = 0.34f * sscale(0);
            float ex = sgn * L * 0.5f + ph * L * 0.4f, ey = shoY + L * 0.6f;
            float hx = ex + ph * L * 0.3f + (air ? sgn*L*0.3f : 0), hy = ey + L * (air ? -0.2f : 0.45f);
            p.setStrokeWidth(dp(5f)); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(0xFF12726B);
            cv.drawLine(sgn * dp(9), shoY + dp(2), ex, ey, p);
            cv.drawLine(ex, ey, hx, hy, p);
        }

        private int dp(float v)   { return UI.dp(getContext(), v); }
        private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
    }
}
